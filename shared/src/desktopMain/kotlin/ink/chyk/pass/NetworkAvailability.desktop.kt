package ink.chyk.pass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import java.net.NetworkInterface
import kotlinx.coroutines.*

@Composable
actual fun rememberNetworkAvailability(): NetworkAvailability {
  val availability by produceState(initialValue = NetworkAvailability.Unknown) {
    while (true) {
      value = withContext(Dispatchers.IO) {
        if (hasUsableNetworkInterface()) NetworkAvailability.Available else NetworkAvailability.Unavailable
      }
      delay(2_000)
    }
  }
  return availability
}

private fun hasUsableNetworkInterface(): Boolean = runCatching {
  val interfaces = NetworkInterface.getNetworkInterfaces()
  while (interfaces.hasMoreElements()) {
    val networkInterface = interfaces.nextElement()
    if (networkInterface.isUp && !networkInterface.isLoopback && !networkInterface.isVirtual) {
      return@runCatching true
    }
  }
  false
}.getOrDefault(false)
