package ink.chyk.pass.utils

internal actual fun platformLog(level: AppLogLevel, tag: String, message: String) {
  when (level) {
    AppLogLevel.DEBUG -> android.util.Log.d(tag, message)
    AppLogLevel.INFO -> android.util.Log.i(tag, message)
    AppLogLevel.WARN -> android.util.Log.w(tag, message)
    AppLogLevel.ERROR -> android.util.Log.e(tag, message)
  }
}
