package ink.chyk.pass.features.settings

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.*
import com.tencent.tmm.kmmresource.compose.stringResource
import ink.chyk.pass.NEUPassApplication
import ink.chyk.pass.activities.MainActivity
import ink.chyk.pass.repositories.SettingsRepository
import ink.chyk.pass.resources.MR
import ink.chyk.pass.utils.isDebug

private fun createCoursesShortcut() {
  val context = NEUPassApplication.context
  val shortcutManager = context.getSystemService(ShortcutManager::class.java)
  val intent = Intent(context, MainActivity::class.java).apply {
    action = Intent.ACTION_VIEW
    putExtra("screen", "courses")
  }
  val shortcut = ShortcutInfo.Builder(context, "courses_shortcut")
    .setShortLabel(MR.strings.courses_shortcut_short.getString(context))
    .setLongLabel(MR.strings.courses_shortcut_long.getString(context))
    .setIcon(Icon.createWithResource(context, MR.images.ic_courses.drawableResId))
    .setIntent(intent)
    .build()
  shortcutManager.requestPinShortcut(shortcut, null)
  Toast.makeText(context, MR.strings.shortcut_created.getString(context), Toast.LENGTH_SHORT).show()
}

@Composable
actual fun PlatformSpecificSettings(onOpenWebPage: (String) -> Unit) {
  SettingsCard(
    stringResource(MR.strings.create_courses_shortcut),
    stringResource(MR.strings.create_courses_shortcut_description),
    MR.images.ic_fluent_calendar_24_regular,
    onClick = ::createCoursesShortcut,
  )

  if (!isDebug) return

  SettingsCard(
    stringResource(MR.strings.debug_tbs_kernel),
    "debugtbs.qq.com",
    MR.images.ic_fluent_code_24_regular,
    onClick = { onOpenWebPage("https://debugtbs.qq.com") },
  )
  SettingsCard(
    stringResource(MR.strings.debug_x5_kernel),
    "debugx5.qq.com",
    MR.images.ic_fluent_code_24_regular,
    onClick = { onOpenWebPage("https://debugx5.qq.com") },
  )
}

@Composable
actual fun PlatformDynamicColorSetting(): Boolean {
  val useSystemDynamicColor by SettingsRepository.useSystemDynamicColor.collectAsState()
  val available = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
  SettingsCard(
    name = stringResource(MR.strings.system_dynamic_color),
    description = stringResource(
      if (available) MR.strings.system_dynamic_color_description
      else MR.strings.system_dynamic_color_unavailable,
    ),
    icon = MR.images.ic_fluent_weather_sunny_low_24_regular,
    state = useSystemDynamicColor,
    enabled = available,
  ) {
    SettingsRepository.setUseSystemDynamicColor(!useSystemDynamicColor)
  }
  return available && useSystemDynamicColor
}
