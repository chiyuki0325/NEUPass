package ink.chyk.pass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberNetworkAvailability(): NetworkAvailability {
  val context = LocalContext.current
  val monitor = remember(context) { NetworkMonitor(context) }
  val availability by monitor.availability.collectAsState()

  DisposableEffect(monitor) {
    monitor.startMonitoring()
    onDispose { monitor.stopMonitoring() }
  }

  return availability
}
