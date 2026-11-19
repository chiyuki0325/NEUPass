package ink.chyk.pass.utils

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform
import platform.Foundation.NSBundle

actual fun platformVersionName(): String =
  NSBundle.mainBundle.objectForInfoDictionaryKey("NEUPassVersionName") as? String
    ?: NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
    ?: "Development Build"

@OptIn(ExperimentalNativeApi::class)
actual val isDebug: Boolean
  get() = Platform.isDebugBinary
