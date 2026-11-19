package ink.chyk.pass

// generated with deepseek

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NetworkMonitor(private val context: Context) {

  private val _isConnected = MutableStateFlow(false)
  val isConnected: StateFlow<Boolean> = _isConnected
  private val _availability = MutableStateFlow(NetworkAvailability.Unknown)
  val availability: StateFlow<NetworkAvailability> = _availability

  private var isMonitoring = false
  private val connectivityManager by lazy {
    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
  }

  private val networkCallback = object : ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
      updateAvailability(true)
    }

    override fun onLost(network: Network) {
      updateAvailability(false)
    }
  }

  fun startMonitoring() {
    if (isMonitoring) return
    isMonitoring = true
    val networkRequest = NetworkRequest.Builder()
      .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      .build()
    connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

    // 初始状态检查
    val currentNetwork = connectivityManager.activeNetwork
    val caps = connectivityManager.getNetworkCapabilities(currentNetwork)
    updateAvailability(caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true)
  }

  fun stopMonitoring() {
    if (!isMonitoring) return
    connectivityManager.unregisterNetworkCallback(networkCallback)
    isMonitoring = false
  }

  private fun updateAvailability(isConnected: Boolean) {
    _isConnected.value = isConnected
    _availability.value = if (isConnected) NetworkAvailability.Available else NetworkAvailability.Unavailable
  }
}
