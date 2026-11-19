package ink.chyk.pass.features.courses

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.adaptive.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.*
import ink.chyk.pass.ui.*
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.seconds

enum class CurrentCourseView { TODAY, WEEK, EXAMS }

@Composable
fun CoursesScreen(
  viewModel: CoursesViewModel,
  innerPadding: PaddingValues,
  onImportCourses: () -> Unit,
  onCreateCourse: (Int, Int) -> Unit,
  onEditCourse: (String) -> Unit,
) {
  var currentView by rememberSaveable {
    val today = viewModel.today.value
    mutableStateOf(
      if (viewModel.getCoursesByDate(today).isEmpty() && viewModel.exams.value.todo.any { it.date == today }) {
        CurrentCourseView.EXAMS
      } else {
        CurrentCourseView.TODAY
      }
    )
  }
  val switchView = { view: CurrentCourseView -> currentView = view }

  // 应用可能在后台停留到第二天，回到前台时立即更新“今天”和考试的待考、已考分组。
  // 前台停留期间每分钟更新一次；进入后台时取消循环，恢复前台后重新开始。
  val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
  LaunchedEffect(lifecycleOwner, viewModel) {
    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
      while (true) {
        viewModel.updateTime()
        delay(60.seconds)
      }
    }
  }

  // 未导入课表时显示导入引导
  val termStartDate by viewModel.termStartDate.collectAsState()
  if (termStartDate == null) {
    ImportCoursesGuide(onImportCourses = onImportCourses)
    return
  }

  // viewmodel 持有唯一一个日期状态
  val dateState = viewModel.date
  val isWideWidth = currentWindowAdaptiveInfo().windowSizeClass.isWideWidth
  Box(
    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(
      start = 16.dp, top = 16.dp, end = 16.dp, bottom = if (isWideWidth) 8.dp else 16.dp,
    ),
    contentAlignment = Alignment.Center,
  ) {
    Crossfade(
      targetState = currentView,
      animationSpec = tween(200),
      label = "CoursesScreen",
    ) { view ->
      when (view) {
        CurrentCourseView.WEEK -> WeeklyCoursesView(
          viewModel, dateState, switchView,
          isActive = view == currentView,
          onEditCourse = onEditCourse,
          onImportCourses = onImportCourses,
          onCreateCourse = onCreateCourse,
        )
        CurrentCourseView.TODAY -> DailyCoursesView(
          viewModel, dateState, switchView,
          isActive = view == currentView,
          onEditCourse = onEditCourse,
          onImportCourses = onImportCourses,
        )
        CurrentCourseView.EXAMS -> ExamsView(
          viewModel, dateState, switchView,
          onImportCourses = onImportCourses,
        )
      }
    }
  }
}
