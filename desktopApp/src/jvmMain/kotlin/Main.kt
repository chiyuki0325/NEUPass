import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.ctrip.flight.mmkv.initialize
import ink.chyk.pass.App
import ink.chyk.pass.AppWindow
import ink.chyk.pass.AppWindowNavigator
import ink.chyk.pass.AppWindowOpenMode
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicLong

private data class HostedWindow(val id: Long, val route: AppWindow)

fun main() {
    val dataDirectory = desktopDataDirectory()
    initialize(dataDirectory.toString())

    application {
        val nextWindowId = remember { AtomicLong(1) }
        val windows = remember {
            mutableStateListOf(HostedWindow(0, AppWindow.Main()))
        }

        fun closeWindow(id: Long) {
            windows.removeAll { it.id == id }
            if (windows.isEmpty()) exitApplication()
        }

        windows.forEach { hostedWindow ->
            key(hostedWindow.id) {
                Window(
                    onCloseRequest = { closeWindow(hostedWindow.id) },
                    title = hostedWindow.route.title,
                ) {
                    val navigator = remember(hostedWindow.id) {
                        object : AppWindowNavigator {
                            override fun open(window: AppWindow, mode: AppWindowOpenMode) {
                                val destination = HostedWindow(nextWindowId.getAndIncrement(), window)
                                when (mode) {
                                    AppWindowOpenMode.PUSH -> windows += destination
                                    AppWindowOpenMode.REPLACE -> {
                                        val index = windows.indexOfFirst { it.id == hostedWindow.id }
                                        if (index >= 0) windows[index] = destination
                                    }
                                    AppWindowOpenMode.RESET -> {
                                        windows.clear()
                                        windows += destination
                                    }
                                }
                            }

                            override fun close() {
                                closeWindow(hostedWindow.id)
                            }
                        }
                    }
                    App(hostedWindow.route, navigator)
                }
            }
        }
    }
}

private fun desktopDataDirectory(): Path {
    val userHome = Path.of(System.getProperty("user.home"))
    val osName = System.getProperty("os.name").lowercase()
    return when {
        osName.contains("win") -> {
            val localAppData = System.getenv("LOCALAPPDATA")
                ?.takeIf(String::isNotBlank)
                ?.let(Path::of)
                ?: userHome.resolve("AppData").resolve("Local")
            localAppData.resolve("NEUPass").resolve("mmkv")
        }
        osName.contains("mac") || osName.contains("darwin") ->
            userHome.resolve("Library").resolve("Application Support").resolve("NEUPass").resolve("mmkv")
        else -> {
            val xdgDataHome = System.getenv("XDG_DATA_HOME")
                ?.takeIf(String::isNotBlank)
                ?.let(Path::of)
                ?: userHome.resolve(".local").resolve("share")
            xdgDataHome.resolve("neupass").resolve("mmkv")
        }
    }
}
