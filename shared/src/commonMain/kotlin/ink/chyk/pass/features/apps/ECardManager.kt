package ink.chyk.pass.features.apps

import ink.chyk.pass.LoadingState
import ink.chyk.pass.api.ecard.ECardBankRecord
import ink.chyk.pass.api.ecard.ECardPage
import ink.chyk.pass.repositories.CacheResult
import ink.chyk.pass.repositories.ECardRepository
import ink.chyk.pass.utils.AppLog
import ink.chyk.pass.utils.DateTimeUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate

class ECardManager(
  private val scope: CoroutineScope,
  private val getRecords: (LocalDate, LocalDate) -> Flow<CacheResult<ECardPage<ECardBankRecord>>> =
    { startDate, endDate -> ECardRepository.getBankRecords(startDate, endDate) },
) {
  private val _loadingState = MutableStateFlow(LoadingState.LOADING)
  val loadingState = _loadingState.asStateFlow()
  private val _records = MutableStateFlow<List<ECardBankRecord>>(emptyList())
  val records = _records.asStateFlow()
  private var isLoading = false

  fun init() {
    scope.launch { refresh() }
  }

  suspend fun refresh() {
    if (isLoading) return
    isLoading = true
    _loadingState.value = LoadingState.LOADING
    try {
      getRecords(
        LocalDate(2000, 1, 1),
        DateTimeUtils.currentLocalDate(),
      ).collect { result ->
        _records.value = result.value.records.take(3)
        _loadingState.value = when (result) {
          is CacheResult.Cached -> LoadingState.PARTIAL_SUCCESS
          is CacheResult.Fresh -> LoadingState.SUCCESS
        }
      }
    } catch (exception: CancellationException) {
      throw exception
    } catch (exception: Exception) {
      AppLog.e("ECardManager", "refresh: ${exception.message}")
      _loadingState.value = LoadingState.FAILED
    } finally {
      isLoading = false
    }
  }
}
