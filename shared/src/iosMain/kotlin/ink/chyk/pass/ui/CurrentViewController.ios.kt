@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package ink.chyk.pass.ui

import platform.UIKit.UIApplication
import platform.UIKit.UINavigationController
import platform.UIKit.UITabBarController
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

internal fun currentViewController(): UIViewController? {
  val window = UIApplication.sharedApplication.connectedScenes
    .filterIsInstance<UIWindowScene>()
    .flatMap { it.windows.filterIsInstance<UIWindow>() }
    .firstOrNull { it.keyWindow }
    ?: UIApplication.sharedApplication.keyWindow
  return window?.rootViewController?.visibleViewController()
}

private fun UIViewController.visibleViewController(): UIViewController =
  presentedViewController?.visibleViewController()
    ?: when (this) {
      is UINavigationController -> visibleViewController?.visibleViewController() ?: this
      is UITabBarController -> selectedViewController?.visibleViewController() ?: this
      else -> this
    }
