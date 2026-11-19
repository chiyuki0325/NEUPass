package ink.chyk.pass.api.portal

import com.ctrip.flight.mmkv.defaultMMKV
import io.ktor.client.plugins.cookies.*
import io.ktor.http.*

object PortalCookiesStorage : CookiesStorage {
  private val mmkv by lazy { defaultMMKV() }
  private const val PORTAL_HOST = "personal.neu.edu.cn"

  private val keys = mapOf(
    "CK_LC" to "portal_lc",
    "CK_VL" to "portal_vl",
    "SESS_ID" to "portal_sess_id",
  )

  override suspend fun get(requestUrl: Url): List<Cookie> {
    if (requestUrl.host != PORTAL_HOST) return emptyList()
    return keys.mapNotNull { (name, key) ->
      if (mmkv.containsKey(key)) Cookie(name, mmkv.getString(key), encoding = CookieEncoding.RAW) else null
    }
  }

  override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
    if (requestUrl.host != PORTAL_HOST || cookie.value.isEmpty()) return
    val key = keys[cookie.name] ?: return
    mmkv[key] = cookie.value
  }

  fun clear() {
    keys.values.forEach { mmkv.removeValueForKey(it) }
  }

  override fun close() = Unit
}
