package ink.chyk.pass

import androidx.compose.runtime.Composable
import ink.chyk.pass.ui.SharedAppTheme

@Composable
actual fun PlatformTheme(content: @Composable () -> Unit) = SharedAppTheme(content = content)
