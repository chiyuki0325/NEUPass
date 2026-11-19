package ink.chyk.pass.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ink.chyk.pass.App
import ink.chyk.pass.AppWindow
import ink.chyk.pass.rememberAndroidAppWindowNavigator

class TermsOfServiceActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      App(AppWindow.TermsOfService, rememberAndroidAppWindowNavigator())
    }
  }
}
