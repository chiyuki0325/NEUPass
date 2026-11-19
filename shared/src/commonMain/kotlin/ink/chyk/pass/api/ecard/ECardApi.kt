package ink.chyk.pass.api.ecard

import ink.chyk.pass.TicketExpiredException
import ink.chyk.pass.api.createApiClient
import io.ktor.client.plugins.cookies.HttpCookies

object ECardApi : ECardRoutes(
  loginHttp = createApiClient {
    followRedirects = false
    expectSuccess = false
    install(HttpCookies) { storage = ECardCookiesStorage }
  },
  http = createApiClient {
    followRedirects = false
    expectSuccess = false
    install(HttpCookies) { storage = ECardCookiesStorage }
    install(ECardPlugin)
  },
) {
  override suspend fun login(appTicket: String) {
    ECardCookiesStorage.clear()
    if (exchangeTicket(appTicket) !in setOf(ECARD_INDEX_PATH, ECARD_INDEX_URL)) {
      throw TicketExpiredException()
    }
  }

  fun clearSession() {
    ECardCookiesStorage.clear()
    ECardViewStateStorage.clearAll()
  }
}
