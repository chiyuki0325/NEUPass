package ink.chyk.pass.features.courses

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.tencent.tmm.kmmresource.compose.*
import ink.chyk.pass.components.BackgroundCard
import ink.chyk.pass.models.*
import ink.chyk.pass.resources.*
import ink.chyk.pass.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*

@Composable
fun ExamsView(
  viewModel: CoursesViewModel,
  dateState: StateFlow<LocalDate>,
  switchView: (CurrentCourseView) -> Unit,
  onImportCourses: () -> Unit,
) {
  // 我的考试主组件
  val date by dateState.collectAsState()
  val exams by viewModel.exams.collectAsState()

  Column(
    modifier = Modifier.fillMaxSize()
  ) {
    ExamTitle(switchView)
    ExamSubtitle(
      exams.done.size,
      exams.todo.size
    )
    Spacer(modifier = Modifier.height(24.dp))

    if (exams.size > 0) {
      BackgroundCard(
        modifier = Modifier.fillMaxWidth(),
      ) {
        ExamList(exams)
      }
    } else {
      NoCoursesSplash(date, stringResource(MR.strings.no_exams), onImportCourses)
    }
  }
}


@Composable
private fun ExamTitle(
  switchView: (CurrentCourseView) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      stringResource(MR.strings.my_exams_title),
      style = MaterialTheme.typography.headlineMedium,
    )
    Spacer(modifier = Modifier.weight(1f))
    IconButton(
      onClick = { switchView(CurrentCourseView.TODAY) }
    ) {
      Icon(
        painter = painterResource(MR.images.ic_fluent_calendar_today_24_regular),
        contentDescription = "日视图",
      )
    }
    IconButton(
      onClick = { switchView(CurrentCourseView.WEEK) }
    ) {
      Icon(
        painter = painterResource(MR.images.ic_fluent_calendar_month_24_regular),
        contentDescription = "周视图",
      )
    }
  }
}


@Composable
fun ExamList(
  exams: ExamState,
  dark: Boolean = isSystemInDarkTheme()
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp),
    // verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    if (exams.todo.isNotEmpty()) {
      // 有未考试科目
      item { ColumnSeparator(stringResource(MR.strings.todo_exams), dark) }
      items(exams.todo) {
        ExamItem(it, dark)
      }
    }
    if (exams.done.isNotEmpty()) {
      // 有已考试科目
      item { ColumnSeparator(stringResource(MR.strings.done_exams), dark) }
      items(exams.done) {
        ExamItem(it, dark)
      }
    }
  }
}

@Composable
private fun ExamItem(it: RenderedExam, dark: Boolean) {
  Box(
    modifier = Modifier
      .height(88.dp)
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxSize(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        // 考试开始和结束时间
        Column(
          modifier = Modifier
            .width(IntrinsicSize.Min)
            .height(68.dp)
        ) {
          Text(
            it.start.time.format(DateTimeUtils.timeMinutesFormat),
            fontFamily = fontFamilyResource(MR.fonts.roboto_numeric.roboto_numeric),
            fontWeight = FontWeight.Medium
          )
          Spacer(
            modifier = Modifier.weight(1f)
          )
          Text(
            it.end.time.format(DateTimeUtils.timeMinutesFormat),
            fontFamily = fontFamilyResource(MR.fonts.roboto_numeric.roboto_numeric),
            fontWeight = FontWeight.Medium
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        // 课程颜色
        Surface(
          modifier = Modifier
            .width(4.dp)
            .height(60.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp)),
          color = Color(ColorUtils.toForegroundColor(it.color, dark))
        ) {}
        Spacer(modifier = Modifier.width(8.dp))
        // 使用剩余权重，截断多余文本，显示省略号
        Column(modifier = Modifier.weight(1f)) {
          Row {
            Column(modifier = Modifier.weight(1f)) {
              // 课程名称
              Text(
                it.name,
                style = TextStyle(
                  fontWeight = FontWeight.Bold,
                  fontSize = 20.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              // 考试日期
              Text(
                buildAnnotatedString {
                  append(it.date.toString())
                  append(" ")
                  withStyle(SpanStyle(color = Color.Gray)) {
                    append(it.type)
                  }
                },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
              painter = painterResource(it.genre.iconResource),
              contentDescription = "Course Genre Icon",
            )
          }
          // 考试地点
          Text(
            buildAnnotatedString {
              // 地点
              append(it.location + " - ")

              // 座位号（加粗 + 特定字体）
              withStyle(
                style = SpanStyle(
                  fontWeight = FontWeight.Bold,
                )
              ) {
                append(it.seat)
              }

              append(" 号座")

              // 号座 + 备注
              if (it.remark != null) {
                withStyle(SpanStyle(color = Color.Gray)) {
                  append(" - " + it.remark)
                }
              }
            },
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}

@Composable
private fun ExamSubtitle(
  doneCount: Int,
  todoCount: Int
) {
  var isVisible by remember { mutableStateOf(false) }

  // 触发动画
  LaunchedEffect(Unit) {
    isVisible = true
  }

  AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
    exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start)
  ) {
    Text(
      text = stringResource(MR.strings.exams_subtitle, doneCount, todoCount),
      style = MaterialTheme.typography.bodyMedium,
      modifier = Modifier.animateContentSize() // 内容大小变化的动画
    )
  }
}
