package ink.chyk.pass.web

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import io.github.kdroidfilter.webview.cookie.Cookie
import io.github.kdroidfilter.webview.web.LoadingState
import io.github.kdroidfilter.webview.web.WebContent
import io.github.kdroidfilter.webview.web.WebView
import io.github.kdroidfilter.webview.web.rememberWebViewNavigator
import io.ktor.http.Url
import io.ktor.http.encodeCookieValue
import io.ktor.http.parseServerSetCookieHeader
import kotlinx.coroutines.delay
import io.github.kdroidfilter.webview.web.WebViewState as NativeWebViewState

actual class WebViewState actual constructor() {
    internal val nativeState = NativeWebViewState(WebContent.Url("about:blank"))

    actual fun evaluateJavascript(script: String, callback: (String?) -> Unit) {
        val webView = nativeState.webView
        if (webView == null) {
            callback(null)
        } else {
            webView.evaluateJavaScript(script) { result -> callback(result) }
        }
    }
}

@Composable actual fun rememberWebViewState(): WebViewState = remember { WebViewState() }

@Composable
@OptIn(ExperimentalComposeUiApi::class)
actual fun CustomWebView(
    url: String,
    state: WebViewState,
    modifier: Modifier,
    isDarkMode: Boolean?,
    listenBack: Boolean,
    finish: () -> Unit,
    cookies: List<String>,
    extraScripts: String?,
    backHandler: ((defaultBack: () -> Unit) -> Unit)?,
) {
    val nativeState = state.nativeState
    val navigator = rememberWebViewNavigator()
    val darkMode = isDarkMode ?: isSystemInDarkTheme()

    WebView(
        state = nativeState,
        navigator = navigator,
        modifier = modifier.fillMaxSize(),
    )

    LaunchedEffect(nativeState, url, cookies) {
        while (nativeState.webView == null) {
            delay(10)
        }
        val target = Url(url)
        cookies.forEach { header ->
            nativeState.cookieManager.setCookie(url, parseCookie(header, target))
        }
        nativeState.content = WebContent.Url(url)
    }

    LaunchedEffect(nativeState.loadingState, nativeState.lastLoadedUrl, darkMode, extraScripts) {
        if (nativeState.loadingState is LoadingState.Finished &&
            nativeState.lastLoadedUrl != null &&
            nativeState.lastLoadedUrl != "about:blank"
        ) {
            state.evaluateJavascript(
                """
                (() => {
                    const id = 'neupass-dark-mode';
                    let style = document.getElementById(id);
                    if ($darkMode) {
                        if (!style) {
                            style = document.createElement('style');
                            style.id = id;
                            document.head.appendChild(style);
                        }
                        style.textContent = 'html { filter: invert(1) hue-rotate(180deg) saturate(1); --UI-BG-0: #fff; } .run__nav-icon { filter: invert(0.1) }';
                    } else {
                        style?.remove();
                    }
                })()
                """.trimIndent(),
            )
            extraScripts?.let { state.evaluateJavascript(it) }
        }
    }

    BackHandler {
        val defaultBack: () -> Unit = {
            if (listenBack && navigator.canGoBack) {
                navigator.navigateBack()
            } else {
                finish()
            }
        }
        if (backHandler != null) backHandler(defaultBack) else defaultBack()
    }
}

private fun parseCookie(header: String, url: Url): Cookie {
    val cookie = parseServerSetCookieHeader(header)
    val sameSite = cookie.extensions.entries.firstOrNull {
        it.key.equals("SameSite", ignoreCase = true)
    }?.value
    return Cookie(
        name = cookie.name,
        value = encodeCookieValue(cookie.value, cookie.encoding),
        domain = cookie.domain ?: url.host,
        path = cookie.path ?: url.encodedPath.substringBeforeLast('/').ifEmpty { "/" },
        expiresDate = cookie.expires?.timestamp,
        isSessionOnly = cookie.expires == null && cookie.maxAge == null,
        maxAge = cookie.maxAge?.toLong(),
        isSecure = cookie.secure,
        isHttpOnly = cookie.httpOnly,
        sameSite = Cookie.HTTPCookieSameSitePolicy.entries.firstOrNull {
            it.name.equals(sameSite, ignoreCase = true)
        },
    )
}
