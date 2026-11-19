package ink.chyk.pass.api.portal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*

// 门户统一响应信封 {"e": 状态码, "m": 提示文案, "d": 业务数据}。
// app_login 的 d 是加密后的 Base64 字符串，其他门户接口的 d 是明文 JSON。
@Serializable
data class PortalEnvelope<T>(
  @Serializable(with = LenientIntSerializer::class)
  @SerialName("e")
  val code: Int,

  @SerialName("m")
  val message: String = "",

  @SerialName("d")
  val data: T? = null,
)

@Serializable
data class UserInfoResponse(
  val info: UserInfo,
)

@Serializable
data class UserInfo(
  val uid: String,
  val name: String,
  val xgh: String, // 学工号
  val identity: String,
  @SerialName("identity_id") val identityId: String,
  val sex: Int,
  val depart: String,
  val mobile: String,
  val email: String,
  val organ: JsonElement,  // 不明
  val organs: JsonElement,  // 不明
  val avatar: String,  // 其实这个才是真正的 avatar_url
  @SerialName("avatar_url") val avatarUrl: String,  // 这个是空字符串，意义不明
  val time: String,
  @SerialName("is_manager") val isManager: Boolean,
  @SerialName("is_app_manager") val isAppManager: Boolean,
  @SerialName("is_process_manager") val isProcessManager: Boolean,
)

@Serializable
data class DataItemIdsResponse(
  val data: List<DataItemId>,
) {
  // 只保留客户端认识的 key，协议 key 与数据库项 id 的映射留在 API 层。
  fun idMap(): Map<PortalDataItemKey, String> =
    data.mapNotNull { item -> PortalDataItemKey.fromString(item.key)?.let { it to item.id } }.toMap()
}

@Serializable
data class DataItemId(
  val key: String,
  val id: String,
  // 只需要这两个字段
)

// 门户「个人数据」条目在协议中的 key。
@Serializable
enum class PortalDataItemKey(val key: String) {
  @SerialName("card.balance")
  CARD_BALANCE("card.balance"),

  @SerialName("net.balance")
  NET_BALANCE("net.balance");

  companion object {
    private val map = entries.associateBy(PortalDataItemKey::key)
    fun fromString(key: String) = map[key]
  }
}

@Serializable
data class DataItemResponse(
  val data: DataItem,
)

@Serializable
data class DataItem(
  val value: JsonElement,
  val unit: String? = null,
  val url: String? = null,
) {
  val valueString: String? by lazy {
    value.jsonPrimitive.content
  }
}

@Serializable
data class UploadImageResponse(
  val url: String,
  val name: String,
  val type: String,
  val size: Int,
  val id: String,
)


// 一个宽松的 Int 序列化器，因为 门户接口的 code 字段有时会返回字符串类型的数字。
object LenientIntSerializer : KSerializer<Int> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("ink.chyk.pass.LenientInt", PrimitiveKind.INT)

  override fun deserialize(decoder: Decoder): Int =
    (decoder as JsonDecoder).decodeJsonElement().jsonPrimitive.content.toInt()

  override fun serialize(encoder: Encoder, value: Int) = encoder.encodeInt(value)
}
