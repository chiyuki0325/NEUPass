@file:OptIn(kotlin.time.ExperimentalTime::class)

package ink.chyk.pass.features.ecode

import androidx.lifecycle.*
import ink.chyk.pass.*
import ink.chyk.pass.api.ecode.ECodeUserInfo
import ink.chyk.pass.repositories.*
import ink.chyk.pass.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class ECodeViewModel : ViewModel() {
  // 一码通 ViewModel

  private val _loadingState = MutableStateFlow(LoadingState.LOADING)
  val loadingState = _loadingState.asStateFlow()

  private val _isRefreshing = MutableStateFlow(false)
  val isRefreshing = _isRefreshing.asStateFlow()

  // 二维码数据
  private val _code = MutableStateFlow<String?>(null)
  val code = _code.asStateFlow()

  // 用户描述
  private val _userInfo = MutableStateFlow<ECodeUserInfo?>(null)
  val userInfo = _userInfo.asStateFlow()

  // 时间描述
  private val _timeDescription = MutableStateFlow<String?>(null)
  val timeDescription = _timeDescription.asStateFlow()

  // 二维码过期时间
  private var codeExpiredAt = 0L


  /**
   * 获取二维码
   */
  private suspend fun realRefreshQRCode() = withContext(Dispatchers.IO) {
    try {
      val qrCode = ECodeRepository.getQrCode()
      _code.value = qrCode.qrCode
      val instant = Instant.fromEpochMilliseconds(qrCode.createTime)
      val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
      _timeDescription.value = localDateTime.format(DateTimeUtils.dateTimeSecondsFormat)
      codeExpiredAt = qrCode.qrInvalidTime
      _loadingState.value = LoadingState.SUCCESS
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      // 网络问题
      println("ECodeViewModel: failed to refresh QR code: ${e.message}")
      _code.value = null
      _loadingState.value = LoadingState.FAILED
    }
  }

  // 包装成非 suspend 函数给 UI 层调用
  fun refreshQRCode() {
    if (_isRefreshing.value) return
    _isRefreshing.value = true
    viewModelScope.launch {
      try {
        realRefreshQRCode()
      } finally {
        _isRefreshing.value = false
      }
    }
  }

  fun startAutoRefreshQRCodeLoop() = flow {
    // 首先刷新一次用户信息
    try {
      ECodeRepository.getUserInfoFlow().collect { userInfo ->
        _userInfo.value = userInfo
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      // 网络问题，不影响二维码刷新
      println("PassCodeViewModel: failed to get user info: ${e.message}")
    }

    // 启动二维码刷新循环
    while (currentCoroutineContext().isActive) {
      val currentTime = kotlin.time.Clock.System.now().toEpochMilliseconds()
      if (currentTime >= codeExpiredAt) {
        realRefreshQRCode()
      }
      emit(Unit)
      delay(1000L.milliseconds)
    }
  }.flowOn(Dispatchers.IO)

  fun shouldRaiseBrightness(): Boolean = SettingsRepository.raiseECodeBrightness

  suspend fun getWebSession(): ECodeWebSession = withContext(Dispatchers.IO) {
    ECodeRepository.getWebSession()
  }
}
