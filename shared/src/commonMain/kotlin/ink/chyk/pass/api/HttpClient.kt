package ink.chyk.pass.api

import ink.chyk.pass.*
import io.ktor.client.*
import io.ktor.client.plugins.*
import kotlinx.coroutines.*

// 网络层采用 createApiClient 创建各业务的 Ktor HttpClient
// 内置特性：非 2xx 报错、异常归类、协议固定请求头
// 其余特性：各业务在 block 中装配自己的协议插件
internal fun createApiClient(block: HttpClientConfig<*>.() -> Unit = {}): HttpClient = HttpClient(sharedApiEngine) {
  expectSuccess = true
  HttpResponseValidator {
    // 协议插件抛出的业务异常原样上抛；HTTP 错误与解码失败（含密文校验）
    // 归为请求失败，其余传输层异常归为网络异常。
    handleResponseExceptionWithRequest { cause, request ->
      val url = request.url.toString()
      when (cause) {
        is NetworkException, is SessionExpiredException, is LoginFailedException,
        is TicketFailedException, is EHallPortalException, is CancellationException -> Unit  // 原样上抛
        is ResponseException, is IllegalArgumentException -> throw RequestFailedException(url)
        is Exception -> throw TransportException("请求失败: $url", cause)
      }
    }
  }
  defaultRequest {
    headers.append("User-Agent", appUserAgent())
    headers.append("Accept-Language", "zh-Hans-CN;q=1, en-CN;q=0.9")
    headers.append("X-Requested-With", "XMLHttpRequest")
    headers.append("From-Eai", "1")
  }
  // 如果为debug build则打印调试日志
  install(HttpLogPlugin)
  block()
}
