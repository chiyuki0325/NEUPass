package ink.chyk.pass.features.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ink.chyk.pass.api.ecard.ECardBankRecord
import ink.chyk.pass.api.ecard.ECardPage
import ink.chyk.pass.api.ecard.ECardWallet
import ink.chyk.pass.repositories.ECardRepository
import ink.chyk.pass.utils.AppLog
import ink.chyk.pass.utils.DateTimeUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.last
import kotlinx.datetime.LocalDate

data class ECardRecordsState(
  val wallet: ECardWallet = ECardWallet.MAIN,
  val records: List<ECardBankRecord> = emptyList(),
  val page: Int = 0,
  val pageCount: Int = 1,
  val isLoading: Boolean = false,
  val failed: Boolean = false,
) {
  val hasMore: Boolean get() = page < pageCount
}

class ECardRecordsViewModel(
  private val getRecords: suspend (LocalDate, LocalDate, ECardWallet, Int) -> ECardPage<ECardBankRecord> =
    { startDate, endDate, wallet, page ->
      ECardRepository.getBankRecords(startDate, endDate, wallet, page).last().value
    },
) : ViewModel() {
  val startDate = LocalDate(2000, 1, 1)
  val endDate = DateTimeUtils.currentLocalDate()
  private val _state = MutableStateFlow(ECardRecordsState())
  val state = _state.asStateFlow()
  private var loadJob: Job? = null

  init {
    loadMore()
  }

  fun selectWallet(wallet: ECardWallet) {
    if (wallet == _state.value.wallet) return
    loadJob?.cancel()
    _state.value = ECardRecordsState(wallet = wallet)
    loadMore()
  }

  fun loadMore() {
    val current = _state.value
    if (current.isLoading || !current.hasMore) return
    _state.value = current.copy(isLoading = true, failed = false)
    loadJob = viewModelScope.launch {
      try {
        val result = getRecords(startDate, endDate, current.wallet, current.page + 1)
        ensureActive()
        _state.value = current.copy(
          records = if (result.page > current.page) current.records + result.records else current.records,
          page = result.page,
          pageCount = result.pageCount,
          failed = false,
        )
      } catch (exception: CancellationException) {
        throw exception
      } catch (exception: Exception) {
        ensureActive()
        AppLog.e("ECardRecordsViewModel", "loadMore: ${exception.message}")
        _state.value = current.copy(failed = true)
      }
    }
  }
}
