package ink.chyk.pass.api

import java.net.InetAddress

// Desktop 没有系统 UA，用主机名和系统属性拼一个可读的设备描述。
actual fun deviceUserAgent(): String {
  val hostname = runCatching { InetAddress.getLocalHost().hostName }.getOrDefault("unknown-host")
  return "$hostname ${System.getProperty("os.name")}/${System.getProperty("os.version")} (${System.getProperty("os.arch")})"
}
