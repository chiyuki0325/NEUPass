package ink.chyk.pass.api

import ink.chyk.pass.utils.AppLog
import io.ktor.client.plugins.api.*
import io.ktor.client.statement.*
import io.ktor.client.utils.EmptyContent
import io.ktor.http.content.NullBody
import kotlinx.coroutines.*

// 网络调试日志，应最先加载

internal val HttpLogPlugin = createClientPlugin("HttpLogPlugin") {
  fun enabled() = AppLog.DEBUG && AppLog.NETWORK_DEBUG

  onRequest { request, content ->
    if (!enabled()) return@onRequest
    val headers = request.headers.entries().associate { it.key to it.value }
    val suffix = buildString {
      if (headers.isNotEmpty()) append(" headers=${headers.redactedHeaderValues()}")
      if (content !is NullBody && content !is EmptyContent) {
        append(" body=${content::class.simpleName}")
      }
    }
    AppLog.d("HTTP", "--> ${request.method.value} ${request.url.buildString().redactedUrl()}$suffix")
  }

  onResponse { response ->
    if (!enabled()) return@onResponse
    val request = response.call.request
    val headers = response.headers.entries().associate { it.key to it.value }.redactedHeaderValues()
    val status = response.status.value
    val bodyDetail = if (AppLog.PRINT_RESPONSE_BODY) {
      val body = runCatching { response.bodyAsText() }.getOrDefault("<unreadable>")
      if (body.isNotBlank()) " body=${body.redactedBody().take(2000)}" else ""
    } else {
      ""
    }
    AppLog.d("HTTP", "<-- ${request.method.value} $status ${request.url.toString().redactedUrl()} headers=$headers$bodyDetail")
  }

  // 传输层失败（无响应）也打印一行，便于区分“没网”和“服务端报错”
  on(Send) { request ->
    try {
      proceed(request)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Throwable) {
      if (enabled()) {
        AppLog.d("HTTP", "<-- ${request.method.value} FAILED ${request.url.buildString().redactedUrl()} ${e::class.simpleName}: ${e.message}")
      }
      throw e
    }
  }
}
