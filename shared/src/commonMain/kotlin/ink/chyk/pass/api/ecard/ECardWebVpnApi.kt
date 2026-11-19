package ink.chyk.pass.api.ecard

import ink.chyk.pass.TicketExpiredException
import ink.chyk.pass.api.createApiClient
import ink.chyk.pass.api.webvpn.WebVpnCookiesStorage
import ink.chyk.pass.api.webvpn.WebVpnCrypto
import ink.chyk.pass.api.webvpn.WebVpnPlugin
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.http.Url

object ECardWebVpnApi : ECardRoutes(
  loginHttp = createApiClient {
    followRedirects = false
    expectSuccess = false
    install(HttpCookies) { storage = WebVpnCookiesStorage }
    install(WebVpnPlugin)
  },
  http = createApiClient {
    followRedirects = false
    expectSuccess = false
    install(HttpCookies) { storage = WebVpnCookiesStorage }
    install(WebVpnPlugin)
    install(ECardPlugin)
  },
) {
  override suspend fun login(appTicket: String) {
    val proxyIndexUrl = WebVpnCrypto.encode(ECARD_INDEX_URL)
    val indexLocations = setOf(ECARD_INDEX_PATH, ECARD_INDEX_URL, proxyIndexUrl, Url(proxyIndexUrl).encodedPath)
    if (exchangeTicket(appTicket) !in indexLocations || !WebVpnCookiesStorage.hasSession()) {
      throw TicketExpiredException()
    }
  }
}
