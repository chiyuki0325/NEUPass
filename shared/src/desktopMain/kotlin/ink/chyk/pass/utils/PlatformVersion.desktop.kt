package ink.chyk.pass.utils

actual fun platformVersionName(): String = "Development Build"

actual val isDebug: Boolean
  get() = java.lang.Boolean.getBoolean("ink.chyk.pass.debug")
