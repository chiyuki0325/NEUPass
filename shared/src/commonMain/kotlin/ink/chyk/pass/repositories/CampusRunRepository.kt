package ink.chyk.pass.repositories

import com.ctrip.flight.mmkv.defaultMMKV
import ink.chyk.pass.*
import ink.chyk.pass.api.CampusApp
import ink.chyk.pass.api.campus_run.*
import ink.chyk.pass.utils.AppLog
import kotlinx.coroutines.sync.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

object CampusRunRepository : IRepository {
  // 校园乐跑

  private val mmkv by lazy { defaultMMKV() }
  private val api by lazy { CampusRunApi() }
  private val json = Json { encodeDefaults = true }

  private var session: CampusRunSession? = null
  private val mutex = Mutex()

  override fun clearAllData() {
    session = null
    mmkv.removeValueForKey("campus_run_session")
  }

  private suspend fun getSession(): CampusRunSession = mutex.withLock {
    // 内存 -> MMKV -> 登录 API
    session?.let { return@withLock it }
    if (mmkv.containsKey("campus_run_session")) {
      val storedSession = mmkv.getString("campus_run_session")
      try {
        return@withLock json.decodeFromString<CampusRunSession>(storedSession).also { session = it }
      } catch (_: Exception) {
        mmkv.removeValueForKey("campus_run_session")
      }
    }
    performLogin()
    session!!
  }

  private suspend fun performLogin() {
    // 登录步道乐跑
    AppLog.d("CampusRunRepository", "正在登录步道乐跑...")

    val newSession = try {
      api.login(AppAuthRepository.newAppTicket(CampusApp.CAMPUS_RUN))
    } catch (_: TicketExpiredException) {
      api.login(AppAuthRepository.newAppTicket(CampusApp.CAMPUS_RUN))
    }

    session = newSession
    mmkv["campus_run_session"] = json.encodeToString(newSession)
  }

  suspend fun <T> campusRunAuthenticated(
    action: suspend (CampusRunSession) -> T
  ): T {
    var s = getSession()

    try {
      return action(s)
    } catch (e: SessionExpiredException) {
      // 校园跑掉登录时返回的不是正常加密响应，认证边界会转换成 SessionExpiredException。
      // 重新登录一次
      // 还不行的话就是校园跑重保，手机不在内网导致的
      AppLog.d("CampusRunRepository", "CampusRun Session 过期或异常: ${e.message}")
      mutex.withLock {
        // 双重检查，防止多协程同时触发重新登录，和其它 repo 一样
        if (session === s || session == null) {
          performLogin()
        }
        s = session!!
      }
      return action(s)
    }
  }

  suspend fun getIndex(): WpIndexResponse? {
    return campusRunAuthenticated { api.getIndex(it) }
  }

  suspend fun getBeforeRun(): BeforeRunResponse? {
    return campusRunAuthenticated { api.getBeforeRun(it) }
  }

  suspend fun getTermRunRecord(): GetTermRunRecordResponse? {
    return campusRunAuthenticated { api.getTermRunRecord(it) }
  }

  suspend fun getStartRunUrl(): String {
    return api.getStartRunUrl(getSession())
  }

}
