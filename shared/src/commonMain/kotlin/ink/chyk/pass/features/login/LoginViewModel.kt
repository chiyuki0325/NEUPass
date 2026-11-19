package ink.chyk.pass.features.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ctrip.flight.mmkv.defaultMMKV
import ink.chyk.pass.*
import ink.chyk.pass.api.portal.login.LoginApi.LoginState
import ink.chyk.pass.repositories.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.*

enum class LoginErrorKind { LOGIN_FAILED, REQUEST_FAILED, OTHER }

sealed interface LoginUiState {
  data object Idle : LoginUiState
  // 有请求正在跑，ui 按钮不可点
  data object Running : LoginUiState
  // 登录完成，可以进入主界面
  data object Completed : LoginUiState
  // ui 弹出失败
  data class Failed(val kind: LoginErrorKind, val message: String? = null) : LoginUiState
}

class LoginViewModel : ViewModel() {
  private val mmkv by lazy { defaultMMKV() }

  val savedStudentId: String get() = mmkv.getString("student_id")
  val savedPassword: String get() = mmkv.getString("password")

  // 登录步骤直接复用 repo 层状态机
  val step = LoginRepository.loginState
  private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
  val uiState = _uiState.asStateFlow()

  init {
    LoginRepository.cancelLogin()
  }

  fun login(studentId: String, password: String) = submit {
    LoginRepository.login(studentId, password)
  }

  fun submitSmsCode(smsCode: String) = submit {
    LoginRepository.submitSmsCode(smsCode)
  }

  fun dismissError() {
    _uiState.value = LoginUiState.Idle
  }

  // 调用 repo 方法，根据 repo 层状态更新 ui 状态
  private fun submit(action: suspend () -> LoginState) {
    if (_uiState.value == LoginUiState.Running) return
    viewModelScope.launch {
      _uiState.value = LoginUiState.Running
      _uiState.value = try {
        val state = action()
        if (state is LoginState.Authenticated) {
          LoginUiState.Completed
        } else {
          // 需要验证码，页面根据 step 显示验证码输入框
          LoginUiState.Idle
        }
      } catch (e: LoginFailedException) {
        LoginUiState.Failed(LoginErrorKind.LOGIN_FAILED, e.message)
      } catch (_: NetworkException) {
        LoginUiState.Failed(LoginErrorKind.REQUEST_FAILED)
      } catch (e: CancellationException) {
        _uiState.value = LoginUiState.Idle
        throw e
      } catch (e: Exception) {
        LoginUiState.Failed(LoginErrorKind.OTHER, e.message)
      }
    }
  }

}
