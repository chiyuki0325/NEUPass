package ink.chyk.pass.features.courses.importer

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.tencent.tmm.kmmresource.compose.*
import ink.chyk.pass.components.*
import ink.chyk.pass.resources.*
import ink.chyk.pass.services.courses.*
import ink.chyk.pass.ui.platformScaffoldContentWindowInsets
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ImportCoursesScreen(
  viewModel: ImportCoursesViewModel,
  autoStart: Boolean,
  onClose: () -> Unit,
  onImported: () -> Unit,
) {
  // 课程表导入窗口

  LaunchedEffect(autoStart) {
    if (autoStart) viewModel.runImport(onImported)
  }

  val importing by viewModel.importing.collectAsState()
  val importCompleted by viewModel.importCompleted.collectAsState()
  val messages by viewModel.messages.collectAsState()
  val hasErrors by viewModel.hasErrors.collectAsState()

  Scaffold(contentWindowInsets = platformScaffoldContentWindowInsets()) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(16.dp),
      contentAlignment = Alignment.Center
    ) {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
          .widthIn(max = 640.dp)
          .fillMaxWidth()
          .then(
            if (importing || importCompleted) Modifier.heightIn(max = 560.dp).fillMaxHeight(0.85f)
            else Modifier
          ),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          BackNavigationBar(
            onBack = onClose,
            title = stringResource(MR.strings.import_courses),
          )
          Spacer(modifier = Modifier.height(16.dp))
          if (importing || importCompleted) {
            ImportMessages(
              messages = messages,
              modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Box(
              modifier = Modifier.fillMaxWidth().height(48.dp),
              contentAlignment = Alignment.Center,
            ) {
              if (importing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
              } else {
                Button(
                  onClick = onClose,
                  modifier = Modifier.fillMaxSize(),
                ) {
                  Text(stringResource(if (hasErrors) MR.strings.cancel else MR.strings.confirm))
                }
              }
            }
          } else {
            ImportContent(
              onClick = { viewModel.runImport(onImported) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ImportContent(
  onClick: () -> Unit
) {
  // 导入课表前的说明语
  Text(
    stringResource(MR.strings.import_courses_content_1)
  )
  Text(
    buildAnnotatedString {
      append(stringResource(MR.strings.import_courses_content_2))
      withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
        append(stringResource(MR.strings.import_courses_content_3))
      }
      append(stringResource(MR.strings.import_courses_content_4))
    }
  )

  Spacer(modifier = Modifier.height(16.dp))
  Button(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth()
  ) {
    Text(stringResource(MR.strings.confirm_import_courses))
  }
}

@Composable
private fun ImportMessages(
  messages: List<CourseImportMessage>,
  modifier: Modifier = Modifier,
) {
  // 渐显滚动日志组件
  var visibleCount by remember { mutableIntStateOf(0) }
  val listState = rememberLazyListState()

  LaunchedEffect(messages) {
    while (visibleCount < messages.size) {
      visibleCount++
      delay(180.milliseconds)
    }
  }
  LaunchedEffect(visibleCount) {
    if (visibleCount > 0) listState.animateScrollToItem(0)
  }

  LazyColumn(
    state = listState,
    reverseLayout = true,
    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom),
    contentPadding = PaddingValues(vertical = 8.dp),
    modifier = modifier,
  ) {
    items(
      count = visibleCount,
      key = { index -> visibleCount - index - 1 },
    ) { index ->
      val messageIndex = visibleCount - index - 1
      val message = messages[messageIndex]
      val visible = remember {
        MutableTransitionState(false).apply { targetState = true }
      }
      AnimatedVisibility(
        visibleState = visible,
        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { height -> height },
        modifier = Modifier.animateItem(),
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Box(Modifier.width(8.dp)) {
            if (message.showBullet) {
              Text("•", color = MaterialTheme.colorScheme.primary)
            }
          }
          Text(
            text = message.text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
          )
        }
      }
    }
  }
}
