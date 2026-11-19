package ink.chyk.pass.api.ipgw

import kotlinx.serialization.*

@Serializable
data class IpgwLoginResponse(
  val code: Int,
  val message: String,
  @SerialName("Redirect") val redirect: String = "",
  @SerialName("ID") val id: String = "",
)

@Serializable
data class IpgwOnlineInfo(
  @SerialName("ServerFlag") val serverFlag: Long,
  @SerialName("add_time") val addTime: Long,
  @SerialName("all_bytes") val allBytes: Long,
  @SerialName("billing_name") val billingName: String,
  @SerialName("bytes_in") val bytesIn: Long,
  @SerialName("bytes_out") val bytesOut: Long,
  @SerialName("checkout_date") val checkoutDate: Long,
  val domain: String,
  val error: String,
  @SerialName("group_id") val groupId: String,
  @SerialName("keepalive_time") val keepaliveTime: Long,
  @SerialName("online_device_total") val onlineDeviceTotal: String,
  @SerialName("online_ip") val onlineIp: String,
  @SerialName("online_ip6") val onlineIp6: String,
  @SerialName("package_id") val packageId: String,
  @SerialName("products_id") val productsId: String,
  @SerialName("products_name") val productsName: String,
  @SerialName("real_name") val realName: String,
  @SerialName("remain_bytes") val remainBytes: Long,
  @SerialName("remain_seconds") val remainSeconds: Long,
  @SerialName("sum_bytes") val sumBytes: Long,
  @SerialName("sum_seconds") val sumSeconds: Long,
  @SerialName("sysver") val sysVer: String,
  @SerialName("user_balance") val userBalance: Double,
  @SerialName("user_charge") val userCharge: Double,
  @SerialName("user_mac") val userMac: String,
  @SerialName("user_name") val userName: String,
  @SerialName("wallet_balance") val walletBalance: Double,
)

@Serializable
data class IpgwOfflineInfo(
  @SerialName("client_ip") val clientIp: String,
  @SerialName("ecode") val errorCode: Int,
  val error: String,
  @SerialName("error_msg") val errorMsg: String,
  @SerialName("online_ip") val onlineIp: String,
  val res: String,
  @SerialName("srun_ver") val srunVer: String,
  val st: Long,
)

data class IpgwInfo(
  val isOnline: Boolean,
  val onlineInfo: IpgwOnlineInfo? = null,
  val offlineInfo: IpgwOfflineInfo? = null,
)

@Serializable
data class IpgwLogoutResponse(
  val error: String,
  @SerialName("error_msg") val errorMsg: String = "",
)
