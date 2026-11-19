package ink.chyk.pass.api.portal.login

import ink.chyk.pass.api.*
import ink.chyk.pass.api.portal.*
import io.ktor.client.call.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.*

// 智慧东大 3.1.5+ 统一身份认证（app_login）客户端
// @“docs/api/1. 登录接口（CASTGC）.md"

class LoginApi (
  private val deviceId: String,
) {
  companion object {
    private const val BASE_URL = "https://personal.neu.edu.cn/portal/manage/common/app_login"
  }

  // app_login 协议由 LoginPlugin 处理，门户会话 Cookie 由 HttpCookies 维护；
  // 接口方法只描述请求与响应模型。
  private val http = createApiClient {
    install(LoginPlugin)
    install(HttpCookies) { storage = PortalCookiesStorage }
  }

  // 登录状态机
  sealed interface LoginState {
    data object Idle : LoginState

    // commit 后需要短信二次验证，tempToken 即临时凭证。
    data class SmsRequired(val tempToken: String) : LoginState

    // 验证码已发送，可重发或提交。
    data class SmsCodeSent(val tempToken: String) : LoginState

    // 登录成功：下发 CASTGC 与账号资料；门户会话 Cookie 随 Set-Cookie 写入 Cookie 存储。
    data class Authenticated(
      val ticket: String,
      val profile: LoginProfile,
    ) : LoginState
  }
  private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
  val state: StateFlow<LoginState> = _state.asStateFlow()

  // 请求登录，进入 SmsRequired 或 Authenticated。
  suspend fun requestLogin(username: String, password: String): LoginState {
    val current = _state.value
    if (current != LoginState.Idle) throw IllegalStateException("当前状态不可请求登录: $current")
    val data = http.post("$BASE_URL/commit") {
      setBody(LoginRequest(username, password, deviceId, "android"))
    }.body<LoginResponse>()
    _state.value = transitionState(data)
    return _state.value
  }

  // 发送短信验证码，SmsRequired → SmsCodeSent；已发送状态允许重发。
  suspend fun sendSmsCode(): LoginState {
    val tempToken = when (val current = _state.value) {
      is LoginState.SmsRequired -> current.tempToken
      is LoginState.SmsCodeSent -> current.tempToken
      else -> throw IllegalStateException("当前状态不可发送短信验证码: $current")
    }
    http.post("$BASE_URL/smsSend") { setBody(SmsSendRequest(tempToken, deviceId)) }
    _state.value = LoginState.SmsCodeSent(tempToken)
    return _state.value
  }

  // 提交短信验证码完成登录；输错验证码停留在 SmsCodeSent，可重试或重发。
  suspend fun submitSmsCode(smsCode: String, trustDevice: Boolean = true): LoginState {
    val current = _state.value
    if (current !is LoginState.SmsCodeSent) throw IllegalStateException("当前状态不可提交短信验证码: $current")
    val data = http.post("$BASE_URL/verifyCommit") {
      setBody(
        VerifyCommitRequest(
          current.tempToken,
          deviceId,
          smsCode,
          if (trustDevice) "1" else "0"
        )
      )
    }.body<LoginResponse>()
    _state.value = transitionState(data)
    return _state.value
  }

  // 校验会话票据有效性，返回角色代码。
  suspend fun check(tgt: String): String =
    http.post("$BASE_URL/check") {
      attributes.put(LoginTicket, tgt)
      setBody(CheckRequest(tgt))
    }.body<CheckResponse>().role

  // 重置流程状态，供重新开始一次登录时调用。
  fun reset() {
    _state.value = LoginState.Idle
  }

  fun close() {
    http.close()
  }

  // 接受响应后，返回新状态
  // login_code == "1" 登录成功，否则 login_result 即 temp_token，进入短信分支。
  private fun transitionState(data: LoginResponse): LoginState =
    if (data.loginCode == "1") {
      LoginState.Authenticated(
        ticket = data.loginTicket.ifEmpty { data.loginResult },
        profile = LoginProfile(
          name = data.name,
          avatar = data.avatar,
          xgh = data.xgh,
          roleType = data.roleType,
          roleName = data.role,
          depart = data.depart,
        ),
      )
    } else {
      LoginState.SmsRequired(data.loginResult)
    }
}
