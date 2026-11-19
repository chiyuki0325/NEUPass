package ink.chyk.pass.api.ipgw

import ink.chyk.pass.api.createApiClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class IpgwApi {
  companion object {
    private const val BASE_URL = "http://ipgw.neu.edu.cn"
  }

  private val redirectHttp = createApiClient {
    followRedirects = false
    expectSuccess = false
  }

  private val http = createApiClient {
    install(IpgwPlugin)
  }

  suspend fun getAcId(): String {
    var url = Url(BASE_URL)
    repeat(5) {
      val response = redirectHttp.get(url)
      if (response.status.value !in 300..399) throw IpgwException("无法获取 ac_id")
      val location = response.headers[HttpHeaders.Location] ?: throw IpgwException("无法获取 ac_id")
      url = URLBuilder(url).takeFrom(location).build()
      url.parameters["ac_id"]?.let { return it }
    }
    throw IpgwException("无法获取 ac_id")
  }

  suspend fun login(acId: String, appTicket: String) {
    val response = http.get("$BASE_URL/v1/srun_portal_sso") {
      parameter("ac_id", acId)
      parameter("ticket", appTicket)
    }.body<IpgwLoginResponse>()
    when (response.code) {
      0 -> return
      1 -> when (response.message) {
        "E2616: Arrearage users." -> throw IpgwArrearageUserException()
        "no_response_data_error" -> throw IpgwTooLongNoLoginException()
        else -> throw IpgwException(response.message)
      }
      else -> throw IpgwException("${response.code}: ${response.message}")
    }
  }

  suspend fun getOnlineInfo(): IpgwInfo {
    repeat(5) {
      val info = http.get("$BASE_URL/cgi-bin/rad_user_info").body<IpgwInfo>()
      if (!info.isOnline || info.onlineInfo?.billingName?.isNotBlank() == true) return info
    }
    throw IpgwException("获取在线信息失败")
  }

  suspend fun logout(acId: String, username: String, ip: String) {
    val response = http.get("$BASE_URL/cgi-bin/srun_portal") {
      parameter("action", "logout")
      parameter("username", username)
      parameter("ip", ip)
      parameter("ac_id", acId)
    }.body<IpgwLogoutResponse>()
    if (response.error != "ok") {
      throw IpgwException(response.errorMsg.ifBlank { response.error })
    }
  }

  fun close() {
    redirectHttp.close()
    http.close()
  }
}
