package ink.chyk.pass.features.ecode

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tencent.tmm.kmmresource.compose.painterResource
import com.tencent.tmm.kmmresource.compose.stringResource
import ink.chyk.pass.repositories.ECodeWebSession
import ink.chyk.pass.resources.MR
import ink.chyk.pass.web.CustomWebView
import ink.chyk.pass.web.rememberWebViewState
import kotlinx.coroutines.*

private val FAKE_ECODE_SCRIPT = """
    (() => {
        const source = 'https://imgsrc.chyk.ink/9WSRzEKlCmLsqOqG.webp';
        const replaceImage = () => {
            const image = document.querySelector('img.img1');
            if (image && image.src !== source) image.src = source;
        };
        replaceImage();
        if (!window.neupassECodeObserver) {
            window.neupassECodeObserver = new MutationObserver(replaceImage);
            window.neupassECodeObserver.observe(document.documentElement, {
                childList: true,
                subtree: true,
                attributes: true,
                attributeFilter: ['src']
            });
        }
    })()
""".trimIndent()

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FakeECode(viewModel: ECodeViewModel, onBack: () -> Unit) {
    PlatformECodeEffects(viewModel.shouldRaiseBrightness())
    var attempt by remember { mutableIntStateOf(0) }
    val session by produceState<Result<ECodeWebSession>?>(null, viewModel, attempt) {
        value = null
        value = try {
            Result.success(viewModel.getWebSession())
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
    val webViewState = rememberWebViewState()

    BackHandler(onBack = onBack)

    Column(Modifier.fillMaxSize().background(Color(54, 83, 163)).safeDrawingPadding()) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("e码通", color = Color.White, fontSize = 16.sp)
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart).size(48.dp),
            ) {
                Icon(
                    painterResource(MR.images.ic_fluent_chevron_left_20_filled),
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val result = session
            when {
                result == null -> CircularProgressIndicator(color = Color.White)
                result.isFailure -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(MR.strings.request_failed_content), color = Color.White)
                    TextButton(onClick = { attempt++ }) {
                        Text(stringResource(MR.strings.retry), color = Color.White)
                    }
                }
                else -> CustomWebView(
                    url = result.getOrThrow().url,
                    cookies = result.getOrThrow().cookies,
                    state = webViewState,
                    isDarkMode = false,
                    finish = onBack,
                    extraScripts = FAKE_ECODE_SCRIPT,
                )
            }
        }
    }
}
