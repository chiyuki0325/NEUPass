package ink.chyk.pass.features.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tencent.tmm.kmmresource.compose.stringResource
import com.tencent.tmm.kmmresource.compose.fileTextResource
import ink.chyk.pass.resources.MR
import ink.chyk.pass.ui.platformScaffoldContentWindowInsets
import ink.chyk.pass.web.CustomWebView
import ink.chyk.pass.web.rememberWebViewState
import kotlinx.serialization.json.JsonPrimitive

// 用一段 js 判断是否在首页
// 在首页时，滑动返回直接退出
// 否则传递
private val JWAPP_HOME_SCRIPT = """
  Array.from(document.querySelectorAll('div')).some(element => {
    const classes = Array.from(element.classList)
    return classes.some(name => name.startsWith('menuItemText_')) &&
      classes.some(name => name.startsWith('menuItemTextCurrent_')) &&
      element.textContent.trim() === '首页'
  })
""".trimIndent()

@Composable
fun JwappScreen(viewModel: JwappViewModel, onBack: () -> Unit) {
  val state by viewModel.state.collectAsState()
  val webViewState = rememberWebViewState()
  val css = fileTextResource(MR.files.jwxt_mobile_css)
  val javascript = fileTextResource(MR.files.jwxt_mobile_js)
  // 注入我们的移动端适配 js/css
  val extraScripts = remember(css, javascript) {
    """
      (() => {
        document.getElementById('neupass-jwxt-mobile')?.remove()
        const style = document.createElement('style')
        style.id = 'neupass-jwxt-mobile'
        style.textContent = ${JsonPrimitive(css)}
        document.head.appendChild(style)
        $javascript
      })()
    """.trimIndent()
  }

  Scaffold(contentWindowInsets = platformScaffoldContentWindowInsets()) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.Center,
    ) {
      when (val pageState = state) {
        // 正在登录
        JwappPageState.Loading -> CircularProgressIndicator()
        // 没网
        JwappPageState.Failed -> Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp),
          modifier = Modifier.padding(16.dp),
        ) {
          Text(stringResource(MR.strings.request_failed_content))
          TextButton(onClick = viewModel::load) {
            Text(stringResource(MR.strings.retry))
          }
          TextButton(onClick = onBack) {
            Text(stringResource(MR.strings.close))
          }
        }
        // 登录完成，显示 webview
        is JwappPageState.Ready -> CustomWebView(
          url = pageState.session.url,
          state = webViewState,
          cookies = pageState.session.cookies,
          modifier = Modifier.fillMaxSize(),
          isDarkMode = false,
          extraScripts = extraScripts,
          finish = onBack,
          backHandler = { defaultBack ->
            webViewState.evaluateJavascript(JWAPP_HOME_SCRIPT) { result ->
              if (result == "true") onBack() else defaultBack()
            }
          },
        )
      }
    }
  }
}
