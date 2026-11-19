package ink.chyk.pass

import androidx.compose.runtime.Composable
import ink.chyk.pass.ui.AppTheme

@Composable
actual fun PlatformTheme(content: @Composable () -> Unit) = AppTheme(content = content)
