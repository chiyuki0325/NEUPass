package ink.chyk.pass.api.pass

import ink.chyk.pass.*
import io.ktor.client.plugins.api.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*

// 传入的 CASTGC
internal val AppLoginTicket = AttributeKey<String>("AppLoginTicket")

// 返回的 ST
private val AppTicket = AttributeKey<String>("AppTicket")

val AppAuthPlugin = createClientPlugin("AppAuthPlugin") {
  onRequest { request, _ ->
    request.cookie("CASTGC", request.attributes[AppLoginTicket])
  }

  onResponse { response ->
    if (response.status.value !in 200..399) {
      // 非法状态
      throw RequestFailedException(response.call.request.url.toString())
    }

    // 有 302 重定向代表成功，取 ?ticket=ST-xxx
    val location = response.headers[HttpHeaders.Location]
    val appTicket = if (response.status == HttpStatusCode.Found && location != null) {
      URLBuilder(response.call.request.url).takeFrom(location).build().parameters["ticket"]
    } else null
    if (appTicket.isNullOrEmpty()) throw TicketFailedException("CASTGC 过期")
    response.call.attributes.put(AppTicket, appTicket)
  }

  // 返回的 ST 由调用方在 body() 中获取
  transformResponseBody { response, _, _ ->
    response.call.attributes[AppTicket]
  }
}
