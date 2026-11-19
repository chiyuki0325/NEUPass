package ink.chyk.pass

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ComposeUIViewController
import com.ctrip.flight.mmkv.initialize
import ink.chyk.pass.ui.SharedAppTheme
import platform.UIKit.UINavigationController
import platform.UIKit.UIViewController

@Composable
actual fun PlatformTheme(content: @Composable () -> Unit) = SharedAppTheme(content = content)

fun MainViewController(): UIViewController {
  initialize()
  return IosAppWindowHost().viewController
}

private class IosAppWindowHost {
  val viewController = UINavigationController()

  init {
    viewController.setNavigationBarHidden(true, animated = false)
    viewController.setViewControllers(
      viewControllers = listOf(createViewController(AppWindow.Main())),
      animated = false,
    )
  }

  private fun createViewController(window: AppWindow): UIViewController {
    lateinit var currentViewController: UIViewController
    val navigator = object : AppWindowNavigator {
      override fun open(window: AppWindow, mode: AppWindowOpenMode) {
        val destination = createViewController(window)
        when (mode) {
          AppWindowOpenMode.PUSH -> viewController.pushViewController(destination, animated = true)
          AppWindowOpenMode.REPLACE -> {
            val controllers = viewController.viewControllers.toMutableList()
            val index = controllers.indexOf(currentViewController)
            if (index >= 0) {
              controllers[index] = destination
              viewController.setViewControllers(controllers, animated = false)
            }
          }
          AppWindowOpenMode.RESET -> viewController.setViewControllers(
            viewControllers = listOf(destination),
            animated = false,
          )
        }
      }

      override fun close() {
        if (viewController.topViewController == currentViewController) {
          viewController.popViewControllerAnimated(true)
        }
      }
    }
    currentViewController = ComposeUIViewController {
      App(window, navigator)
    }
    return currentViewController
  }
}
