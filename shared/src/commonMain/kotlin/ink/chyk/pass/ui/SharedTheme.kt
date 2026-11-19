package ink.chyk.pass.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.materialkolor.rememberDynamicColorScheme
import ink.chyk.pass.repositories.SettingsRepository

@Composable
fun SharedAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val seedColor by SettingsRepository.themeSeedColor.collectAsState()
    GeneratedAppTheme(Color(seedColor), darkTheme, content)
}

@Composable
fun GeneratedAppTheme(
    seedColor: Color,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = rememberDynamicColorScheme(seedColor = seedColor, isDark = darkTheme)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
