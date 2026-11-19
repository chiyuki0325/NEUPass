package ink.chyk.pass.api

import ink.chyk.pass.utils.platformVersionName

// 固定 UA 前缀
private const val PROTOCOL_UA_PREFIX = "ZhilinEai ZhilinNeuApp/3.1.5"

// 设备实际 UA，由各平台提供。
expect fun deviceUserAgent(): String

// 对外请求的 User-Agent：协议前缀 + 应用版本 + 设备实际 UA。
fun appUserAgent(): String =
  "$PROTOCOL_UA_PREFIX  NEUPass/${platformVersionName()} ${deviceUserAgent()}"
