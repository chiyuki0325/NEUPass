package ink.chyk.pass.web

import androidx.compose.runtime.*
import com.tencent.smtt.sdk.WebView

@Stable
actual class WebViewState actual constructor() {
  internal var webView by mutableStateOf<WebView?>(null)
  actual fun evaluateJavascript(script: String, callback: (String?) -> Unit) {
    webView?.evaluateJavascript(script) { callback(it) }
  }
}
