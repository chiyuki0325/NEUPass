package ink.chyk.pass.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

val ColorScheme.success: Color
    get() = if (surface.luminance() < 0.5f) Color(0xFF91D88C) else Color(0xFF256B2C)

val ColorScheme.warning: Color
    get() = if (surface.luminance() < 0.5f) Color(0xFFFFB86A) else Color(0xFF8B5000)
