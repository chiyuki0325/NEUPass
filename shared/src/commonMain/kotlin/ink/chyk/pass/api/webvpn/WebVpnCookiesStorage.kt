@file:OptIn(kotlin.time.ExperimentalTime::class)

package ink.chyk.pass.api.webvpn

import io.ktor.client.plugins.cookies.*
import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

/**
 * WebVPN 自己的 Cookie 存储，只存在于内存中。
 *
 * WebVPN 用 `wengine_vpn_ticket...` 标识登录会话，`route` 把后续请求固定到同一台后端节点。
 *
 * 注意：接入 WebVPN 的业务必须删除自己的 Cookies storage，只保留 WebVpnCookiesStorage
 * 因为此时业务 cookies 是服务端负责维护，客户端收不到业务 cookies
 *
 * 票据不落盘：每次启动都重新走统一认证获取，避免复用已经失效的旧票据。
 */
object WebVpnCookiesStorage : CookiesStorage {
  private const val TICKET = "wengine_vpn_ticketwebvpn_neu_edu_cn"

  private data class StoredCookie(
    val cookie: Cookie,
    // 过期时间（epoch 毫秒）；null 表示会话 Cookie，进程存活期间有效
    val expiresAt: Long?,
  )

  private val mutex = Mutex()
  private val cookies = mutableMapOf<String, StoredCookie>()

  override suspend fun get(requestUrl: Url): List<Cookie> {
    if (requestUrl.host != WebVpnCrypto.HOST) return emptyList()
    return mutex.withLock {
      removeExpired(Clock.System.now().toEpochMilliseconds())
      cookies.values.map { it.cookie }
    }
  }

  override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
    if (requestUrl.host != WebVpnCrypto.HOST) return
    val now = Clock.System.now().toEpochMilliseconds()
    val maxAge = cookie.maxAge
    val expires = cookie.expires
    // 服务端用 Max-Age=0 或空值删除 Cookie；Max-Age 优先于 Expires。
    val expiresAt = when {
      maxAge != null -> now + maxAge * 1000L
      expires != null -> expires.timestamp
      else -> null
    }
    mutex.withLock {
      if (cookie.value.isEmpty() || (expiresAt != null && expiresAt <= now)) {
        cookies.remove(cookie.name)
      } else {
        cookies[cookie.name] = StoredCookie(cookie, expiresAt)
      }
    }
  }

  /** 票据存在且未过期。票据是否仍被服务端接受，以 WebVPN 的登录跳转响应为准。 */
  internal suspend fun hasSession(): Boolean = mutex.withLock {
    removeExpired(Clock.System.now().toEpochMilliseconds())
    cookies.containsKey(TICKET)
  }

  fun clear() {
    cookies.clear()
  }

  override fun close() = Unit

  private fun removeExpired(now: Long) {
    val expired = cookies.filterValues { it.expiresAt?.let { at -> at <= now } == true }.keys
    expired.forEach(cookies::remove)
  }
}
