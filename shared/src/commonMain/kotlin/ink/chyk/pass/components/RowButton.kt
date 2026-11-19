package ink.chyk.pass.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tencent.tmm.kmmresource.compose.painterResource
import com.tencent.tmm.kmmresource.resource.ImageResource
import ink.chyk.pass.resources.MR

@Composable
fun RowButton(
  iconResource: ImageResource,
  text: String,
  content: String? = null,
  clickable: Boolean = false,
  onClick: () -> Unit = {},
) {
  // 带图标和文字的行按钮，点击时触发 onClick 回调。若 clickable 为 false，则不显示右侧箭头。
  var rowModifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 16.dp).clip(RoundedCornerShape(8.dp))
  if (clickable) rowModifier = rowModifier.clickable(onClick = onClick)
  Row(rowModifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
    Row {
      Icon(painterResource(iconResource), contentDescription = text)
      Spacer(Modifier.width(16.dp)); Text(text)
      if (content != null) { Spacer(Modifier.width(8.dp)); Text(content, color = MaterialTheme.colorScheme.secondary) }
    }
    if (clickable) Icon(painterResource(MR.images.ic_fluent_chevron_right_20_filled), "前往") else Box {}
  }
}
