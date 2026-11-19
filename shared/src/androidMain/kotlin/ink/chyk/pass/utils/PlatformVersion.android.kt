package ink.chyk.pass.utils

import android.content.pm.ApplicationInfo
import ink.chyk.pass.NEUPassApplication

actual fun platformVersionName(): String = NEUPassApplication.context.packageManager
  .getPackageInfo(NEUPassApplication.context.packageName, 0).versionName ?: "Development Build"

actual val isDebug: Boolean
  get() = NEUPassApplication.context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
