package ink.chyk.pass.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tencent.tmm.kmmresource.compose.painterResource
import ink.chyk.pass.resources.MR

@Composable
fun SchoolTitle(text: String) {
  // 学校标题
  Row(
    modifier = Modifier.fillMaxWidth().height(64.dp).padding(vertical = 16.dp, horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Image(
      painter = painterResource(
        // 根据白天 / 黑夜模式选择不同的图标
        if (isSystemInDarkTheme()) MR.images.logo_white else MR.images.logo_black,
      ),
      contentDescription = "School Logo",
    )
    Text(text, style = MaterialTheme.typography.headlineSmall)
  }
}
