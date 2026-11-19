package ink.chyk.pass.api

import android.os.Build

// Android 不引入 WebView，沿用设备描述的取法（Build 信息）拼 UA。
actual fun deviceUserAgent(): String =
  "${Build.BRAND}_${Build.DEVICE} Android/${Build.VERSION.RELEASE} (${Build.MODEL})"
