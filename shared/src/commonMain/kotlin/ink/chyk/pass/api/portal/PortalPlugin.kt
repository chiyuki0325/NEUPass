package ink.chyk.pass.api.portal

import ink.chyk.pass.*
import io.ktor.client.plugins.api.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

class PortalPluginConfig {
  var json: Json = Json { ignoreUnknownKeys = true }
}

private val PortalData = AttributeKey<JsonElement>("PortalData")

val PortalPlugin = createClientPlugin("PortalPlugin", ::PortalPluginConfig) {
  val json = pluginConfig.json

  onResponse { response ->
    val status = response.status.value
    if (status in 300..399 || status == 401 || status == 403) {
      // 状态码不对 抛上层重新登录
      throw SessionExpiredException("backend returned HTTP $status")
    }
    if (!response.status.isSuccess()) {
      // 其他非 2xx 状态码，抛异常
      throw RequestFailedException(response.call.request.url.toString())
    }

    // 解开 e/m/d 外层信封
    val envelope = json.decodeFromString<PortalEnvelope<JsonElement>>(response.bodyAsText())
    // 大于 10000 的 code 是 session 过期，抛异常让上层重新登录
    if (envelope.code > 10000) {
      throw SessionExpiredException("${envelope.code}, ${envelope.message}")
    }
    // 其它非 0 报错
    if (envelope.code > 0) {
      throw EHallPortalException("${envelope.code}, ${envelope.message}")
    }
    // 含有 loginUrl 的响应也表示 session 过期
    val loginUrl = (envelope.data as? JsonObject)?.get("loginUrl") as? JsonPrimitive
    if (loginUrl?.isString == true) throw SessionExpiredException()

    // 成功响应
    response.call.attributes.put(PortalData, envelope.data ?: JsonNull)
  }

  transformResponseBody { response, _, requestedType ->
    val type = requestedType.kotlinType ?: return@transformResponseBody null
    json.decodeFromJsonElement(
      json.serializersModule.serializer(type),
      response.call.attributes[PortalData],
    )
  }
}
