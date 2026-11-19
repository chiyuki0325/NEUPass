package ink.chyk.pass.api.jwapp

import ink.chyk.pass.*
import ink.chyk.pass.api.CampusApp
import ink.chyk.pass.api.createApiClient
import io.ktor.client.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.http.*

// 校内直连教务系统。是否使用直连由 JwappRepository 判断。
internal class JwappApi private constructor(
  private val loginHttp: HttpClient,
  private val http: HttpClient,
) : JwappRoutes(http) {
  constructor() : this(
    // 登录：跟随 CAS 重定向，_WEU 由 JwappCookiesStorage 保存。
    loginHttp = createApiClient {
      followRedirects = true
      expectSuccess = false
      install(HttpCookies) { storage = JwappCookiesStorage }
    },
    // 业务请求：未登录时教务系统会 302 到统一认证，由 JwappPlugin 归类为会话过期。
    http = createApiClient {
      followRedirects = false
      expectSuccess = false
      install(HttpCookies) { storage = JwappCookiesStorage }
      install(JwappPlugin)
    },
  )

  suspend fun login(newAppTicket: suspend (CampusApp) -> String) {
    JwappCookiesStorage.clear()
    val appTicket = newAppTicket(CampusApp.JWAPP)
    val response = loginHttp.get(CampusApp.JWAPP.callbackUrl) {
      parameter("ticket", appTicket)
    }
    if (!response.status.isSuccess() || !JwappCookiesStorage.hasSession()) {
      throw TicketExpiredException()
    }
  }

  fun close() {
    loginHttp.close()
    http.close()
  }
}
