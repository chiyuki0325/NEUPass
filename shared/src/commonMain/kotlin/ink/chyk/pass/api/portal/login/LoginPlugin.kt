@file:OptIn(ExperimentalTime::class)

package ink.chyk.pass.api.portal.login

import ink.chyk.pass.*
import ink.chyk.pass.api.portal.PortalEnvelope
import io.ktor.client.plugins.api.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.time.*

// app_login 协议（@docs/api/1. 登录接口（CASTGC）.md §2–§4）：
// 请求体编码为 JSON 后 RSA 加密放入表单字段 content，并签 authorization-str；
// 响应拆统一信封、校验 code、解密 data，再按调用方期望的类型反序列化。
class LoginPluginConfig {
  var json: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }
}

// 本次请求的会话票据，写入 authorization-str；登录前不设置，即空字符串。
val LoginTicket = AttributeKey<String>("LoginTicket")

// 信封 data 解密后的业务数据明文，留给 body() 反序列化。
private val LoginData = AttributeKey<String>("LoginData")

val LoginPlugin = createClientPlugin("LoginPlugin", ::LoginPluginConfig) {
  val json = pluginConfig.json

  // onRequest：每次请求前附加请求签名 authorization-str
  onRequest { request, _ ->
    val auth = AuthorizationStr(
      timestamp = Clock.System.now().toEpochMilliseconds().toString(),
      ticket = request.attributes.getOrNull(LoginTicket).orEmpty(),
    )
    request.headers["authorization-str"] = RsaCodec.encrypt(json.encodeToString(auth))
  }

  // transformRequestBody：将请求体 JSON 编码后 RSA 加密放入表单字段 content
  transformRequestBody { _, body, bodyType ->
    val type = bodyType?.kotlinType
    if (body is OutgoingContent || type == null) return@transformRequestBody null
    val plain = json.encodeToString(json.serializersModule.serializer(type), body)
    FormDataContent(parameters { append("content", RsaCodec.encrypt(plain)) })
  }

  // onResponse：拆信封、校验 code、解密 data
  // 此处不反序列化业务 json，留给调用方在 body() 中按期望类型反序列化。
  onResponse { response ->
    if (!response.status.isSuccess()) return@onResponse
    val envelope = json.decodeFromString<PortalEnvelope<String>>(response.bodyAsText())
    if (envelope.code != 0) {
      // 10013 会话失效由上层（repo 层）静默重登
      if (envelope.code == 10013) throw SessionExpiredException()
      throw LoginFailedException(envelope.message)
    }
    envelope.data?.let { response.call.attributes.put(LoginData, RsaCodec.decrypt(it)) }
  }

  transformResponseBody { response, _, requestedType ->
    val type = requestedType.kotlinType ?: return@transformResponseBody null
    val plain = response.call.attributes.getOrNull(LoginData) ?: "{}"
    json.decodeFromString(json.serializersModule.serializer(type), plain)
  }
}
