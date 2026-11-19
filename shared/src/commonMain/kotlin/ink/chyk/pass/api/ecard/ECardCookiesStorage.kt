package ink.chyk.pass.api.ecard

import io.ktor.client.plugins.cookies.*
import io.ktor.http.*

// 自助查询平台的登录会话由服务端下发的多个 Cookie 共同维持：
// ASP.NET_SessionId 标识会话，.ASPXAUTSSM 是登录凭证，每次登录都会先清空再重设；
// SelfSearch 后续页面的 WebForms 回发也沿用同一套 Cookie。
// 这里按主机收下 ecard.neu.edu.cn 的全部 Cookie，与浏览器行为保持一致。
object ECardCookiesStorage : CookiesStorage {
  private const val HOST = "ecard.neu.edu.cn"

  private val cookies = mutableMapOf<String, Cookie>()

  override suspend fun get(requestUrl: Url): List<Cookie> =
    if (requestUrl.host == HOST) cookies.values.toList() else emptyList()

  override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
    // 服务端用空值 Cookie 清旧凭证，真正的凭证由紧随其后的同名 Set-Cookie 下发
    if (requestUrl.host != HOST || cookie.value.isEmpty()) return
    cookies[cookie.name] = cookie
  }

  fun clear() {
    cookies.clear()
  }

  override fun close() = Unit
}
