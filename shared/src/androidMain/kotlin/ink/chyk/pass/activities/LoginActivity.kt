package ink.chyk.pass.activities

import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ink.chyk.pass.App
import ink.chyk.pass.AppWindow
import ink.chyk.pass.rememberAndroidAppWindowNavigator

class LoginActivity : ComponentActivity() {
  // 登录窗口
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val lightTransparentStyle = SystemBarStyle.light(scrim = TRANSPARENT, darkScrim = TRANSPARENT)
    enableEdgeToEdge(statusBarStyle = lightTransparentStyle, navigationBarStyle = lightTransparentStyle)
    setContent {
      App(AppWindow.Login, rememberAndroidAppWindowNavigator())
    }
  }
}
