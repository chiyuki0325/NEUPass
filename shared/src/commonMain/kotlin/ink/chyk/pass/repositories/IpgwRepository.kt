package ink.chyk.pass.repositories

import ink.chyk.pass.api.CampusApp
import ink.chyk.pass.api.ipgw.*

object IpgwRepository {
  private val api by lazy { IpgwApi() }

  suspend fun getOnlineInfo(): IpgwInfo = api.getOnlineInfo()

  suspend fun login() {
    val acId = api.getAcId()
    val appTicket = AppAuthRepository.newAppTicket(CampusApp.ipGateway(acId))
    api.login(acId, appTicket)
  }

  suspend fun logout() {
    val onlineInfo = api.getOnlineInfo().onlineInfo ?: return
    api.logout(api.getAcId(), onlineInfo.userName, onlineInfo.onlineIp)
  }
}
