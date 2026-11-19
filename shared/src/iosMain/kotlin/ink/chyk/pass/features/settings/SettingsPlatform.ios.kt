package ink.chyk.pass.features.settings

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformSpecificSettings(onOpenWebPage: (String) -> Unit) = Unit

@Composable
actual fun PlatformDynamicColorSetting(): Boolean = false
