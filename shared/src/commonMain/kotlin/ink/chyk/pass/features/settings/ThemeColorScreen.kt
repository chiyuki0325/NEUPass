package ink.chyk.pass.features.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tencent.tmm.kmmresource.compose.stringResource
import ink.chyk.pass.components.BackNavigationBar
import ink.chyk.pass.repositories.SettingsRepository
import ink.chyk.pass.resources.MR
import ink.chyk.pass.utils.ColorUtils
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ThemeColorScreen(onBack: () -> Unit) {
  val storedSeedColor by SettingsRepository.themeSeedColor.collectAsState()
  val defaultHsl = remember {
    with(ColorUtils) { SettingsRepository.DEFAULT_THEME_SEED_COLOR.toHSL() }
  }
  val selectedHue = with(ColorUtils) { storedSeedColor.toHSL()[0] }
  val paletteDescription = stringResource(MR.strings.theme_color_palette)

  Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Spacer(Modifier.height(32.dp))
      BackNavigationBar(
        onBack = onBack,
        title = stringResource(MR.strings.theme_colors),
        modifier = Modifier.absoluteOffset(x = (-8).dp),
      )

      val usesSystemDynamicColor = PlatformDynamicColorSetting()
      Text(
        if (usesSystemDynamicColor) {
          stringResource(MR.strings.disable_system_dynamic_color_hint)
        } else {
          stringResource(MR.strings.custom_theme_color_description)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 8.dp),
      )
      BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().weight(1f),
        contentAlignment = Alignment.Center,
      ) {
        val diameter = minOf(maxWidth, maxHeight, 360.dp)
        HueRingPicker(
          hue = selectedHue,
          enabled = !usesSystemDynamicColor,
          contentDescription = paletteDescription,
          onHueChanged = { hue ->
            SettingsRepository.setThemeSeedColor(
              Color.hsl(
                hue = hue,
                saturation = defaultHsl[1],
                lightness = defaultHsl[2],
              ).toArgb(),
            )
          },
          modifier = Modifier
            .size(diameter)
            .alpha(if (usesSystemDynamicColor) 0.38f else 1f),
        )
      }
      TextButton(
        onClick = {
          SettingsRepository.setThemeSeedColor(SettingsRepository.DEFAULT_THEME_SEED_COLOR)
        },
        enabled = !usesSystemDynamicColor && storedSeedColor != SettingsRepository.DEFAULT_THEME_SEED_COLOR,
        modifier = Modifier.align(Alignment.End),
      ) {
        Text(stringResource(MR.strings.restore_default_theme_color))
      }
      Spacer(Modifier.height(16.dp))
    }
  }
}

@Composable
private fun HueRingPicker(
  hue: Float,
  enabled: Boolean,
  contentDescription: String,
  onHueChanged: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  val hueColors = remember {
    listOf(
      Color.Red,
      Color.Yellow,
      Color.Green,
      Color.Cyan,
      Color.Blue,
      Color.Magenta,
      Color.Red,
    )
  }
  Canvas(
    modifier = modifier
      .semantics { this.contentDescription = contentDescription }
      .pointerInput(enabled) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
          val down = awaitFirstDown(requireUnconsumed = false)
          val center = Offset(size.width / 2f, size.height / 2f)
          down.consume()
          onHueChanged(down.position.hueAround(center))
          do {
            val event = awaitPointerEvent()
            event.changes.forEach { change ->
              if (change.pressed) {
                onHueChanged(change.position.hueAround(center))
              }
              change.consume()
            }
          } while (event.changes.any { it.pressed })
        }
      },
  ) {
    val ringWidth = 32.dp.toPx()
    val ringRadius = size.minDimension / 2f - ringWidth / 2f
    drawCircle(
      brush = Brush.sweepGradient(hueColors, center),
      radius = ringRadius,
      style = Stroke(ringWidth),
    )

    val angle = hue / 180f * PI.toFloat()
    val indicatorCenter = center + Offset(
      x = cos(angle) * ringRadius,
      y = sin(angle) * ringRadius,
    )
    drawCircle(Color.White, radius = 10.dp.toPx(), center = indicatorCenter)
    drawCircle(
      color = Color.Black.copy(alpha = 0.55f),
      radius = 10.dp.toPx(),
      center = indicatorCenter,
      style = Stroke(2.dp.toPx()),
    )
  }
}

private fun Offset.hueAround(center: Offset): Float {
  val degrees = atan2(y - center.y, x - center.x) * 180f / PI.toFloat()
  return (degrees + 360f) % 360f
}
