package ink.chyk.pass.api.ecode

import ink.chyk.pass.*
import ink.chyk.pass.api.CampusApp
import ink.chyk.pass.api.createApiClient
import io.ktor.client.call.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

private val LOGIN_URL = CampusApp.ECODE.callbackUrl
private const val QR_CODE_URL = "https://ecode.neu.edu.cn/ecode/api/qr-code"
private const val USER_INFO_URL = "https://ecode.neu.edu.cn/ecode/api/user-info"

internal const val ECODE_LOGIN_PATH = "/ecode/api/sso/login"
internal const val ECODE_REDIRECT_URL = "https://ecode.neu.edu.cn/ecode/#/"

// 一码通（ecode）API
class ECodeApi {
  // 登录/会话建立：只看 3xx 与 Location，然后往 ECodeCookiesStorage 里写入 xsrf-token 等会话数据
  private val loginHttp = createApiClient {
    followRedirects = false
    expectSuccess = false
    install(HttpCookies) { storage = ECodeCookiesStorage }
    install(ECodeSessionPlugin)
  }

  // 业务请求：采用之前写好的会话
  private val http = createApiClient {
    followRedirects = false
    expectSuccess = false
    install(HttpCookies) { storage = ECodeCookiesStorage }
    install(ECodeSessionPlugin)
    install(ECodeBizPlugin)
  }

  // 用统一认证换出的 ST 建立一码通会话。
  suspend fun login(appTicket: String) {
    // 先清掉旧 Cookie，避免第一跳被服务端当成已登录会话处理
    ECodeCookiesStorage.clear()

    // 拿一个会话 cookie 先
    val bootstrap = loginHttp.get(LOGIN_URL)
    if (bootstrap.status.value >= 400) throw RequestFailedException(LOGIN_URL)
    if (!ECodeCookiesStorage.hasSession()) {
      throw RequestFailedException("$LOGIN_URL 未下发会话 Cookie")
    }

    // 消费 ST；成功时服务端返回重定向。
    loginHttp.get(LOGIN_URL) {
      parameter("ticket", appTicket)
    }.requireRedirect()

    // 带着会话 cookie 再访问一次登录入口，确认最终回到一码通首页。
    loginHttp.get(LOGIN_URL).requireRedirect(ECODE_REDIRECT_URL)
  }

  suspend fun getQrCode(): ECodeQrCode = http.get(QR_CODE_URL).body()

  suspend fun getUserInfo(): ECodeUserInfo = http.get(USER_INFO_URL).body()

  fun clearSession() {
    ECodeCookiesStorage.clear()
  }

  fun close() {
    loginHttp.close()
    http.close()
  }
}

private fun HttpResponse.requireRedirect(expectedLocation: String? = null) {
  val location = headers[HttpHeaders.Location]
  if (status.value !in 300..399 || location == null) {
    // 服务端错误不当作票据失效，避免无意义地重换 ST。
    if (status.value >= 500) throw RequestFailedException(call.request.url.toString())
    throw TicketExpiredException()
  }
  if (expectedLocation != null && location != expectedLocation) throw TicketExpiredException()
}
