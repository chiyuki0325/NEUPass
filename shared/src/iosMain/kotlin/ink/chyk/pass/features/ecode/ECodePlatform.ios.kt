package ink.chyk.pass.features.ecode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import ink.chyk.pass.utils.DateTimeUtils
import kotlinx.datetime.LocalTime
import platform.UIKit.UIScreen

@Composable
actual fun PlatformECodeEffects(raiseBrightness: Boolean) {
  val screen = UIScreen.mainScreen
  val originalBrightness = remember(screen) { screen.brightness }

  LaunchedEffect(screen, raiseBrightness) {
    val now = DateTimeUtils.currentLocalTime()
    screen.brightness = if (raiseBrightness && now >= LocalTime(7, 0) && now < LocalTime(22, 0)) {
      1.0
    } else {
      originalBrightness
    }
  }
  DisposableEffect(screen) {
    onDispose { screen.brightness = originalBrightness }
  }
}
