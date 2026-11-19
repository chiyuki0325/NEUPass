package ink.chyk.pass.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

val Dp.px: Int
    @Composable @ReadOnlyComposable
    get() = with(LocalDensity.current) { toPx().toInt() }
