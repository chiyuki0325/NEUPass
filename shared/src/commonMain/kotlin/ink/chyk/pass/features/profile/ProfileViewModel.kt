package ink.chyk.pass.features.profile

import androidx.lifecycle.*
import ink.chyk.pass.*
import ink.chyk.pass.api.CampusApp
import ink.chyk.pass.api.portal.*
import ink.chyk.pass.features.apps.openExternalWebApp
import ink.chyk.pass.repositories.*
import ink.chyk.pass.utils.AppLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.milliseconds

class ProfileViewModel : ViewModel() {
  private val _userInfo = MutableStateFlow<UserInfo?>(null)
  val userInfo: StateFlow<UserInfo?> = _userInfo
  private val _cardBalance = MutableStateFlow<CacheResult<DataItem?>?>(null)
  val cardBalance = _cardBalance.asStateFlow()
  private val _netBalance = MutableStateFlow<CacheResult<DataItem?>?>(null)
  val netBalance = _netBalance.asStateFlow()

  private val _loadingState = MutableStateFlow(LoadingState.LOADING)
  val loadingState = _loadingState.asStateFlow()

  private val _newAvatarContent = MutableStateFlow<ByteArray?>(null)
  private val _newAvatarMimeType = MutableStateFlow<String?>(null)
  private val _newAvatarFileName = MutableStateFlow<String?>(null)

  fun refreshUserInfo(callback: (() -> Unit)? = null) {
    // 第一次进入个人中心 / 下拉刷新时调用
    viewModelScope.launch {
      _loadingState.value = LoadingState.LOADING
      // Only downgrade transport failures when the shared error state is visible.
      val portalRequestFailureVisible = MutableStateFlow(false)
      val confirmPortalRequestFailureVisible = {
        portalRequestFailureVisible.value = true
        true
      }
      try {
        withContext(Dispatchers.IO) {
          merge(
            PortalRepository.getUserInfo(confirmPortalRequestFailureVisible).onEach { _userInfo.value = it.value },
            PortalRepository.getCardBalance(confirmPortalRequestFailureVisible).onEach { _cardBalance.value = it },
            PortalRepository.getNetBalance(confirmPortalRequestFailureVisible).onEach { _netBalance.value = it }
          ).collect()
        }
        _loadingState.value = if (portalRequestFailureVisible.value) {
          LoadingState.FAILED
        } else {
          LoadingState.SUCCESS
        }
        callback?.invoke()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        AppLog.e("ProfileViewModel", "Error refreshing user info: ${e.message}")
        _loadingState.value = LoadingState.FAILED
        callback?.invoke()
      }
    }
  }

  fun logout() {
    // 极度危险的登出方法
    LoginRepository.logout()
  }


  fun recharge() {
    // 打开缴费服务大厅

    viewModelScope.launch {
      // 首先等待加载完成
      while (loadingState.value != LoadingState.SUCCESS) {
        delay(100.milliseconds)
      }
      openExternalWebApp(AppAuthRepository.loginWebApp(CampusApp.PAY_HALL))
    }
  }

  fun setAvatarToUpload(
    byteArray: ByteArray,
    mimeType: String,
    fileName: String
  ) {
    // 设置准备上传的头像
    _newAvatarContent.value = byteArray
    _newAvatarMimeType.value = mimeType
    _newAvatarFileName.value = fileName
  }

  // 头像通过门户 API 拉取，由 Ktor 自动携带会话 Cookie。
  fun loadAvatar(path: String): Flow<CacheResult<ByteArray>> = PortalRepository.getAvatar(path)

  fun uploadAvatar(
    onUploadComplete: () -> Unit
  ) {
    val byteArray = _newAvatarContent.value
      ?: throw Exception("No avatar content to upload")
    val mimeType = _newAvatarMimeType.value
      ?: throw Exception("No avatar mime type to upload")
    val fileName = _newAvatarFileName.value
      ?: throw Exception("No avatar file name to upload")

    viewModelScope.launch {
      try {
        PortalRepository.uploadAvatar(byteArray, mimeType, fileName)?.let { uploadResponse ->
          AppLog.d("uploadAvatar", "Uploaded url: ${uploadResponse.url}")
          _userInfo.value = _userInfo.value?.copy(avatar = uploadResponse.url)
          val response2 = PortalRepository.updateAvatar(uploadResponse.url)
          AppLog.d("uploadAvatar", "Update avatar response: $response2")
          onUploadComplete()
        } ?: run {
          AppLog.d("uploadAvatar", "Upload failed")
          throw Exception("Upload failed")
        }
      } catch (e: Exception) {
        AppLog.e("uploadAvatar", "Error uploading avatar: ${e.message}")
        throw e
      }
    }
  }

}
