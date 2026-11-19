package ink.chyk.pass.features.courses.importer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ink.chyk.pass.services.courses.CourseImportMessage
import ink.chyk.pass.services.courses.CourseImportService
import ink.chyk.pass.utils.AppLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.collections.plus

class ImportCoursesViewModel : ViewModel() {
  private var _importing = MutableStateFlow(false)
  val importing: StateFlow<Boolean> = _importing
  private var _importCompleted = MutableStateFlow(false)
  val importCompleted: StateFlow<Boolean> = _importCompleted
  private val _messages = MutableStateFlow<List<CourseImportMessage>>(emptyList())
  val messages: StateFlow<List<CourseImportMessage>> = _messages
  private var _hasErrors = MutableStateFlow(false)
  val hasErrors: StateFlow<Boolean> = _hasErrors

  fun runImport(onImported: () -> Unit) {
    if (_importing.value) return
    _importing.value = true
    _importCompleted.value = false
    _hasErrors.value = false
    _messages.value = emptyList()
    viewModelScope.launch {
      try {
        withContext(Dispatchers.Default) {
          CourseImportService.importCourses { message ->
            _messages.update { it + message }
          }
        }
        onImported()
      } catch (error: CancellationException) {
        throw error
      } catch (error: Exception) {
        val message = "导入课表失败：${error.message ?: "未知错误"}"
        AppLog.e("CourseImport", message)
        _messages.update { it + CourseImportMessage(message) }
        _hasErrors.value = true
      } finally {
        _importing.value = false
      }
      _importCompleted.value = true
    }
  }
}