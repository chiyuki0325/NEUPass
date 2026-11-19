package ink.chyk.pass.api.portal

import ink.chyk.pass.*
import io.ktor.client.plugins.api.*
import io.ktor.http.*

internal val PortalLoginPlugin = createClientPlugin("PortalLoginPlugin") {
  onResponse { response ->
    if (response.status.value >= 500) {
      throw RequestFailedException(response.call.request.url.toString())
    }
    if (response.status != HttpStatusCode.Found) throw TicketExpiredException()

    val cookies = response.setCookie()
    val sessionCookies = listOf("CK_LC", "CK_VL", "SESS_ID").map { name ->
      cookies.firstOrNull { it.name == name && it.value.isNotEmpty() }
        ?: throw TicketExpiredException()
    }
    sessionCookies.forEach { cookie ->
      PortalCookiesStorage.addCookie(response.call.request.url, cookie)
    }
  }
}
