package ink.chyk.pass.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable

@Composable
expect fun platformScaffoldContentWindowInsets(): WindowInsets

@Composable
expect fun platformNavigationBarWindowInsets(): WindowInsets
