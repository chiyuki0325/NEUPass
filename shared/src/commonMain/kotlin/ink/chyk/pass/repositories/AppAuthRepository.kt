package ink.chyk.pass.repositories

import ink.chyk.pass.api.CampusApp
import ink.chyk.pass.api.pass.AppAuthApi

object AppAuthRepository : IRepository {
  private val appAuthApi by lazy { AppAuthApi() }

  /** 持 CASTGC 为目标应用换一张 ST。 */
  suspend fun newAppTicket(app: CampusApp): String =
    LoginRepository.withLoginTicket { loginTicket ->
      appAuthApi.requestAppTicket(loginTicket, app.callbackUrl.substringBefore('#'))
    }

  /** 换票并拼出可直接在浏览器打开的登录地址。 */
  suspend fun loginWebApp(app: CampusApp): String {
    val appTicket = newAppTicket(app)
    val serviceUrl = app.callbackUrl.substringBefore('#')
    val fragment = app.callbackUrl.substringAfter('#', "")
    return serviceUrl + (if (serviceUrl.contains('?')) "&" else "?") + "ticket=$appTicket" +
      (if (fragment.isNotEmpty()) "#$fragment" else "")
  }

  override fun clearAllData() {
    // 当前不缓存 ST，无本地数据需要清理
  }
}
