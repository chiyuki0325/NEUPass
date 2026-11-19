package ink.chyk.pass.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ink.chyk.pass.App
import ink.chyk.pass.AppWindow
import ink.chyk.pass.EXTRA_URL
import ink.chyk.pass.rememberAndroidAppWindowNavigator

class CampusRunningActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      App(
        AppWindow.CampusRun(intent.getStringExtra(EXTRA_URL)),
        rememberAndroidAppWindowNavigator(),
      )
    }
  }
}
