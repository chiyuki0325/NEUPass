@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package ink.chyk.pass.repositories

import com.ctrip.flight.mmkv.defaultMMKV
import ink.chyk.pass.*
import ink.chyk.pass.api.portal.*
import ink.chyk.pass.api.portal.login.*
import ink.chyk.pass.api.portal.login.LoginApi.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.*
import kotlin.uuid.Uuid

object LoginRepository: IRepository {
  private val mmkv by lazy { defaultMMKV() }
  private val loginApi by lazy { LoginApi(deviceId) }

  // 设备标识是 UUID v4，首次生成后不再变化（协议的 imei 字段），不随账户切换清除。
  private val deviceId: String
    get() {
      if (!mmkv.containsKey("device_uuid")) mmkv["device_uuid"] = Uuid.random().toString()
      return mmkv.getString("device_uuid")
    }

  sealed interface AuthStatus {
    data object Healthy : AuthStatus
    data class LoginRequired(val message: String) : AuthStatus
    data object Unknown : AuthStatus
  }

  // 给 UI 层观察当前登录状态，用于跳登录
  private val _authStatus = MutableStateFlow<AuthStatus>(AuthStatus.Unknown)
  val authStatus: StateFlow<AuthStatus> = _authStatus.asStateFlow()

  private val loginMutex = Mutex()
  private var cached: String? = null
  private var pendingCredentials: Pair<String, String>? = null


  // ===== app 启动时判断本地是否有账号 =====
  // 只读本地凭据，不发网络请求；凭据是否过期由业务侧 withLoginTicket 静默重登处理，
  // 重登失败会把 authStatus 置为 LoginRequired，由 UI 跳登录。
  fun hasAccount(): Boolean = loadCredentials() != null


  // ===== 给业务侧的 api =====

  // 业务侧可能有需求
  suspend fun getLoginTicket(): String = ensureLoginTicket()

  // 业务高级函数：自动注入 CASTGC；凭证失效时静默重登并原样重放一次。
  suspend fun <T> withLoginTicket(block: suspend (loginTicket: String) -> T): T {
    val before = ensureLoginTicket()
    try {
      return block(before)
    } catch (_: SessionExpiredException) {
      // 信封 10013，重新登录
    } catch (_: TicketFailedException) {
      // CASTGC->ST 报错，重新登录
    }

    // 静默重登
    val fresh = refreshLoginTicket(stale = before)
    return try {
      block(fresh)
    } catch (_: SessionExpiredException) {
      requireLogin()
    } catch (_: TicketFailedException) {
      requireLogin()
    }
  }

  fun logout() {
    loginApi.reset()
    repositories.forEach { it.clearAllData() }
    clearAllCache()
    // ui 会自己跳登录
  }

  internal suspend fun requireLogin(
    message: String = "登录信息已失效，请重新登录。",
  ): Nothing =
    loginMutex.withLock {
      requireLoginInLock(message)
    }

  //===== 业务侧 api end =====

  // ===== 给登录页调用的 api =====

  // 登录页可能有需求
  val loginState: StateFlow<LoginState> get() = loginApi.state

  // 丢弃上次未完成的登录流程（如停在验证码步骤）
  fun cancelLogin() {
    pendingCredentials = null
    loginApi.reset()
  }

  // 返回 Authenticated 或 SmsCodeSent；需要短信二次验证时直接发送验证码
  suspend fun login(username: String, password: String): LoginState {
    // 学号密码暂存到内存，登录成功后写到磁盘
    return loginMutex.withLock {
      pendingCredentials = username to password
      loginApi.reset()
      val state = finish(loginApi.requestLogin(username, password))
      if (state is LoginState.SmsRequired) loginApi.sendSmsCode() else state
    }
  }

  suspend fun submitSmsCode(smsCode: String, trustDevice: Boolean = true): LoginState =
    loginMutex.withLock { finish(loginApi.submitSmsCode(smsCode, trustDevice)) }

