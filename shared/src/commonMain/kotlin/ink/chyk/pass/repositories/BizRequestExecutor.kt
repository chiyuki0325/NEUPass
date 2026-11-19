package ink.chyk.pass.repositories

import ink.chyk.pass.SessionExpiredException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 这个类用于向外部提供authenticated方法，处理业务请求的会话过期问题。
 *
 * 一次业务请求发现会话已经过期后，将会尝试重登建立新session
 * 只有第一个请求真正执行重建，其余请求等待并复用新的会话。
 * 会话重建完成后，每个请求都会重新执行一次；如果重新执行后仍然返回 [SessionExpiredException]，就交给
 * [LoginRepository] 发布全局登录要求，让用户重新登录校园账号。
 */
internal class BizRequestExecutor {
  private val mutex = Mutex()
  private var generation = 0L

  suspend fun <T> authenticated(
    action: suspend () -> T,
    refresh: suspend () -> Unit,
    onRejectedAfterRefresh: suspend (SessionExpiredException) -> T = {
      LoginRepository.requireLogin()
    },
  ): T {
    val failedGeneration = mutex.withLock { generation }
    return try {
      action()
    } catch (_: SessionExpiredException) {
      refreshIfCurrent(failedGeneration, refresh)
      try {
        action()
      } catch (e: SessionExpiredException) {
        onRejectedAfterRefresh(e)
      }
    }
  }

  private suspend fun refreshIfCurrent(
    failedGeneration: Long,
    refresh: suspend () -> Unit,
  ) = mutex.withLock {
    if (generation != failedGeneration) return@withLock
    refresh()
    generation++
  }
}
