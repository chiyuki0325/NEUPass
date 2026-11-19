package ink.chyk.pass.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable

@Composable
actual fun platformScaffoldContentWindowInsets(): WindowInsets =
  ScaffoldDefaults.contentWindowInsets

@Composable
actual fun platformNavigationBarWindowInsets(): WindowInsets =
  NavigationBarDefaults.windowInsets
