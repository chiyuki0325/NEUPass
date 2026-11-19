package ink.chyk.pass.api

import platform.UIKit.UIDevice

actual fun deviceUserAgent(): String {
  val device = UIDevice.currentDevice
  return "${device.systemName}/${device.systemVersion} (${device.model})"
}
