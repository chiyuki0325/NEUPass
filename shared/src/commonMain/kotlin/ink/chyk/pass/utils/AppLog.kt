package ink.chyk.pass.utils

internal enum class AppLogLevel { DEBUG, INFO, WARN, ERROR }

internal object AppLog {
  /** 调试总开关；发布前关闭即可同时禁用所有调试日志。 */
  var DEBUG: Boolean = true

  /** 网络请求日志开关，可独立于其他调试日志关闭。 */
  var NETWORK_DEBUG: Boolean = true

  /** 是否在网络日志中附带响应体，在应用启动时从设置中载入。 */
  var PRINT_RESPONSE_BODY: Boolean = false

  fun d(tag: String, message: String) = log(AppLogLevel.DEBUG, tag, message)
  fun i(tag: String, message: String) = log(AppLogLevel.INFO, tag, message)
  fun w(tag: String, message: String) = log(AppLogLevel.WARN, tag, message)
  fun e(tag: String, message: String) = log(AppLogLevel.ERROR, tag, message)

  private fun log(level: AppLogLevel, tag: String, message: String) {
    if (DEBUG) platformLog(level, tag, message)
  }
}

internal expect fun platformLog(level: AppLogLevel, tag: String, message: String)
