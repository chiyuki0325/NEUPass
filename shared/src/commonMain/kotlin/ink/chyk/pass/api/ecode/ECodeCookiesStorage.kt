package ink.chyk.pass.api.ecode

import com.ctrip.flight.mmkv.defaultMMKV
import io.ktor.client.plugins.cookies.*
import io.ktor.http.*

object ECodeCookiesStorage : CookiesStorage {
  private val mmkv by lazy { defaultMMKV() }

  private const val HOST = "ecode.neu.edu.cn"
  private const val SESSION_KEY = "ecode_session"
  private const val XSRF_KEY = "ecode_xsrf_token"

  private val keys = mapOf(
    "SESSION" to SESSION_KEY,
    "XSRF-TOKEN" to XSRF_KEY,
  )

  override suspend fun get(requestUrl: Url): List<Cookie> {
    if (requestUrl.host != HOST) return emptyList()

    val cookies = keys.mapNotNull { (name, key) ->
      if (mmkv.containsKey(key)) Cookie(name, mmkv.getString(key), encoding = CookieEncoding.RAW) else null
    }

    // 首次 bootstrap 还没有 SESSION，不带 KC_REDIRECT；
    // 之后的登录请求带上它，服务端才会在确认跳转时回到一码通首页。
    return if (requestUrl.encodedPath == ECODE_LOGIN_PATH && cookies.any { it.name == "SESSION" }) {
      cookies + Cookie("KC_REDIRECT", ECODE_REDIRECT_URL, encoding = CookieEncoding.RAW)
    } else {
      cookies
    }
  }

  override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
    if (requestUrl.host != HOST || cookie.value.isEmpty()) return
    val key = keys[cookie.name] ?: return
    mmkv[key] = cookie.value
  }

  internal fun hasSession(): Boolean =
    mmkv.containsKey(SESSION_KEY) && mmkv.containsKey(XSRF_KEY)

  internal fun xsrfToken(): String? =
    if (mmkv.containsKey(XSRF_KEY)) mmkv.getString(XSRF_KEY) else null

  fun clear() {
    keys.values.forEach { mmkv.removeValueForKey(it) }
  }

  override fun close() = Unit
}
