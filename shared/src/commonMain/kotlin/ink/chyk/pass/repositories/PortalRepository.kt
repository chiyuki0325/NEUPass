package ink.chyk.pass.repositories

import com.ctrip.flight.mmkv.defaultMMKV
import ink.chyk.pass.*
import ink.chyk.pass.api.CampusApp
import ink.chyk.pass.api.portal.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

object PortalRepository : IRepository {
  private const val AVATAR_CACHE_PREFIX = "portal_avatar_"

  private val mmkv by lazy { defaultMMKV() }
  private val api by lazy { PortalApi() }
  private val bizRequestExecutor = BizRequestExecutor()

  // "card.balance" -> "data_item_id_12345"
  private var ids: Map<PortalDataItemKey, String>? =
    mmkv.getString("portal_data_item_ids").takeIf { it.isNotEmpty() }?.let {
      runCatching { Json.decodeFromString<Map<PortalDataItemKey, String>>(it) }.getOrNull()
    }

  override fun clearAllData() {
    clearSessionData()
  }

  private fun clearSessionData() {
    ids = null
    PortalCookiesStorage.clear()
    mmkv.removeValueForKey("portal_data_item_ids")
  }

  suspend fun getUserInfo(confirmFailureVisible: () -> Boolean): Flow<CacheResult<UserInfo?>> = cached("portal_user_info") {
    val resp = authenticated {
      portalRead(confirmFailureVisible) { api.getUserInfo() }
    }
    return@cached resp?.info
  }

  suspend fun getCardBalance(confirmFailureVisible: () -> Boolean): Flow<CacheResult<DataItem?>> =
    getPortalDataItem(PortalDataItemKey.CARD_BALANCE, confirmFailureVisible)

  suspend fun getNetBalance(confirmFailureVisible: () -> Boolean): Flow<CacheResult<DataItem?>> =
    getPortalDataItem(PortalDataItemKey.NET_BALANCE, confirmFailureVisible)

  // 头像按地址缓存，先给缓存、再给网络结果。
  fun getAvatar(path: String): Flow<CacheResult<ByteArray>> =
    cachedBytes("$AVATAR_CACHE_PREFIX$path") { authenticated { api.getAvatar(path) } }

  suspend fun uploadAvatar(
    byteArray: ByteArray,
    mimeType: String,
    fileName: String,
  ): UploadImageResponse? = authenticated { api.uploadImage(byteArray, mimeType, fileName) }

  suspend fun updateAvatar(avatarUrl: String): List<String>? =
    authenticated { api.updateAvatar(avatarUrl) }

  private suspend fun fetchPortalDataItemIds(
    confirmFailureVisible: () -> Boolean,
  ): Map<PortalDataItemKey, String>? {
    val resp = portalRead(confirmFailureVisible) { api.getDataItemIds() }
    return resp?.idMap()
  }

  @Throws(EHallPortalException::class, CancellationException::class)
  private suspend fun getIds(confirmFailureVisible: () -> Boolean): Map<PortalDataItemKey, String> {
    // 如果第一次就断网那只能抛异常了 没招了
    ids?.let { return it }
    val fetchedIds = fetchPortalDataItemIds(confirmFailureVisible) ?: throw EHallPortalException()
    ids = fetchedIds
    mmkv["portal_data_item_ids"] = Json.encodeToString(fetchedIds)
    return fetchedIds
  }

  private suspend fun getPortalDataItem(
    key: PortalDataItemKey,
    confirmFailureVisible: () -> Boolean,
  ) = cached("portal_data_item_$key") {
    val resp = authenticated {
      val dataItemId = getIds(confirmFailureVisible)[key]
        ?: throw EHallPortalException("Data item ID not found for key: $key")
      portalRead(confirmFailureVisible) { api.getDataItem(dataItemId) }
    }
    return@cached resp?.data
  }

  // Downgrade only after the caller makes the transport failure visible.
  private suspend fun <T> portalRead(
    confirmFailureVisible: () -> Boolean,
    action: suspend () -> T,
  ): T? =
    try {
      action()
    } catch (e: NetworkException) {
      if (confirmFailureVisible()) null else throw e
    }

  private suspend fun <T> authenticated(action: suspend () -> T): T =
    bizRequestExecutor.authenticated(
      action = action,
      refresh = ::login,
    )

  private suspend fun login() {
    clearSessionData()
    try {
      api.login(AppAuthRepository.newAppTicket(CampusApp.PORTAL))
    } catch (_: TicketExpiredException) {
      // ST 过期：换一张新 ST 再登录一次。
      api.login(AppAuthRepository.newAppTicket(CampusApp.PORTAL))
    }
  }
}
