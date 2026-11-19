package ink.chyk.pass.api.ecode

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ECodeQrCode(
  val qrCode: String,
  val createTime: Long,
  val qrInvalidTime: Long,
)

@Serializable
data class ECodeUserInfo(
  val userCode: String,
  val userName: String,
  val unitName: String,
  val idType: String,
)

// 一码通业务响应：{"data":[{"type":null,"attributes":{...}}]}
@Serializable
internal data class ECodeListedResponse(
  val data: List<ECodeListedItem>,
)

@Serializable
internal data class ECodeListedItem(
  val attributes: JsonElement,
)
