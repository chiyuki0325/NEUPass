package ink.chyk.pass.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tencent.mmkv.MMKV
import com.tencent.smtt.export.external.TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE
import com.tencent.smtt.export.external.TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER
import com.tencent.smtt.sdk.QbSdk.PreInitCallback
import com.tencent.smtt.sdk.QbSdk.initTbsSettings
import com.tencent.smtt.sdk.QbSdk.initX5Environment
import com.tencent.smtt.sdk.QbSdk.setDownloadWithoutWifi
import ink.chyk.pass.App
import ink.chyk.pass.AppWindow
import ink.chyk.pass.EXTRA_SCREEN
import ink.chyk.pass.rememberAndroidAppWindowNavigator
import ink.chyk.pass.rootSection
import kotlinx.serialization.ExperimentalSerializationApi

class MainActivity : ComponentActivity() {
  // Universal Pass 主窗口；所有业务 UI 均由 common App 提供。
  @OptIn(ExperimentalSerializationApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    MMKV.initialize(this)

    val screen = intent.getStringExtra(EXTRA_SCREEN)
    Log.d("MainActivity", "screen: $screen")

    setDownloadWithoutWifi(true) // 学生应该都有流量卡
    initTbsSettings(
      hashMapOf(
        TBS_SETTINGS_USE_SPEEDY_CLASSLOADER to true,
        TBS_SETTINGS_USE_DEXLOADER_SERVICE to true,
      ) as Map<String, Boolean>,
    )
    initX5Environment(applicationContext, object : PreInitCallback {
      override fun onCoreInitFinished() {
        Log.d("MainActivity", "X5 Core Init Finished")
      }

      override fun onViewInitFinished(isX5: Boolean) {
        Log.d("MainActivity", "X5 View Init Finished: $isX5")
      }
    })

    enableEdgeToEdge()
    setContent {
      App(
        window = AppWindow.Main(rootSection(screen)),
        navigator = rememberAndroidAppWindowNavigator(),
      )
    }
  }
}
