package ink.chyk.pass.repositories

import ink.chyk.pass.api.webvpn.WebVpnApi
import ink.chyk.pass.api.webvpn.WebVpnCookiesStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * WebVPN 登录态。
 *
 * 上层只需要在请求前调用 [ensureLogin] 或让业务仓储在重登时调用 [login]；
 * 真正的 URL 改写和 Cookie 携带由 WebVpnPlugin、WebVpnCookiesStorage 完成。
 */
object WebVpnRepository : IRepository {
  private val api by lazy { WebVpnApi() }
  private val loginMutex = Mutex()

  /** 当前是否持有一张未过期的 WebVPN 票据；服务端提前失效仍由登录跳转暴露。 */
  suspend fun hasSession(): Boolean = WebVpnCookiesStorage.hasSession()

  /** 复用已有票据，没有时才走统一认证。 */
  suspend fun ensureLogin() {
    if (hasSession()) return
    login()
  }

  /** 强制换一张新票据，供目标站点会话失效后的重登流程使用。 */
  suspend fun login() = loginMutex.withLock {
    api.login { app -> AppAuthRepository.newAppTicket(app) }
  }

  override fun clearAllData() = api.clear()
}
