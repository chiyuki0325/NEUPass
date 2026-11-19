package ink.chyk.pass.repositories

import com.ctrip.flight.mmkv.defaultMMKV
import ink.chyk.pass.utils.AppLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 用户设置统一入口。
 *
 * 设置项的 MMKV key、默认值和读写方式都集中在这里，
 * UI 与业务代码不再直接操作 MMKV。
 */
object SettingsRepository {
  val DEFAULT_THEME_SEED_COLOR = 0xFF204198.toInt()

  private const val KEY_RAISE_ECODE_BRIGHTNESS = "ecode_raise_brightness"
  private const val KEY_CAMPUS_RUNNING_ALWAYS_DARK = "campus_running_always_dark"
  private const val KEY_REDACT_SENSITIVE_INFO = "redact_sensitive_info"
  private const val KEY_PRINT_RESPONSE_BODY = "print_response_body"
  private const val KEY_THEME_SEED_COLOR = "theme_seed_color"
  private const val KEY_USE_SYSTEM_DYNAMIC_COLOR = "use_system_dynamic_color"

  private val mmkv by lazy { defaultMMKV() }
  private var loggingSettingsInitialized = false
  private val _redactSensitiveInfo by lazy {
    MutableStateFlow(mmkv.getBoolean(KEY_REDACT_SENSITIVE_INFO, false))
  }
  val redactSensitiveInfo get() = _redactSensitiveInfo.asStateFlow()
  private val _printResponseBody by lazy {
    MutableStateFlow(mmkv.getBoolean(KEY_PRINT_RESPONSE_BODY, false))
  }
  val printResponseBody get() = _printResponseBody.asStateFlow()
  private val _themeSeedColor by lazy {
    MutableStateFlow(mmkv.getInt(KEY_THEME_SEED_COLOR, DEFAULT_THEME_SEED_COLOR))
  }
  val themeSeedColor get() = _themeSeedColor.asStateFlow()
  private val _useSystemDynamicColor by lazy {
    MutableStateFlow(mmkv.getBoolean(KEY_USE_SYSTEM_DYNAMIC_COLOR, true))
  }
  val useSystemDynamicColor get() = _useSystemDynamicColor.asStateFlow()

  fun initializeLoggingSettings() {
    if (loggingSettingsInitialized) return
    AppLog.PRINT_RESPONSE_BODY = _printResponseBody.value
    loggingSettingsInitialized = true
  }

  fun setRedactSensitiveInfo(value: Boolean) {
    mmkv[KEY_REDACT_SENSITIVE_INFO] = value
    _redactSensitiveInfo.value = value
  }

  fun setPrintResponseBody(value: Boolean) {
    mmkv[KEY_PRINT_RESPONSE_BODY] = value
    _printResponseBody.value = value
  }

  fun setThemeSeedColor(value: Int) {
    mmkv[KEY_THEME_SEED_COLOR] = value
    _themeSeedColor.value = value
  }

  fun setUseSystemDynamicColor(value: Boolean) {
    mmkv[KEY_USE_SYSTEM_DYNAMIC_COLOR] = value
    _useSystemDynamicColor.value = value
  }

  /** 一码通页面是否主动调高屏幕亮度。 */
  var raiseECodeBrightness: Boolean
    get() = mmkv.getBoolean(KEY_RAISE_ECODE_BRIGHTNESS, false)
    set(value) {
      mmkv[KEY_RAISE_ECODE_BRIGHTNESS] = value
    }

  /** 校园跑页面是否始终使用深色模式。 */
  var campusRunningAlwaysDark: Boolean
    get() = mmkv.getBoolean(KEY_CAMPUS_RUNNING_ALWAYS_DARK, false)
    set(value) {
      mmkv[KEY_CAMPUS_RUNNING_ALWAYS_DARK] = value
    }
}
