package ink.chyk.pass.features.apps

import ink.chyk.pass.*
import ink.chyk.pass.api.ipgw.*
import ink.chyk.pass.repositories.IpgwRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class IpGatewayManager(
  private val scope: CoroutineScope
) {
  // 加载状态：网关
  private var _ipGatewayState = MutableStateFlow(LoadingState.LOADING)
  val ipGatewayState: StateFlow<LoadingState> = _ipGatewayState

  // 网关信息
  private var _gatewayInfo = MutableStateFlow<IpgwInfo?>(null)
  val gatewayInfo: StateFlow<IpgwInfo?> = _gatewayInfo

  private suspend fun updateGateway(action: suspend () -> Unit = {}) = withContext(Dispatchers.IO) {
    _ipGatewayState.value = LoadingState.LOADING
    try {
      action()
      _gatewayInfo.value = IpgwRepository.getOnlineInfo()
      _ipGatewayState.value = LoadingState.SUCCESS
    } catch (_: NetworkException) {
      _ipGatewayState.value = LoadingState.FAILED
    } catch (e: IpgwException) {
      e.printStackTrace()
      _ipGatewayState.value = LoadingState.PARTIAL_SUCCESS
    } catch (e: TicketFailedException) {
      e.printStackTrace()
      _ipGatewayState.value = LoadingState.PARTIAL_SUCCESS
    }
  }

  fun initIpGateway() {
    if (_ipGatewayState.value == LoadingState.SUCCESS) return
    scope.launch {
      updateGateway()
    }
  }

  suspend fun refreshIpGateway() {
    updateGateway()
  }

  fun loginIpGateway() {
    scope.launch {
      updateGateway { IpgwRepository.login() }
    }
  }


  fun logoutIpGateway() {
    scope.launch {
      updateGateway { IpgwRepository.logout() }
    }
  }


}
