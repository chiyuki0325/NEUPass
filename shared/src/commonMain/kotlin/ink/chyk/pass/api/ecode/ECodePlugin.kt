package ink.chyk.pass.api.ecode

import ink.chyk.pass.*
import io.ktor.client.plugins.api.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

class ECodeBizPluginConfig {
  var json: Json = Json { ignoreUnknownKeys = true }
}

private val ECodeResponseData = AttributeKey<JsonElement>("ECodeResponseData")

// 一码通使用 XSRF-TOKEN Cookie + X-XSRF-TOKEN 头的双提交校验。
val ECodeSessionPlugin = createClientPlugin("ECodeSessionPlugin") {
  onRequest { request, _ ->
    ECodeCookiesStorage.xsrfToken()?.let { request.headers["X-XSRF-TOKEN"] = it }
  }
}

// 业务响应：3xx/401/403 视为会话失效，其余非 2xx 视为请求失败；
val ECodeBizPlugin = createClientPlugin("ECodeBizPlugin", ::ECodeBizPluginConfig) {
  val json = pluginConfig.json

  onResponse { response ->
    val status = response.status.value
    if (status in 300..399 || status == 401 || status == 403) {
      throw SessionExpiredException("backend returned HTTP $status")
    }
    if (!response.status.isSuccess()) {
      throw RequestFailedException(response.call.request.url.toString())
    }

    // 解包一码通响应信封
    val listed = json.decodeFromString<ECodeListedResponse>(response.bodyAsText())
    val attributes = listed.data.firstOrNull()?.attributes
      ?: throw RequestFailedException(response.call.request.url.toString())
    response.call.attributes.put(ECodeResponseData, attributes)
  }

  transformResponseBody { response, _, requestedType ->
    val type = requestedType.kotlinType ?: return@transformResponseBody null
    // 反序列化信封内业务响应
    json.decodeFromJsonElement(
      json.serializersModule.serializer(type),
      response.call.attributes[ECodeResponseData],
    )
  }
}
