package ink.chyk.pass.features.others

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ink.chyk.pass.ui.platformScaffoldContentWindowInsets
import ink.chyk.pass.web.CustomWebView
import ink.chyk.pass.web.rememberWebViewState

@Composable
fun WebPageScreen(
  url: String,
  scripts: String? = null,
  cookies: String? = null,
  onBack: () -> Unit,
) {
  val state = rememberWebViewState()
  Scaffold(contentWindowInsets = platformScaffoldContentWindowInsets()) { innerPadding ->
    CustomWebView(
      url = url,
      state = state,
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      finish = onBack,
      cookies = listOfNotNull(cookies),
      extraScripts = scripts,
    )
  }
}
