package ink.chyk.pass.features.apps

import ink.chyk.pass.LoadingState
import ink.chyk.pass.api.jwapp.GpaSummary
import ink.chyk.pass.models.StoredGrade
import ink.chyk.pass.repositories.CacheResult
import ink.chyk.pass.repositories.JwappRepository
import ink.chyk.pass.utils.AppLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class JwappManager(private val scope: CoroutineScope) {
  private val _loadingState = MutableStateFlow(LoadingState.LOADING)
  val loadingState = _loadingState.asStateFlow()

  private val _gpaSummary = MutableStateFlow<CacheResult<GpaSummary>?>(null)
  val gpaSummary = _gpaSummary.asStateFlow()

  private val _grades = MutableStateFlow<List<StoredGrade>>(emptyList())
  val grades = _grades.asStateFlow()

  fun initJwapp() {
    scope.launch { refreshJwapp() }
  }

  suspend fun refreshJwapp() = withContext(Dispatchers.IO) {
    _loadingState.value = LoadingState.LOADING
    launch {
      try {
        JwappRepository.getGrades().collect { result ->
          _grades.value = result.value
        }
      } catch (exception: CancellationException) {
        throw exception
      } catch (exception: Exception) {
        AppLog.e("JwappManager", "refreshGrades: ${exception.message}")
      }
    }
    try {
      JwappRepository.getGpaSummary().collect { result ->
        _gpaSummary.value = result
        _loadingState.value = when (result) {
          is CacheResult.Cached -> LoadingState.PARTIAL_SUCCESS
          is CacheResult.Fresh -> LoadingState.SUCCESS
        }
      }
    } catch (exception: CancellationException) {
      throw exception
    } catch (exception: Exception) {
      AppLog.e("JwappManager", "refreshJwapp: ${exception.message}")
      _loadingState.value = LoadingState.FAILED
    }
  }
}
