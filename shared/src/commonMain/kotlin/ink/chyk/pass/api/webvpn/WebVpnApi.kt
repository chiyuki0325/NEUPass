package ink.chyk.pass.api.webvpn

import ink.chyk.pass.*
import ink.chyk.pass.api.CampusApp
import ink.chyk.pass.api.createApiClient
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*

/**
 * WebVPN 登录客户端。
 *
 * WebVPN 的登录入口是 `/login?cas_login=true`：先由统一认证换出绑定该回调地址的 ST，
 * 再把 ST 交回 WebVPN，服务端校验后写入 `wengine_vpn_ticket...` 并跳到
 * `/wengine-vpn-token-login` 完成会话绑定。只拿到第一跳的 Cookie 还不算登录，
 * 必须跟随到 token-login 页面。
 */
class WebVpnApi {
  private val http = createApiClient {
    followRedirects = true
    expectSuccess = false
    install(HttpCookies) { storage = WebVpnCookiesStorage }
    install(WebVpnPlugin)
  }

  suspend fun login(newAppTicket: suspend (CampusApp) -> String) {
    WebVpnCookiesStorage.clear()
    try {
      val appTicket = newAppTicket(CampusApp.WEBVPN)
      val response = http.get(CampusApp.WEBVPN.callbackUrl) {
        parameter("ticket", appTicket)
      }

      if (!response.status.isSuccess()) {
        throw RequestFailedException(response.call.request.url.toString())
      }
      // 失败时统一认证会接管页面，最终地址落在代理后的 tpass 登录页；
      // 成功时会停在 WebVPN 的 token-login 绑定页，已登录时则直接回到站点首页。
      val path = response.call.request.url.encodedPath
      if (path != "/" && !path.startsWith("/wengine-vpn-token-login")) {
        throw TicketExpiredException()
      }
      if (!WebVpnCookiesStorage.hasSession()) throw TicketExpiredException()
    } catch (e: CancellationException) {
      WebVpnCookiesStorage.clear()
      throw e
    } catch (e: Exception) {
      // 校园网访问 WebVPN 会返回 403，且同样带 Set-Cookie；失败时不能留下假登录态
      WebVpnCookiesStorage.clear()
      throw e
    }
  }

  fun clear() {
    WebVpnCookiesStorage.clear()
  }

  fun close() {
    http.close()
  }
}
