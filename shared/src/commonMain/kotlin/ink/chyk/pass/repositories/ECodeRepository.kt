package ink.chyk.pass.repositories

import ink.chyk.pass.*
import ink.chyk.pass.api.*
import ink.chyk.pass.api.ecode.*
import io.ktor.http.Url
import io.ktor.http.renderSetCookieHeader
import kotlinx.coroutines.flow.*

object ECodeRepository : IRepository {
  private val api by lazy { ECodeApi() }
  private val bizRequestExecutor = BizRequestExecutor()

  suspend fun getQrCode(): ECodeQrCode = authenticated {
    api.getQrCode()
  }

  fun getUserInfoFlow(): Flow<ECodeUserInfo?> = flow {
    emit(authenticated { api.getUserInfo() })
  }

  suspend fun getWebSession(): ECodeWebSession = authenticated {
    api.getUserInfo()
    ECodeWebSession(
      url = ECODE_REDIRECT_URL,
      cookies = ECodeCookiesStorage.get(Url(ECODE_REDIRECT_URL)).map { cookie ->
        renderSetCookieHeader(cookie.copy(path = "/"))
      },
    )
  }

  override fun clearAllData() {
    api.clearSession()
  }

  private suspend fun <T> authenticated(block: suspend () -> T): T =
    bizRequestExecutor.authenticated(
      action = block,
      refresh = ::login,
    )

  private suspend fun login() {
    try {
      api.login(AppAuthRepository.newAppTicket(CampusApp.ECODE))
    } catch (_: TicketExpiredException) {
      // ST 过期：换一张新 ST 再登录一次。
      api.login(AppAuthRepository.newAppTicket(CampusApp.ECODE))
    }
  }
}

data class ECodeWebSession(
  val url: String,
  val cookies: List<String>,
)