  // 承接登录页结果
  private fun finish(state: LoginState): LoginState {
    if (state is LoginState.Authenticated) {
      // 登录成功，保存学号密码到磁盘
      // 暂存凭据为空说明登录过程中账户已被清空，本次登录作废，从头开始
      val (username, password) = pendingCredentials ?: run {
        loginApi.reset()
        throw LoginFailedException("登录已失效，请重新登录")
      }
      mmkv["student_id"] = username
      mmkv["password"] = password
      pendingCredentials = null
      // 保存ticket
      saveLoginTicketInLock(state.ticket)
    }
    return state
  }

  // ===== 登录页 api end =====


  // ===== 内部方法，带 InLock 表示脏读写，必须在 withLock 内调用 =====

  // 内存->MMKV->null
  private fun currentLoginTicket(): String? =
    cached ?: if (mmkv.containsKey("login_ticket")) mmkv.getString("login_ticket") else null
  
  // 内存->MMKV->重新登录
  private suspend fun ensureLoginTicket(): String {
    currentLoginTicket()?.let { return it }
    return refreshLoginTicket(stale = null)
  }

  // 最终落到重新登录，需要传入 stale loginTicket 防止重复刷新。（二次检查）
  private suspend fun refreshLoginTicket(stale: String?): String =
    loginMutex.withLock {
      val current = currentLoginTicket()
      if (current != null && current != stale) current
      // 已经掉登录，等用户重新登录，不再重复静默登录
      else if (authStatus.value is AuthStatus.LoginRequired) throw LoginRequiredException("请先登录校园账号。")
      else silentLoginInLock()
    }

  // 锁内执行静默登录，保证不会有并发刷新。
  // 不处理短信，有短信直接抛异常，跳登录页
  private suspend fun silentLoginInLock(): String {
    val (username, password) = loadCredentials() ?: requireLoginInLock("请先登录校园账号。")
    // 单独创建一个 loginApi 实例 防止状态污染
    val silentApi = LoginApi(deviceId)
    val state = try {
      silentApi.requestLogin(username, password)
    } catch (e: NetworkException) {
      // 网络失败原样上抛，凭据保留。
      throw e
    } catch (_: LoginFailedException) {
      // 密码更改过，跳登录页
      requireLoginInLock("登录信息已过期，请重新登录。")
    } finally {
      silentApi.close()
    }
    return when (state) {
      is LoginState.Authenticated -> state.ticket.also { saveLoginTicketInLock(it) }
      else -> {
        // 需要短信二次验证，跳登录页
        requireLoginInLock("登录信息已过期，请重新登录。")
      }
    }
  }

  // 读取磁盘中之前存过的学号密码
  private fun loadCredentials(): Pair<String, String>? =
    if (mmkv.containsKey("student_id") && mmkv.containsKey("password")) {
      mmkv.getString("student_id") to mmkv.getString("password")
    } else null

  private fun saveLoginTicketInLock(loginTicket: String) {
    mmkv["login_ticket"] = loginTicket
    cached = loginTicket
    _authStatus.value = AuthStatus.Healthy
  }

  // 清除登录态并中断业务调用链（LoginRequiredException 是 CancellationException，业务协程安静结束），
  // UI 观察 authStatus 跳登录页
  private fun requireLoginInLock(message: String): Nothing {
    clearAuthInLock(message)
    throw LoginRequiredException(message)
  }

  private fun clearAuthInLock(message: String) {
    mmkv.removeValueForKey("login_ticket")
    PortalCookiesStorage.clear()
    cached = null
    _authStatus.value = AuthStatus.LoginRequired(message)
  }

  override fun clearAllData() {
    pendingCredentials = null
    mmkv.removeValueForKey("student_id")
    mmkv.removeValueForKey("password")
    clearAuthInLock("请先登录校园账号。")
  }
}
