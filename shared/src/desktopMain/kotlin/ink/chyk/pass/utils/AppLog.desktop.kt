package ink.chyk.pass.utils

internal actual fun platformLog(level: AppLogLevel, tag: String, message: String) {
  println("[${level.name}] [$tag] $message")
}
