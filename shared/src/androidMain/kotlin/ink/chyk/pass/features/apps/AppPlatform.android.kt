package ink.chyk.pass.features.apps

import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import ink.chyk.pass.NEUPassApplication
import ink.chyk.pass.NetworkMonitor
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import ink.chyk.pass.R

actual fun openExternalWebApp(url: String) {
  val context = NEUPassApplication.context
  val customTab = CustomTabsIntent.Builder()
    .setShowTitle(false)
    .setUrlBarHidingEnabled(true)
    .setStartAnimations(context, R.anim.slide_in_right, R.anim.slide_out_left)
    .setExitAnimations(context, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    .build()
  customTab.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  customTab.launchUrl(context, Uri.parse(url))
}

@Composable
actual fun PlatformNetworkReconnectEffect(onConnected: () -> Unit) {
  val context = LocalContext.current
  val monitor = remember(context) { NetworkMonitor(context) }
  LaunchedEffect(Unit) { monitor.startMonitoring() }
  val connected by monitor.isConnected.collectAsState()
  LaunchedEffect(connected) { if (connected) onConnected() }
}
