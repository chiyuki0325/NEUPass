package ink.chyk.pass.features.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ink.chyk.pass.repositories.JwappRepository
import ink.chyk.pass.repositories.JwappWebSession
import ink.chyk.pass.utils.AppLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface JwappPageState {
  data object Loading : JwappPageState
  data class Ready(val session: JwappWebSession) : JwappPageState
  data object Failed : JwappPageState
}

class JwappViewModel : ViewModel() {
  private val _state = MutableStateFlow<JwappPageState>(JwappPageState.Loading)
  val state = _state.asStateFlow()

  init {
    load()
  }

  fun load() {
    _state.value = JwappPageState.Loading
    viewModelScope.launch {
      try {
        _state.value = JwappPageState.Ready(JwappRepository.getWebSession())
      } catch (exception: CancellationException) {
        throw exception
      } catch (exception: Exception) {
        AppLog.e("JwappViewModel", "load: ${exception.message}")
        _state.value = JwappPageState.Failed
      }
    }
  }
}
