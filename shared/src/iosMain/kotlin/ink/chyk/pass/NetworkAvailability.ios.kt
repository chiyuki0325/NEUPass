@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package ink.chyk.pass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.Network.*
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberNetworkAvailability(): NetworkAvailability {
  val monitor = remember { nw_path_monitor_create() }
  var availability by remember { mutableStateOf(NetworkAvailability.Unknown) }

  DisposableEffect(monitor) {
    nw_path_monitor_set_update_handler(monitor) { path ->
      availability = if (nw_path_get_status(path) == nw_path_status_satisfied) {
        NetworkAvailability.Available
      } else {
        NetworkAvailability.Unavailable
      }
    }
    nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
    nw_path_monitor_start(monitor)
    onDispose {
      nw_path_monitor_cancel(monitor)
      nw_path_monitor_set_update_handler(monitor, null)
    }
  }

  return availability
}
