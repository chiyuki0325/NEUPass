package ink.chyk.pass.web

import android.util.*
import androidx.compose.runtime.*

@Composable
actual fun rememberWebViewState(): WebViewState = remember { WebViewState() }
