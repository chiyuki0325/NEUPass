package ink.chyk.pass.api.webvpn

import io.ktor.client.plugins.api.*
import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val webVpnRequestMutex = Mutex()

// 把发往校内站点的请求改写成 WebVPN 代理地址。
// 调用方仍然按原始地址使用 Ktor；WebVPN 域名本身（登录回调等）不重复改写。
val WebVpnPlugin = createClientPlugin("WebVpnPlugin") {
  onRequest { request, _ ->
    val url = request.url.build()
    if (url.host == WebVpnCrypto.HOST) return@onRequest
    // takeFrom(String) 会把新 query 追加到现有参数后面，导致 termCode/ticket 重复；
    // takeFrom(Url) 是整体替换。
    request.url.takeFrom(Url(WebVpnCrypto.encode(url.toString())))
  }

  // WebVPN 的多个业务客户端共用同一会话，所有代理请求必须串行发送。
  on(Send) { request ->
    webVpnRequestMutex.withLock {
      proceed(request)
    }
  }
}
