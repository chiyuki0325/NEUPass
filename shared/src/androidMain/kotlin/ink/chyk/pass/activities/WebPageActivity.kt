package ink.chyk.pass.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ink.chyk.pass.App
import ink.chyk.pass.AppWindow
import ink.chyk.pass.EXTRA_COOKIES
import ink.chyk.pass.EXTRA_SCRIPTS
import ink.chyk.pass.EXTRA_URL
import ink.chyk.pass.rememberAndroidAppWindowNavigator

class WebPageActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val url = intent.getStringExtra(EXTRA_URL) ?: "https://stu.neu.edu.cn/"
    enableEdgeToEdge()
    setContent {
      App(
        AppWindow.WebPage(
          url = url,
          scripts = intent.getStringExtra(EXTRA_SCRIPTS),
          cookies = intent.getStringExtra(EXTRA_COOKIES),
        ),
        rememberAndroidAppWindowNavigator(),
      )
    }
  }
}
