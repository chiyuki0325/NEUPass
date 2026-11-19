package ink.chyk.pass.api.portal.login

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 登录接口 content 明文，字段顺序固定。
@Serializable
data class LoginRequest(
  val username: String,
  val password: String,
  val imei: String,
  @SerialName("mobile_type") val mobileType: String,
)

// 会话校验 content 明文。
@Serializable
data class CheckRequest(val ticket: String)

// 发送验证码 content 明文。
@Serializable
data class SmsSendRequest(
  @SerialName("temp_token") val tempToken: String,
  val imei: String,
)

// 提交验证码 content 明文。
@Serializable
data class VerifyCommitRequest(
  @SerialName("temp_token") val tempToken: String,
  val imei: String,
  @SerialName("sms_code") val smsCode: String,
  @SerialName("trust_device") val trustDevice: String,
)

// commit / verifyCommit 解密后的业务数据，所有字段均为字符串。
// login_code == "1" 登录成功；否则 login_result 即短信二次验证的 temp_token。
@Serializable
data class LoginResponse(
  @SerialName("login_code") val loginCode: String = "",
  @SerialName("login_result") val loginResult: String = "",
  @SerialName("login_ticket") val loginTicket: String = "",
  val name: String = "",
  val avatar: String = "",
  val xgh: String = "",
  @SerialName("role_type") val roleType: String = "",
  val role: String = "",
  val depart: String = "",
  @SerialName("CK_LC") val ckLc: String = "",
  @SerialName("CK_VL") val ckVl: String = "",
)

// check 解密后的业务数据。
@Serializable
data class CheckResponse(val role: String = "")

// 登录成功返回的账号资料。
data class LoginProfile(
  val name: String = "",
  val avatar: String = "",
  val xgh: String = "",
  val roleType: String = "",
  val roleName: String = "",
  val depart: String = "",
)

// authorization-str 请求头的明文，编码为紧凑 JSON；timestamp 为字符串类型。
@Serializable
data class AuthorizationStr(val timestamp: String, val ticket: String)
