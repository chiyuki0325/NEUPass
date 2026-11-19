package ink.chyk.pass.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.layout.ContentScale
import ink.chyk.pass.api.sharedApiEngine
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes

private val imageMemoryCache = mutableMapOf<String, ImageBitmap>()
private val imageHttpClient = HttpClient(sharedApiEngine) {
    expectSuccess = true
}

@Composable
fun NetworkImage(
    url: String,
    headers: Map<String, String> = emptyMap(),
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    onSuccess: () -> Unit = {},
) {
    val cacheKey = "$url|${headers.entries.sortedBy { it.key }}"
    val bitmap by produceState<ImageBitmap?>(imageMemoryCache[cacheKey], cacheKey) {
        if (value == null) {
            value = runCatching {
                val requestHeaders = headers
                imageHttpClient.get(url) {
                    requestHeaders.forEach { (name, value) -> header(name, value) }
                }.bodyAsBytes().decodeToImageBitmap()
            }
                .getOrNull()
                ?.also { imageMemoryCache[cacheKey] = it }
        }
    }
    bitmap?.let {
        LaunchedEffect(it) { onSuccess() }
        Image(it, contentDescription, modifier, contentScale = contentScale)
    }
}
