package ink.chyk.pass

sealed interface AppWindow {
  val title: String

  data object Login : AppWindow {
    override val title = "NEU Pass 登录"
  }

  data class Main(val initialSection: RootSection = RootSection.ECODE) : AppWindow {
    override val title = "NEU Pass"
  }

  data object TermsOfService : AppWindow {
    override val title = "NEU Pass 用户协议"
  }

  data class CampusRun(val url: String?) : AppWindow {
    override val title = "NEU Pass 校园乐跑"
  }

  data object Jwapp : AppWindow {
    override val title = "NEU Pass 教务系统"
  }

  data object FakeECode : AppWindow {
    override val title = "e码通"
  }

  data class WebPage(
    val url: String,
    val scripts: String? = null,
    val cookies: String? = null,
  ) : AppWindow {
    override val title = "NEU Pass"
  }
}

enum class AppWindowOpenMode {
  PUSH,
  REPLACE,
  RESET,
}

interface AppWindowNavigator {
  fun open(window: AppWindow, mode: AppWindowOpenMode)
  fun close()
}
