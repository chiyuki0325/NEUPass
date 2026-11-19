package ink.chyk.pass.repositories

import ink.chyk.pass.*
import ink.chyk.pass.api.*
import ink.chyk.pass.api.ecard.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate

object ECardRepository : IRepository {
  private val directApi by lazy { ECardApi }
  private val webVpnApi by lazy { ECardWebVpnApi }
  private val bizRequestExecutor = BizRequestExecutor()
  private val loginMutex = Mutex()
  private var useWebVpn: Boolean? = null

  private val api: ECardRoutes
    get() = if (useWebVpn == true) webVpnApi else directApi

  fun getConsumeRecords(
    startDate: LocalDate,
    endDate: LocalDate,
    wallet: ECardWallet = ECardWallet.MAIN,
    page: Int = 1,
  ): Flow<CacheResult<ECardPage<ECardConsumeRecord>>> =
    cached<ECardPage<ECardConsumeRecord>>("ecard_consume_${startDate}_${endDate}_${wallet.name}_$page") {
      authenticated { it.getConsumeRecords(startDate, endDate, wallet, page) }
    }.flowOn(Dispatchers.IO)

  fun getBankRecords(
    startDate: LocalDate,
    endDate: LocalDate,
    wallet: ECardWallet = ECardWallet.MAIN,
    page: Int = 1,
  ): Flow<CacheResult<ECardPage<ECardBankRecord>>> =
    cached<ECardPage<ECardBankRecord>>("ecard_bank_${startDate}_${endDate}_${wallet.name}_$page") {
      authenticated { it.getBankRecords(startDate, endDate, wallet, page) }
    }.flowOn(Dispatchers.IO)

  override fun clearAllData() {
    directApi.clearSession()
    useWebVpn = null
  }

  private suspend fun <T> authenticated(block: suspend (ECardRoutes) -> T): T =
    try {
      ensureTransport()
      execute(block)
    } catch (exception: TransportException) {
      if (!switchTransport()) throw exception
      execute(block)
    }

  private suspend fun <T> execute(block: suspend (ECardRoutes) -> T): T =
    bizRequestExecutor.authenticated(
      action = { block(api) },
      refresh = ::login,
    )

  private suspend fun ensureTransport() = loginMutex.withLock {
    val newUseWebVpn = !CampusNetwork.isCampusNetwork()
    if (useWebVpn == newUseWebVpn) return@withLock
    loginInLock(newUseWebVpn)
  }

  private suspend fun login() = loginMutex.withLock {
    loginInLock(!CampusNetwork.isCampusNetwork())
  }

  private suspend fun switchTransport(): Boolean = loginMutex.withLock {
    CampusNetwork.invalidate()
    val newUseWebVpn = !CampusNetwork.isCampusNetwork()
    if (newUseWebVpn == useWebVpn) return@withLock false
    try {
      loginInLock(newUseWebVpn)
      true
    } catch (exception: CancellationException) {
      throw exception
    } catch (_: Exception) {
      false
    }
  }

  private suspend fun loginInLock(newUseWebVpn: Boolean) {
    if (newUseWebVpn) {
      try {
        WebVpnRepository.login()
        loginApi(webVpnApi)
        useWebVpn = true
        return
      } catch (exception: CancellationException) {
        throw exception
      } catch (exception: Exception) {
        CampusNetwork.invalidate()
        if (!CampusNetwork.isCampusNetwork()) throw exception
      }
    }
    loginApi(directApi)
    useWebVpn = false
  }

  private suspend fun loginApi(selectedApi: ECardRoutes) {
    try {
      selectedApi.login(AppAuthRepository.newAppTicket(CampusApp.ECARD))
    } catch (_: TicketExpiredException) {
      // ST 过期：换一张新 ST 再登录一次。
      selectedApi.login(AppAuthRepository.newAppTicket(CampusApp.ECARD))
    }
  }
}
