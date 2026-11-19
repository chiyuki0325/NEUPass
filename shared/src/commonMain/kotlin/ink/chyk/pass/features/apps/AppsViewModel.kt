package ink.chyk.pass.features.apps

import androidx.lifecycle.*
import com.ctrip.flight.mmkv.defaultMMKV
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import ink.chyk.pass.api.CampusApp
import ink.chyk.pass.repositories.AppAuthRepository
import kotlinx.coroutines.*

@OptIn(ExperimentalSerializationApi::class)
class AppsViewModel : ViewModel() {

  private val mmkv by lazy { defaultMMKV() }

  // 卡片顺序
  private var _appCardsOrder = MutableStateFlow(getAppCardsOrder())
  val appCardsOrder: StateFlow<List<Pair<String, Boolean>>> = _appCardsOrder

  val campusRun = CampusRunManager(viewModelScope)
  val ipgw = IpGatewayManager(viewModelScope)
  val jwapp = JwappManager(viewModelScope)
  val ecard = ECardManager(viewModelScope)


  fun getAppCardsOrder(): List<Pair<String, Boolean>> {
    val default = listOf(
      "campus_run" to true,
      "jwapp" to true,
      "ecard" to true,
      "ip_gateway" to true,
    )

    if (!mmkv.containsKey("app_cards_order")) return default
    val savedData = mmkv.getString("app_cards_order")

    return try {
      val parsedList = savedData.split(",").mapNotNull { entry ->
        val parts = entry.split(":")
        if (parts.size == 2) parts[0] to parts[1].toBoolean() else null
      }

      parsedList + default.filter { card -> parsedList.none { it.first == card.first } }
    } catch (e: Exception) {
      default
    }
  }

  fun setAppCardsOrder(order: List<Pair<String, Boolean>>) {
    _appCardsOrder.value = order
    mmkv["app_cards_order"] = order.joinToString(",") { "${it.first}:${it.second}" }
  }

  // 启动网页应用
  // 这个函数懒得挪走了，就放在 ViewModel 里吧
  fun openWebApp(app: CampusApp) {
    viewModelScope.launch {
      openExternalWebApp(AppAuthRepository.loginWebApp(app))
    }
  }
}
