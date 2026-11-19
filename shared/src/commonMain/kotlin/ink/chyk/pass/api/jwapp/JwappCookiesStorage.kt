package ink.chyk.pass.api.jwapp

import com.ctrip.flight.mmkv.defaultMMKV
import io.ktor.client.plugins.cookies.*
import io.ktor.http.*

object JwappCookiesStorage : CookiesStorage {
  private val mmkv by lazy { defaultMMKV() }

  private const val HOST = "jwxt.neu.edu.cn"
  private const val WEU_KEY = "jwapp_weu"

  override suspend fun get(requestUrl: Url): List<Cookie> {
    if (requestUrl.host != HOST) return emptyList()
    return if (mmkv.containsKey(WEU_KEY)) {
      listOf(Cookie("_WEU", mmkv.getString(WEU_KEY), encoding = CookieEncoding.RAW))
    } else {
      emptyList()
    }
  }

  override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
    if (requestUrl.host != HOST || cookie.name != "_WEU" || cookie.value.isEmpty()) return
    mmkv[WEU_KEY] = cookie.value
  }

  internal fun hasSession(): Boolean = mmkv.containsKey(WEU_KEY)

  fun clear() {
    mmkv.removeValueForKey(WEU_KEY)
  }

  override fun close() = Unit
}
