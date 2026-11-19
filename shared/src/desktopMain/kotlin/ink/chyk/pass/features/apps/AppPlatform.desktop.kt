package ink.chyk.pass.features.apps

import androidx.compose.runtime.Composable
import ink.chyk.pass.utils.AppLog

actual fun openExternalWebApp(url: String) {
  val os = System.getProperty("os.name").lowercase()
  val command = when {
    os.contains("win") -> listOf("rundll32", "url.dll,FileProtocolHandler", url)
    os.contains("mac") || os.contains("darwin") -> listOf("open", url)
    else -> listOf("xdg-open", url)
  }
  runCatching { ProcessBuilder(command).start() }
    .onFailure { AppLog.e("Apps", "无法打开网页: ${it.message}") }
}
@Composable actual fun PlatformNetworkReconnectEffect(onConnected: () -> Unit) = Unit
