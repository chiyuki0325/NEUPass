package ink.chyk.pass.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

expect class WebViewState() {
    fun evaluateJavascript(script: String, callback: (String?) -> Unit = {})
}

@Composable
expect fun rememberWebViewState(): WebViewState

@Composable
expect fun CustomWebView(
    url: String,
    state: WebViewState = rememberWebViewState(),
    modifier: Modifier = Modifier,
    isDarkMode: Boolean? = null,
    listenBack: Boolean = true,
    finish: () -> Unit = {},
    cookies: List<String> = emptyList(),
    extraScripts: String? = null,
    backHandler: ((defaultBack: () -> Unit) -> Unit)? = null,
)
