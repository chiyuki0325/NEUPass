package ink.chyk.pass.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun rememberLineHeight(style: TextStyle): Dp {
  val textMeasurer = rememberTextMeasurer()
  val density = LocalDensity.current
  return remember(style, density) {
    val result = textMeasurer.measure(
      text = "HeightPlaceholder", // 只要不为空，测量出的高度就是该样式的行高
      style = style,
      maxLines = 1
    )
    with(density) { result.size.height.toDp() }
  }
}

@Composable
fun TextSkeleton(
  style: TextStyle = MaterialTheme.typography.bodyMedium,
  width: Dp? = null,
  widthFraction: Float? = null,
  modifier: Modifier = Modifier
) {
  val height = rememberLineHeight(style)
  Box(
    modifier = modifier
      .then(
        when {
          width != null -> Modifier.width(width)
          widthFraction != null -> Modifier.fillMaxWidth(widthFraction)
          else -> Modifier.fillMaxWidth()
        }
      )
      .height(height)
      .clip(RoundedCornerShape(4.dp))
      .shimmerBackground()
  )
}
