package ink.chyk.pass.features.settings

import androidx.compose.runtime.Composable

@Composable
expect fun PlatformSpecificSettings(onOpenWebPage: (String) -> Unit)

@Composable
expect fun PlatformDynamicColorSetting(): Boolean
