package ink.chyk.pass.features.apps

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import ink.chyk.pass.NetworkAvailability
import ink.chyk.pass.rememberNetworkAvailability
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openExternalWebApp(url: String) {
  NSURL.URLWithString(url)?.let { UIApplication.sharedApplication.openURL(it) }
}

@Composable
actual fun PlatformNetworkReconnectEffect(onConnected: () -> Unit) {
  val availability = rememberNetworkAvailability()
  val currentOnConnected by rememberUpdatedState(onConnected)
  LaunchedEffect(availability) {
    if (availability == NetworkAvailability.Available) currentOnConnected()
  }
}
