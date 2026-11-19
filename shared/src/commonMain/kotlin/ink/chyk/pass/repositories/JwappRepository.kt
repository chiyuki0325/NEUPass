package ink.chyk.pass.repositories

import com.ctrip.flight.mmkv.defaultMMKV
import ink.chyk.pass.TransportException
import ink.chyk.pass.api.CampusNetwork
import ink.chyk.pass.api.CampusApp
import ink.chyk.pass.api.jwapp.*
import ink.chyk.pass.api.webvpn.WebVpnCookiesStorage
import ink.chyk.pass.api.webvpn.WebVpnCrypto
import ink.chyk.pass.models.StoredGrade
import io.ktor.http.Url
import io.ktor.http.renderSetCookieHeader
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

object JwappRepository : IRepository {
  private const val GRADES_KEY = "jwapp_grades"
  private val mmkv by lazy { defaultMMKV() }
  private val directApi by lazy { JwappApi() }
  private val webVpnApi by lazy { JwappWebVpnApi() }
  private val bizRequestExecutor = BizRequestExecutor()
  private val loginMutex = Mutex()
  private val gradesMutex = Mutex()

  // 选路状态由仓储维护，API 实例本身不保存校园网/WebVPN 状态。
  private var useWebVpn: Boolean? = null

  private val api: JwappRoutes
    get() = if (useWebVpn == true) webVpnApi else directApi

  override fun clearAllData() {
    JwappCookiesStorage.clear()
    mmkv.removeValueForKey(GRADES_KEY)
    useWebVpn = null
  }

  suspend fun getCurrentTermId(): String = authenticated { it.getCurrentTermId() }

  fun getGpaSummary(): Flow<CacheResult<GpaSummary>> = cached("jwapp_gpa_summary") {
    authenticated { it.getGpaSummary() }
  }.flowOn(Dispatchers.IO)

  internal fun getStoredGrades(): List<StoredGrade> =
    if (mmkv.containsKey(GRADES_KEY)) {
      Json.decodeFromString<List<StoredGrade>>(mmkv.getString(GRADES_KEY))
    } else {
      emptyList()
    }

  fun getGrades(): Flow<CacheResult<List<StoredGrade>>> = flow {
    emit(CacheResult.Cached(getStoredGrades()))
    val grades = authenticated { it.getGrades() }.rows
    emit(CacheResult.Fresh(saveGrades(grades)))
  }.flowOn(Dispatchers.IO)

  @OptIn(ExperimentalTime::class)
  internal suspend fun saveGrades(grades: List<CourseGrade>): List<StoredGrade> =
    withContext(Dispatchers.IO) {
      gradesMutex.withLock {
        val stored = getStoredGrades().associateBy { it.grade.courseName }.toMutableMap()
        val now = Clock.System.now().toEpochMilliseconds()
        grades.forEach { grade ->
          val previous = stored[grade.courseName]
          if (previous == null || previous.grade.gpa != grade.gpa) {
            stored[grade.courseName] = StoredGrade(grade, now)
          }
        }
        val updated = stored.values.sortedByDescending { it.storedAt }
        mmkv[GRADES_KEY] = Json.encodeToString(updated)
        updated
      }
    }

  suspend fun getWebSession(): JwappWebSession {
    CampusNetwork.invalidate()
    return authenticated { selectedApi ->
      selectedApi.getCurrentUser()
      val directUrl = CampusApp.JWAPP.callbackUrl
      val viaWebVpn = selectedApi === webVpnApi
      val url = if (viaWebVpn) WebVpnCrypto.encode(directUrl) else directUrl
      val storage = if (viaWebVpn) WebVpnCookiesStorage else JwappCookiesStorage
      JwappWebSession(
        url = url,
        cookies = storage.get(Url(url)).map { cookie ->
          renderSetCookieHeader(cookie.copy(path = cookie.path ?: "/"))
        },
      )
    }
  }
  suspend fun getScheduledCampuses(termId: String): List<ScheduledCampus> = authenticated {
    it.getScheduledCampuses(termId)
  }

  suspend fun getTermWeeks(termId: String): List<TermWeek> = authenticated {
    it.getTermWeeks(termId)
  }

  suspend fun getScheduleDetail(termId: String, campusId: String): ScheduleDetail = authenticated {
    it.getScheduleDetail(termId, campusId)
  }

  suspend fun getExamArrangement(termId: String): ExamArrangement = authenticated {
    it.getExamArrangement(termId)
  }

  private suspend fun <T> authenticated(request: suspend (JwappRoutes) -> T): T {
    ensureTransport()
    return try {
      execute(request)
    } catch (e: TransportException) {
      // 直连超时可能意味着已经离开校园网，重新探测后换路重试一次。
      if (!switchTransport()) throw e
      execute(request)
    }
  }

  private suspend fun <T> execute(request: suspend (JwappRoutes) -> T): T =
    bizRequestExecutor.authenticated(
      action = { request(api) },
      refresh = ::login,
    )

  // 网络位置变化时切换 API 并主动登录，避免业务请求先撞上旧会话。
  private suspend fun ensureTransport() = loginMutex.withLock {
    val newUseWebVpn = !CampusNetwork.isCampusNetwork()
    if (useWebVpn == newUseWebVpn) return@withLock
    useWebVpn = newUseWebVpn
    loginInLock()
  }

  private suspend fun login() = loginMutex.withLock {
    useWebVpn = !CampusNetwork.isCampusNetwork()
    loginInLock()
  }

  // 传输层失败后强制重新探测；只有新线路真正登录成功才提交切换。
  // 探测失败可能是断网/系统问题，不一定是离开了校园网，所以失败时回滚并保留原始错误。
  private suspend fun switchTransport(): Boolean = loginMutex.withLock {
    CampusNetwork.invalidate()
    val newUseWebVpn = !CampusNetwork.isCampusNetwork()
    if (newUseWebVpn == useWebVpn) return@withLock false

    val previous = useWebVpn
    useWebVpn = newUseWebVpn
    try {
      loginInLock()
      true
    } catch (e: CancellationException) {
      useWebVpn = previous
      throw e
    } catch (_: Exception) {
      useWebVpn = previous
      false
    }
  }

  private suspend fun loginInLock() {
    val newAppTicket: suspend (CampusApp) -> String = { app ->
      AppAuthRepository.newAppTicket(app)
    }
    if (useWebVpn == true) {
      try {
        WebVpnRepository.login()
        webVpnApi.login(newAppTicket)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        // WebVPN 在校园网会被直接拒绝；重新探测后确实在校园网就改走直连。
        CampusNetwork.invalidate()
        if (!CampusNetwork.isCampusNetwork()) throw e
        useWebVpn = false
        directApi.login(newAppTicket)
      }
    } else {
      directApi.login(newAppTicket)
    }
  }
}

data class JwappWebSession(
  val url: String,
  val cookies: List<String>,
)
