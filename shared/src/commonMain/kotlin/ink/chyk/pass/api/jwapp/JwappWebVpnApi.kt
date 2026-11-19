package ink.chyk.pass.api.jwapp

import ink.chyk.pass.*
import ink.chyk.pass.api.CampusApp
import ink.chyk.pass.api.createApiClient
import ink.chyk.pass.api.webvpn.*
import io.ktor.client.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

/**
 * 校外通过 WebVPN 访问教务系统。
 *
 * 业务请求仍按原始教务地址发出，由 WebVpnPlugin 改写成代理地址；
 * 目标站点的会话由 WebVPN 服务端按票据保存，客户端只维护 WebVPN Cookie。
 */
internal class JwappWebVpnApi private constructor(
  private val loginHttp: HttpClient,
  private val http: HttpClient,
) : JwappRoutes(http) {
  constructor() : this(
    // WebVPN 登录：请求按原地址发出，由 WebVpnPlugin 改写成代理地址，并跟随 CAS 重定向。
    loginHttp = createApiClient {
      followRedirects = true
      expectSuccess = false
      install(HttpCookies) { storage = WebVpnCookiesStorage }
      install(WebVpnPlugin)
    },
    // 业务请求由 WebVPN 服务端按票据转发，客户端不需要跟随目标站点的跳转。
    http = createApiClient {
      followRedirects = false
      expectSuccess = false
      install(HttpCookies) { storage = WebVpnCookiesStorage }
      install(WebVpnPlugin)
      install(JwappPlugin)
    },
  )

  // 调用前需要先由 WebVpnRepository 建立 WebVPN 票据。
  suspend fun login(newAppTicket: suspend (CampusApp) -> String) {
    val appTicket = newAppTicket(CampusApp.JWAPP)
    val response = loginHttp.get(CampusApp.JWAPP.callbackUrl) {
      parameter("ticket", appTicket)
    }
    if (!response.status.isSuccess() || !WebVpnCookiesStorage.hasSession()) {
      throw TicketExpiredException()
    }
    // 票据无效时 WebVPN 会把统一认证登录页代理回来；只有最终仍停在教务主机上才算登录成功。
    val targetPrefix = "/https/${WebVpnCrypto.hostSegment(CampusApp.JWAPP.callbackUrl)}/"
    if (!response.request.url.encodedPath.startsWith(targetPrefix)) {
      throw TicketExpiredException()
    }
  }

  fun close() {
    loginHttp.close()
    http.close()
  }
}
