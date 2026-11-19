package ink.chyk.pass.features.login

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import com.tencent.tmm.kmmresource.compose.painterResource
import com.tencent.tmm.kmmresource.compose.stringResource
import ink.chyk.pass.resources.MR
import ink.chyk.pass.ui.platformScaffoldContentWindowInsets
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.seconds

@Composable
fun TermsOfServiceScreen(onAccept: () -> Unit = {}) {
  val scrollState = rememberScrollState()
  var countdown by remember { mutableIntStateOf(10) }
  var timer by remember { mutableStateOf(false) }
  var isButtonEnabled by remember { mutableStateOf(false) }
  var check1 by remember { mutableStateOf(false) }
  var check2 by remember { mutableStateOf(false) }
  var check3 by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    while (countdown > 0) { delay(1.seconds); countdown-- }
    timer = true
  }
  LaunchedEffect(scrollState.canScrollForward) {
    if (!scrollState.canScrollForward) isButtonEnabled = true
  }

  Scaffold(contentWindowInsets = platformScaffoldContentWindowInsets()) { innerPadding ->
    Box(Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
      Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(painterResource(MR.images.ic_fluent_book_information_24_regular), "Back")
          Text(stringResource(MR.strings.tos_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 8.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(MR.strings.tos_read_carefully))
        Spacer(Modifier.height(16.dp))
        Box(
          Modifier.fillMaxWidth().weight(1f).clipToBounds()
            .border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp)),
        ) {
          TextField(
            value = stringResource(MR.strings.tos_v400), onValueChange = {}, readOnly = true,
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
          )
        }
        Spacer(Modifier.height(16.dp))
        Check(check1, { check1 = it }, stringResource(MR.strings.tos_v220_checkbox_1))
        Check(check2, { check2 = it }, stringResource(MR.strings.tos_v220_checkbox_2))
        Check(check3, { check3 = it }, stringResource(MR.strings.tos_v220_checkbox_3))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          Button(onClick = onAccept, enabled = isButtonEnabled && timer && check1 && check2 && check3) {
            Text(if (timer) stringResource(MR.strings.tos_confirm) else "${countdown}s")
          }
        }
      }
    }
  }
}

@Composable
private fun Check(checked: Boolean, onCheckedChange: (Boolean) -> Unit, text: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    Text(text)
  }
}
