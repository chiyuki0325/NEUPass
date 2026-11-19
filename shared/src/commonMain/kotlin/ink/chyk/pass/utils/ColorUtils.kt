package ink.chyk.pass.utils

import kotlin.math.abs
import kotlin.math.roundToInt

object ColorUtils {
  private fun isCourseStopped(courseName: String): Boolean =
    courseName.startsWith("停课")

  private val hueCache = mutableMapOf<String, Float>()
  private val darkBgCache = mutableMapOf<Int, Int>()
  private val lightFgCache = mutableMapOf<Int, Int>()
  private val darkFgCache = mutableMapOf<Int, Int>()
  private val blockCache = mutableMapOf<Int, Int>()

  fun calcColor(courseName: String): Int {
    return hueCache.getOrSet(courseName) {
      (((courseName.hashCode() % 360) + 360) % 360).toFloat()
    }.let { hue ->
      val saturation = 0.5f
      val lightness = if (isCourseStopped(courseName)) 0.9f else 0.85f
      hslToColor(floatArrayOf(hue, saturation, lightness))
    }
  }

  fun Int.toHSL(): FloatArray {
    val hsl = FloatArray(3)
    colorToHsl(this, hsl)
    return hsl
  }

  fun <K, V> MutableMap<K, V>.getOrSet(key: K, defaultValue: () -> V): V {
    return if (containsKey(key)) {
      this[key]!!
    } else {
      defaultValue().also { this[key] = it }
    }
  }

  fun toBackgroundColor(color: Int, isDark: Boolean): Int {
    if (!isDark) return color

    return darkBgCache.getOrSet(color) {
      val hsl = color.toHSL()
      hsl[1] = 0.4f
      hsl[2] = 0.15f
      hslToColor(hsl)
    }
  }

  fun toForegroundColor(color: Int, isDark: Boolean): Int {
    return if (isDark) {
      darkFgCache.getOrSet(color) {
        val hsl = color.toHSL()
        hsl[1] = 0.65f
        hsl[2] = 0.6f
        hslToColor(hsl)
      }
    } else {
      lightFgCache.getOrSet(color) {
        val hsl = color.toHSL()
        hsl[1] = 0.6f
        hsl[2] = 0.3f
        hslToColor(hsl)
      }
    }
  }

  fun toBlockColor(color: Int): Int {
    return blockCache.getOrSet(color) {
      val hsl = color.toHSL()
      hsl[1] = 0.7f
      hsl[2] = 0.5f
      hslToColor(hsl)
    }
  }

  private fun colorToHsl(color: Int, out: FloatArray) {
    val red = ((color shr 16) and 0xff) / 255f
    val green = ((color shr 8) and 0xff) / 255f
    val blue = (color and 0xff) / 255f
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min
    val lightness = (max + min) / 2f

    val hue = when {
      delta == 0f -> 0f
      max == red -> 60f * (((green - blue) / delta) % 6f)
      max == green -> 60f * (((blue - red) / delta) + 2f)
      else -> 60f * (((red - green) / delta) + 4f)
    }.let { if (it < 0f) it + 360f else it }
    val saturation = if (delta == 0f) 0f else delta / (1f - abs(2f * lightness - 1f))
    out[0] = hue
    out[1] = saturation
    out[2] = lightness
  }

  private fun hslToColor(hsl: FloatArray): Int {
    val hue = ((hsl[0] % 360f) + 360f) % 360f
    val saturation = hsl[1].coerceIn(0f, 1f)
    val lightness = hsl[2].coerceIn(0f, 1f)
    val chroma = (1f - abs(2f * lightness - 1f)) * saturation
    val x = chroma * (1f - abs((hue / 60f) % 2f - 1f))
    val (red, green, blue) = when {
      hue < 60f -> Triple(chroma, x, 0f)
      hue < 120f -> Triple(x, chroma, 0f)
      hue < 180f -> Triple(0f, chroma, x)
      hue < 240f -> Triple(0f, x, chroma)
      hue < 300f -> Triple(x, 0f, chroma)
      else -> Triple(chroma, 0f, x)
    }
    val match = lightness - chroma / 2f
    val r = ((red + match) * 255f).roundToInt().coerceIn(0, 255)
    val g = ((green + match) * 255f).roundToInt().coerceIn(0, 255)
    val b = ((blue + match) * 255f).roundToInt().coerceIn(0, 255)
    return 0xff000000.toInt() or (r shl 16) or (g shl 8) or b
  }
}
