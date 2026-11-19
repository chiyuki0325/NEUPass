@file:OptIn(kotlin.time.ExperimentalTime::class)

package ink.chyk.pass.api.campus_run

import ink.chyk.pass.*
import io.ktor.client.plugins.api.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.time.Clock

// 校园跑协议：会话参数补齐 nonce 与秒级时间戳，MD5 签名后 AES 加密放入表单字段 data；
// 响应拆信封、解密 data，再按调用方期望的类型反序列化。
class CampusRunPluginConfig {
  var json: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }
}

// 信封 data 解密后的业务数据明文，留给 body() 反序列化。
private val CampusRunData = AttributeKey<String>("CampusRunData")

val CampusRunPlugin = createClientPlugin("CampusRunPlugin", ::CampusRunPluginConfig) {
  val json = pluginConfig.json

  // onRequest：附加乐跑 H5 Referer 与客户端标识，覆盖通用的 X-Requested-With
  onRequest { request, _ ->
    request.headers[HttpHeaders.Referrer] =
      "https://tybzhtypt.neu.edu.cn/bdlp_h5_fitness_test/view/db/"
    request.headers["X-Requested-With"] = "com.sunyt.testdemo"
  }

  // transformRequestBody：补齐动态参数、签名，将 JSON 加密后编码为表单
  transformRequestBody { _, body, _ ->
    if (body !is CampusRunSession) return@transformRequestBody null
    val payload = json.encodeToJsonElement(
      body.toRequest(
        nonce = (100000..999999).random().toString(),
        timestamp = Clock.System.now().toEpochMilliseconds() / 1000,
      ),
    ).jsonObject
    // 签名前按字段名排序，拼接字段名与值；sign 本身不参与，盐值由 Crypto 附加。
    val textToHash = payload.entries.sortedBy { it.key }
      .joinToString("") { (key, value) -> key + value.jsonPrimitive.content }
    val signed = JsonObject(payload + ("sign" to JsonPrimitive(CampusRunCrypto.signMD5(textToHash))))
    FormDataContent(parameters {
      append("ostype", "5")
      append("data", CampusRunCrypto.aesEncrypt(json.encodeToString(signed)))
    })
  }

  // onResponse：归类 HTTP 错误、拆信封、解密 data
  // 此处不反序列化业务 JSON，留给调用方在 body() 中按期望类型反序列化。
  onResponse { response ->
    val status = response.status.value
    if (status in 300..399 || status == 401 || status == 403) {
      throw SessionExpiredException("校园跑返回 HTTP $status")
    }
    if (!response.status.isSuccess()) {
      throw RequestFailedException(response.call.request.url.toString())
    }
    // 掉登录时可能返回非信封响应，转为会话失效，由 repo 层静默重登。
    val envelope = try {
      json.decodeFromString<CampusRunResponse>(response.bodyAsText())
    } catch (_: SerializationException) {
      throw SessionExpiredException("校园跑返回了未认证响应")
    }
    response.call.attributes.put(CampusRunData, CampusRunCrypto.aesDecrypt(envelope.data))
  }

  // transformResponseBody：反序列化解密后的业务响应，沿用旧接口的会话异常归类
  transformResponseBody { response, _, requestedType ->
    val type = requestedType.kotlinType ?: return@transformResponseBody null
    try {
      json.decodeFromString(
        json.serializersModule.serializer(type),
        response.call.attributes[CampusRunData],
      )
    } catch (_: SerializationException) {
      throw SessionExpiredException("校园跑 session 响应无法解析")
    }
  }
}
