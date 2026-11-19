package ink.chyk.pass.api

import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 判断设备是否处于校园网。
 *
 * `ip.neu.edu.cn` 是校内可达的 IP 查询页；能正常完成 HTTP 请求就按校园网处理，
 * 超时、连接失败或非 2xx 则按校外处理。探测结果缓存一小段时间，
 * 避免一次课表导入里的多个接口各发一次探测请求。
 */
object CampusNetwork {
  private const val PROBE_URL = "http://ip.neu.edu.cn"
  private const val CACHE_MILLIS = 30_000L

  private val http by lazy {
    createApiClient {
      install(HttpTimeout) {
        requestTimeoutMillis = 3_000
        connectTimeoutMillis = 2_000
        socketTimeoutMillis = 3_000
      }
    }
  }

  private val probeMutex = Mutex()
  private var cachedResult: Boolean? = null
  private var checkedAt = 0L

  @OptIn(ExperimentalTime::class)
  suspend fun isCampusNetwork(): Boolean {
    val now = Clock.System.now().toEpochMilliseconds()
    cachedResult?.takeIf { now - checkedAt < CACHE_MILLIS }?.let { return it }

    return probeMutex.withLock {
      val lockedNow = Clock.System.now().toEpochMilliseconds()
      cachedResult?.takeIf { lockedNow - checkedAt < CACHE_MILLIS }?.let { return@withLock it }

      val result = probe()
      cachedResult = result
      checkedAt = lockedNow
      result
    }
  }

  /** 网络位置可能变化时清掉缓存，下一次调用重新探测。 */
  suspend fun invalidate() = probeMutex.withLock {
    cachedResult = null
  }

  private suspend fun probe(): Boolean = try {
    http.get(PROBE_URL).status.isSuccess()
  } catch (e: CancellationException) {
    throw e
  } catch (_: Exception) {
    false
  }
}