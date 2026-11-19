package ink.chyk.pass

import android.app.Activity
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import ink.chyk.pass.activities.CampusRunningActivity
import ink.chyk.pass.activities.LoginActivity
import ink.chyk.pass.activities.JwappActivity
import ink.chyk.pass.activities.FakeECodeActivity
import ink.chyk.pass.activities.MainActivity
import ink.chyk.pass.activities.TermsOfServiceActivity
import ink.chyk.pass.activities.WebPageActivity

internal const val EXTRA_SCREEN = "screen"
internal const val EXTRA_URL = "url"
internal const val EXTRA_SCRIPTS = "scripts"
internal const val EXTRA_COOKIES = "cookies"

private class AndroidAppWindowNavigator(
  private val activity: Activity,
) : AppWindowNavigator {
  override fun open(window: AppWindow, mode: AppWindowOpenMode) {
    val target = when (window) {
      AppWindow.Login -> LoginActivity::class.java
      is AppWindow.Main -> MainActivity::class.java
      AppWindow.TermsOfService -> TermsOfServiceActivity::class.java
      is AppWindow.CampusRun -> CampusRunningActivity::class.java
      AppWindow.Jwapp -> JwappActivity::class.java
      AppWindow.FakeECode -> FakeECodeActivity::class.java
      is AppWindow.WebPage -> WebPageActivity::class.java
    }
    val intent = Intent(activity, target).apply {
      when (window) {
        is AppWindow.Main -> putExtra(EXTRA_SCREEN, window.initialSection.route)
        is AppWindow.CampusRun -> putExtra(EXTRA_URL, window.url)
        is AppWindow.WebPage -> {
          putExtra(EXTRA_URL, window.url)
          putExtra(EXTRA_SCRIPTS, window.scripts)
          putExtra(EXTRA_COOKIES, window.cookies)
        }
        else -> Unit
      }
      if (mode == AppWindowOpenMode.RESET) {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      }
    }
    activity.startActivity(intent)
    if (mode == AppWindowOpenMode.REPLACE) activity.finish()
  }

  override fun close() {
    activity.finish()
  }
}

@Composable
fun rememberAndroidAppWindowNavigator(): AppWindowNavigator {
  val activity = LocalContext.current as Activity
  return remember(activity) { AndroidAppWindowNavigator(activity) }
}
