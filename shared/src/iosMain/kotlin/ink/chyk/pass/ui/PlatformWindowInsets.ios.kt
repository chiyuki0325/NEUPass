package ink.chyk.pass.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable

@Composable
actual fun platformScaffoldContentWindowInsets(): WindowInsets = WindowInsets.statusBars

@Composable
actual fun platformNavigationBarWindowInsets(): WindowInsets = WindowInsets(0, 0, 0, 0)
