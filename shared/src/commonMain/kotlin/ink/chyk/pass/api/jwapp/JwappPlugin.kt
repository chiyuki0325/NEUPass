package ink.chyk.pass.api.jwapp

import ink.chyk.pass.*
import io.ktor.client.plugins.api.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

class JwappPluginConfig {
  // jwapp 模型只写了部分 key，这里忽略一下未知字段
  var json: Json = Json { ignoreUnknownKeys = true }
}

// 信封里的 datas，留给 body() 反序列化。
private val JwappData = AttributeKey<JsonElement>("JwappData")

// 教务系统未登录时返回 302 到统一认证登录页。
val JwappPlugin = createClientPlugin("JwappPlugin", ::JwappPluginConfig) {
  val json = pluginConfig.json

  onResponse { response ->
    val status = response.status.value
    if (status in 300..399 || status == 401 || status == 403) {
      throw SessionExpiredException("backend returned HTTP $status")
    }
    if (!response.status.isSuccess()) {
      throw RequestFailedException(response.call.request.url.toString())
    }

    // 解开 code/msg/datas 外层信封
    val envelope = json.decodeFromString<JwappResponse<JsonElement>>(response.bodyAsText())
    if (envelope.code != "0") {
      throw RequestFailedException(response.call.request.url.toString())
    }

    response.call.attributes.put(JwappData, envelope.datas ?: JsonNull)
  }

  transformResponseBody { response, _, requestedType ->
    val type = requestedType.kotlinType ?: return@transformResponseBody null
    json.decodeFromJsonElement(
      json.serializersModule.serializer(type),
      response.call.attributes[JwappData],
    )
  }
}
