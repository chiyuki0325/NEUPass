@file:OptIn(kotlin.time.ExperimentalTime::class)

package ink.chyk.pass.api.ipgw

import io.ktor.client.plugins.api.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.random.Random
import kotlin.time.Clock

class IpgwPluginConfig {
  var json: Json = Json { ignoreUnknownKeys = true }
}

private val IpgwData = AttributeKey<String>("IpgwData")

val IpgwPlugin = createClientPlugin("IpgwPlugin", ::IpgwPluginConfig) {
  val json = pluginConfig.json

  onRequest { request, _ ->
    if (request.url.encodedPath.startsWith("/cgi-bin/")) {
      val timestamp = (Clock.System.now().toEpochMilliseconds() / 1000).toString()
      val random = Random.nextLong().toString().filter(Char::isDigit).padEnd(17, '0').take(17)
      request.url.parameters.append("callback", "jQuery1124${random}_$timestamp")
      request.url.parameters.append("_", timestamp)
    }
  }

  onResponse { response ->
    val raw = response.bodyAsText().trim()
    val callback = response.call.request.url.parameters["callback"]
    val body = if (callback == null) raw else {
      val jsonp = raw.removeSuffix(";")
      require(jsonp.startsWith("$callback(") && jsonp.endsWith(")"))
      jsonp.removePrefix("$callback(").removeSuffix(")")
    }
    response.call.attributes.put(IpgwData, body)
  }

  transformResponseBody { response, _, requestedType ->
    val type = requestedType.kotlinType ?: return@transformResponseBody null
    val body = response.call.attributes[IpgwData]
    if (requestedType.type == IpgwInfo::class) {
      val data = json.parseToJsonElement(body).jsonObject
      if ("billing_name" in data) {
        IpgwInfo(true, onlineInfo = json.decodeFromJsonElement<IpgwOnlineInfo>(data))
      } else {
        IpgwInfo(false, offlineInfo = json.decodeFromJsonElement<IpgwOfflineInfo>(data))
      }
    } else {
      json.decodeFromString(json.serializersModule.serializer(type), body)
    }
  }
}
