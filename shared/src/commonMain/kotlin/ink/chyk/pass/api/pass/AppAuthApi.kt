package ink.chyk.pass.api.pass

import ink.chyk.pass.api.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class AppAuthApi {
  // CASTGC->ST

  private val http = createApiClient {
    followRedirects = false
    expectSuccess = false
    install(AppAuthPlugin)
  }

  suspend fun requestAppTicket(loginTicket: String, callbackUrl: String): String =
    http.get("https://pass.neu.edu.cn/tpass/login") {
      parameter("service", callbackUrl)
      attributes.put(AppLoginTicket, loginTicket)
    }.body<String>()

  fun close() {
    http.close()
  }
}
