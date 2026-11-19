package ink.chyk.pass.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.tencent.tmm.kmmresource.compose.painterResource
import com.tencent.tmm.kmmresource.compose.stringResource
import com.tencent.tmm.kmmresource.resource.ImageResource
import ink.chyk.pass.repositories.SettingsRepository
import ink.chyk.pass.resources.MR
import ink.chyk.pass.ui.platformScaffoldContentWindowInsets
import ink.chyk.pass.utils.isDebug

@Composable
fun SettingsScreen(
  onBack: () -> Unit,
  onOpenThemeColors: () -> Unit,
  onOpenWebPage: (String) -> Unit,
) {
  val redactSensitiveInfo by SettingsRepository.redactSensitiveInfo.collectAsState()
  val printResponseBody by SettingsRepository.printResponseBody.collectAsState()
  var raiseECodeBrightness by remember {
    mutableStateOf(SettingsRepository.raiseECodeBrightness)
  }
  var campusRunningAlwaysDarkState by remember {
    mutableStateOf(SettingsRepository.campusRunningAlwaysDark)
  }

  Scaffold(contentWindowInsets = platformScaffoldContentWindowInsets()) { innerPadding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Spacer(modifier = Modifier.height(32.dp))
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.absoluteOffset(x = (-8).dp)
      ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
          Icon(painterResource(MR.images.ic_fluent_chevron_left_20_filled), "Back")
        }
        Text(stringResource(MR.strings.settings), style = MaterialTheme.typography.headlineLarge)
      }
      Box {}
      SettingsCard(
        stringResource(MR.strings.theme_colors),
        stringResource(MR.strings.theme_colors_description),
        MR.images.ic_fluent_calligraphy_pen_24_regular,
        onClick = onOpenThemeColors,
      )
      SettingsCard(
        stringResource(MR.strings.raise_passcode_brightness),
        stringResource(MR.strings.raise_passcode_brightness_description),
        MR.images.ic_fluent_weather_sunny_24_regular,
        raiseECodeBrightness,
      ) {
        raiseECodeBrightness = !raiseECodeBrightness
        SettingsRepository.raiseECodeBrightness = raiseECodeBrightness
      }
      SettingsCard(
        stringResource(MR.strings.campus_running_always_dark),
        stringResource(MR.strings.campus_running_always_dark_description),
        MR.images.ic_fluent_weather_moon_24_regular,
        campusRunningAlwaysDarkState,
      ) {
        campusRunningAlwaysDarkState = !campusRunningAlwaysDarkState
        SettingsRepository.campusRunningAlwaysDark = campusRunningAlwaysDarkState
      }

      if (isDebug) {
        SettingsCard(
          stringResource(MR.strings.print_response_body),
          stringResource(MR.strings.print_response_body_description),
          MR.images.ic_fluent_code_circle_24_regular,
          printResponseBody,
        ) {
          SettingsRepository.setPrintResponseBody(!printResponseBody)
        }
        SettingsCard(
          stringResource(MR.strings.redact_sensitive_info),
          stringResource(MR.strings.redact_sensitive_info_description),
          MR.images.ic_fluent_eye_off_24_regular,
          redactSensitiveInfo,
        ) {
          SettingsRepository.setRedactSensitiveInfo(!redactSensitiveInfo)
        }
      }

      PlatformSpecificSettings(onOpenWebPage)
    }
  }
}

@Composable
fun SettingsCard(
  name: String,
  description: String,
  icon: ImageResource,
  state: Boolean? = null,
  enabled: Boolean = true,
  onClick: () -> Unit,
) {
  Card(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(16.dp).defaultMinSize(minHeight = 48.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Icon(painterResource(icon), name, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(name, fontWeight = FontWeight.Bold)
          Text(description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth(), softWrap = true)
        }
      }
      if (state != null) {
        Spacer(modifier = Modifier.width(16.dp))
        Switch(checked = state, enabled = enabled, onCheckedChange = { onClick() })
      }
    }
  }
}
