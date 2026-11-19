package ink.chyk.pass.features.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tencent.tmm.kmmresource.compose.painterResource
import com.tencent.tmm.kmmresource.compose.stringResource
import com.tencent.tmm.kmmresource.resource.StringResource
import ink.chyk.pass.repositories.SettingsRepository
import ink.chyk.pass.resources.MR
import ink.chyk.pass.ui.platformScaffoldContentWindowInsets
import ink.chyk.pass.web.CustomWebView
import ink.chyk.pass.web.rememberWebViewState

@Composable
fun CampusRunningScreen(url: String?, onBack: () -> Unit) {
  if (url == null) {
    CampusRunningErrorScreen(MR.strings.args_error, MR.strings.args_error_content)
    return
  }
  val permissionGranted = rememberLocationPermissionGranted()
  val webViewState = rememberWebViewState()
  Scaffold(contentWindowInsets = platformScaffoldContentWindowInsets()) { innerPadding ->
    if (permissionGranted) {
      val alwaysDark = SettingsRepository.campusRunningAlwaysDark
      CustomWebView(
        url,
        state = webViewState,
        modifier = Modifier.padding(innerPadding),
        listenBack = false,
        finish = onBack,
        isDarkMode = alwaysDark || isSystemInDarkTheme(),
      )
    } else {
      CampusRunningErrorScreen(
        MR.strings.location_permission_required,
        MR.strings.location_permission_required_content,
        Modifier.padding(innerPadding),
      )
    }
  }
}

@Composable
private fun CampusRunningErrorScreen(
  title: StringResource,
  content: StringResource,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Image(painterResource(MR.images.neko1), "Neko")
    Spacer(Modifier.height(32.dp))
    Text(stringResource(title), style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(4.dp))
    Text(stringResource(content), textAlign = TextAlign.Center)
  }
}

@Composable
expect fun rememberLocationPermissionGranted(): Boolean
