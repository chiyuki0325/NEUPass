package ink.chyk.pass.api.ecard

import com.ctrip.flight.mmkv.defaultMMKV

internal data class ECardFormState(
  val viewState: String,
  val eventValidation: String,
  val descending: Boolean,
)

internal enum class ECardGridPage(val cacheKey: String) {
  CONSUME("consume"),
  BANK("bank"),
}

// 倒序状态的 VIEWSTATE/EVENTVALIDATION 可以跨 ASP.NET 会话复用，
// 但服务端 machineKey 轮换后会因 MAC 校验失败而失效。
// 这里按页面持久化最近一次倒序状态，查询时优先复用；
// 遇到 MAC 失败由 ECardApi 清掉并重新 GET。
internal object ECardViewStateStorage {
  private val mmkv by lazy { defaultMMKV() }

  fun load(page: ECardGridPage): ECardFormState? {
    val prefix = prefix(page)
    if (!mmkv.containsKey(prefix + "viewstate") || !mmkv.containsKey(prefix + "eventvalidation")) return null
    return ECardFormState(
      viewState = mmkv.getString(prefix + "viewstate"),
      eventValidation = mmkv.getString(prefix + "eventvalidation"),
      descending = true,
    )
  }

  fun save(page: ECardGridPage, state: ECardFormState) {
    val prefix = prefix(page)
    mmkv[prefix + "viewstate"] = state.viewState
    mmkv[prefix + "eventvalidation"] = state.eventValidation
  }

  fun clear(page: ECardGridPage) {
    val prefix = prefix(page)
    mmkv.removeValueForKey(prefix + "viewstate")
    mmkv.removeValueForKey(prefix + "eventvalidation")
  }

  fun clearAll() {
    ECardGridPage.entries.forEach(::clear)
  }

  private fun prefix(page: ECardGridPage) = "ecard_${page.cacheKey}_"
}
