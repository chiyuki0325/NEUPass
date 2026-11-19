package ink.chyk.pass.features.others

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tencent.tmm.kmmresource.compose.painterResource
import com.tencent.tmm.kmmresource.compose.stringResource
import ink.chyk.pass.resources.MR
import ink.chyk.pass.ui.platformScaffoldContentWindowInsets

// 报错信息提示窗口（土豆服务器）
@Composable
fun RequestErrorScreen(isConnected: Boolean, onRetry: () -> Unit) {
  Scaffold(contentWindowInsets = platformScaffoldContentWindowInsets()) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(16.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Image(
            painter = painterResource(MR.images.no_internet),
            contentDescription = "断网图标",
            modifier = Modifier.size(32.dp),
          )
          Spacer(Modifier.width(8.dp))
          Text(
            text = stringResource(MR.strings.request_failed_title),
            style = MaterialTheme.typography.headlineMedium,
          )
        }
        Spacer(Modifier.height(8.dp))
        Text(
          text = stringResource(MR.strings.request_failed_content),
          textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Image(
          painter = painterResource(MR.images.potato_server),
          contentDescription = "土豆服务器",
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(
              width = 1.dp,
              color = MaterialTheme.colorScheme.onSurface,
              shape = RoundedCornerShape(8.dp),
            ),
        )
        Spacer(Modifier.height(32.dp))
        Button(
          onClick = onRetry,
          modifier = Modifier.width(96.dp),
          enabled = isConnected,
        ) {
          Text(text = stringResource(MR.strings.retry))
        }
      }
    }
  }
}
