package ink.chyk.pass.features.ecode

import android.view.*
import androidx.activity.compose.*
import androidx.compose.runtime.*
import ink.chyk.pass.ui.setScreenBrightness
import ink.chyk.pass.utils.*
import kotlinx.datetime.*

@Composable
actual fun PlatformECodeEffects(raiseBrightness: Boolean) {
    val activity = LocalActivity.current
    LaunchedEffect(activity, raiseBrightness) {
        val now = DateTimeUtils.currentLocalTime()
        if (raiseBrightness && now >= LocalTime(7, 0) && now < LocalTime(22, 0)) {
            activity?.let { setScreenBrightness(it.window, 1.0f) }
        }
    }
    DisposableEffect(activity) {
        onDispose {
            activity?.let { setScreenBrightness(it.window, WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) }
        }
    }
}
