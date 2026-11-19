package ink.chyk.pass.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.unit.*


// 模拟灰色卡片波动加载效果
// credits: https://www.youtube.com/watch?v=NyO99OJPPec

@Composable
fun ShimmeringBox(modifier: Modifier, content: @Composable () -> Unit) {
  Box(
    modifier = modifier
      .shimmerBackground()
      .clip(MaterialTheme.shapes.medium)
  ) {
    content()
  }
}

fun Modifier.shimmerBackground(isLoading: Boolean = true): Modifier = composed {
  val isDark = isSystemInDarkTheme()
  val trans = rememberInfiniteTransition()
  var size by remember { mutableStateOf(IntSize.Zero) }

  val startOffsetX by trans.animateFloat(
    initialValue = -2 * size.width.toFloat(),
    targetValue = 2 * size.width.toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(2500),
    )
  )

  val color1 = if (isDark) Color.DarkGray else Color.LightGray
  val color2 = if (isDark) Color.Gray else Color.White

  background(
    brush = Brush.linearGradient(
      colors = listOf(
        color1,
        color2,
        color1,
      ),
      start = Offset(startOffsetX, 0f),
      end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat()),
    )
  ).onGloballyPositioned { size = it.size }
}

fun Modifier.shimmerContent(
  isLoading: Boolean = true,
  durationMillis: Int = 2500,
  sweepWidth: Float = 2f,
): Modifier = composed {
  if (!isLoading) return@composed this

  val isDark = isSystemInDarkTheme()
  val transition = rememberInfiniteTransition()
  val progress by transition.animateFloat(
    initialValue = -sweepWidth,
    targetValue = sweepWidth,
    animationSpec = infiniteRepeatable(animation = tween(durationMillis)),
  )
  val shimmerColors = if (isDark) {
    val shade = Color.Black.copy(alpha = 0.35f)
    listOf(shade, Color.Transparent, shade)
  } else {
    listOf(Color.Transparent, Color.White.copy(alpha = 0.8f), Color.Transparent)
  }

  graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
      drawContent()
      val startOffsetX = progress * size.width
      drawRect(
        brush = Brush.linearGradient(
          colors = shimmerColors,
          start = Offset(startOffsetX, 0f),
          end = Offset(startOffsetX + size.width, size.height),
        ),
        blendMode = BlendMode.SrcAtop,
      )
    }
}
