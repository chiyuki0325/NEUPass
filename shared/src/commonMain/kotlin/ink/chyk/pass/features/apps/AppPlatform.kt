package ink.chyk.pass.features.apps

import androidx.compose.runtime.Composable

expect fun openExternalWebApp(url: String)

@Composable
expect fun PlatformNetworkReconnectEffect(onConnected: () -> Unit)
