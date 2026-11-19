package ink.chyk.pass.features.courses

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.ctrip.flight.mmkv.*
import com.tencent.tmm.kmmresource.compose.*
import ink.chyk.pass.components.*
import ink.chyk.pass.models.*
import ink.chyk.pass.resources.*
import ink.chyk.pass.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import kotlin.math.*

private const val DEFAULT_GRID_HEIGHT_PERCENT = 8
private const val MIN_GRID_HEIGHT_PERCENT = 4
private const val MAX_GRID_HEIGHT_PERCENT = 20

@Composable
fun WeeklyCoursesView(
  viewModel: CoursesViewModel,
  dateState: StateFlow<LocalDate>,
  switchView: (CurrentCourseView) -> Unit,
  isActive: Boolean,
  onEditCourse: (String) -> Unit,
  onImportCourses: () -> Unit,
  onCreateCourse: (dayIndex: Int, week: Int) -> Unit,
) {
  val gridHeightPercent = rememberGridHeightPercent()
  var showGridHeightDialog by remember { mutableStateOf(false) }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val gridHeight = maxHeight * (gridHeightPercent.value / 100f)
    val pageWidth = maxWidth

    Column(
      modifier = Modifier.fillMaxSize()
    ) {
      WeekTitle(viewModel, dateState, switchView) { showGridHeightDialog = true }
      Spacer(modifier = Modifier.height(8.dp))
      WeekCoursesGridOuter(
        viewModel = viewModel,
        onImportCourses = onImportCourses,
        onCreateCourse = onCreateCourse,
        dateState = dateState,
        isActive = isActive,
        widthDp = pageWidth,
        gridHeight = gridHeight,
      )
    }

    // 编辑行高对话框
    if (showGridHeightDialog) {
      EditGridHeightDialog(
        onDismissRequest = { showGridHeightDialog = false },
        gridHeightPercent = gridHeightPercent,
      )
    }
  }

  val selectedCourse by viewModel.selectedCourse.collectAsState()

  // 课程详情对话框
  selectedCourse?.let { course ->
    CourseDetailDialog(
      course,
      onDismissRequest = viewModel::hideCourseDetailDialog,
      onEdit = onEditCourse,
    )
  }
}

@Composable
private fun EditGridHeightDialog(
  onDismissRequest: () -> Unit,
  gridHeightPercent: MutableState<Int>,
) {
  var inputValue by remember { mutableStateOf(gridHeightPercent.value.toString()) }
  val newValue = inputValue.toIntOrNull()
  val isValid = newValue != null && newValue in MIN_GRID_HEIGHT_PERCENT..MAX_GRID_HEIGHT_PERCENT

  AlertDialog(
    onDismissRequest = onDismissRequest,
    title = { DialogTitle(MR.images.ic_fluent_resize_table_24_regular, "编辑单元格高度") },
    text = {
      OutlinedTextField(
        value = inputValue,
        onValueChange = { inputValue = it.filter { ch -> ch.isDigit() } },
        label = { Text("页面高度百分比") },
        suffix = { Text("%") },
        supportingText = {
          if (!isValid) {
            Text("请输入 $MIN_GRID_HEIGHT_PERCENT 至 $MAX_GRID_HEIGHT_PERCENT")
          }
        },
        isError = !isValid,
        singleLine = true
      )
    },
    confirmButton = {
      TextButton(
        onClick = {
          gridHeightPercent.value = newValue!!
          onDismissRequest()
        },
        enabled = isValid,
      ) {
        Text("确定")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismissRequest) {
        Text("取消")
      }
    }
  )
}

@Composable
private fun WeekTitle(
  viewModel: CoursesViewModel,
  dateState: StateFlow<LocalDate>,
  switchView: (CurrentCourseView) -> Unit,
  editGridHeight: () -> Unit
) {
  val date by dateState.collectAsState()
  val thisWeek = viewModel.thisWeek(date)
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      stringResource(MR.strings.current_week_overview, thisWeek),
      style = MaterialTheme.typography.headlineMedium,
      modifier = Modifier.weight(1f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      autoSize = TextAutoSize.StepBased(
        minFontSize = 14.sp,
        maxFontSize = MaterialTheme.typography.headlineMedium.fontSize,
      ),
    )

    IconButton(onClick = editGridHeight) {
      Icon(
        painter = painterResource(MR.images.ic_fluent_resize_table_24_regular),
        contentDescription = "编辑格子高度",
      )
    }

    IconButton(
      onClick = { switchView(CurrentCourseView.EXAMS) }
    ) {
      Icon(
        painter = painterResource(MR.images.ic_fluent_calligraphy_pen_24_regular),
        contentDescription = "考试视图",
      )
    }

    IconButton(
      onClick = { switchView(CurrentCourseView.TODAY) }
    ) {
      Icon(
        painter = painterResource(MR.images.ic_fluent_calendar_today_24_regular),
        contentDescription = "日视图",
      )
    }
  }
}

@Composable
private fun WeekCoursesGridOuter(
  viewModel: CoursesViewModel,
  onImportCourses: () -> Unit,
  onCreateCourse: (dayIndex: Int, week: Int) -> Unit,
  dateState: StateFlow<LocalDate>,
  isActive: Boolean,
  widthDp: Dp,
  gridHeight: Dp,
) {
  val date by dateState.collectAsState()
  val termStartDate = viewModel.termStartDate.collectAsState().value
  val termEndDate = viewModel.termEndDate.collectAsState().value
  val scrollState = rememberScrollState()

  if (termStartDate == null || termEndDate == null) {
    WeekCoursesGrid(
      viewModel = viewModel,
      onImportCourses = onImportCourses,
      onCreateCourse = onCreateCourse,
      scrollState = scrollState,
      date = date,
      widthDp = widthDp,
      gridHeight = gridHeight,
      showImportGuide = true,
    )
    return
  }

  val clampedDate = date.coerceIn(termStartDate, termEndDate)
  if (clampedDate != date) {
    WeekCoursesGrid(
      viewModel = viewModel,
      onImportCourses = onImportCourses,
      onCreateCourse = onCreateCourse,
      scrollState = scrollState,
      date = date,
      widthDp = widthDp,
      gridHeight = gridHeight,
      showImportGuide = true,
    )
    return
  }

  val selectedDayOffset = termStartDate.daysUntil(date) % 7
  val pagerState = rememberCoursesPagerState(
    viewModel = viewModel,
    termStartDate = termStartDate,
    daysPerPage = 7,
    isActive = isActive,
  )
  val navigation by viewModel.pagerNavigation.collectAsState()

  HorizontalPager(
    state = pagerState,
    modifier = Modifier
      .width(widthDp)
      .fillMaxHeight(),
    userScrollEnabled = isActive && navigation.userScrollEnabled,
    pageSpacing = 4.dp,
    beyondViewportPageCount = 1,
    verticalAlignment = Alignment.Top,
    flingBehavior = PagerDefaults.flingBehavior(
      state = pagerState,
      pagerSnapDistance = PagerSnapDistance.atMost(1),
    ),
    key = { page -> termStartDate.plus(page, DateTimeUnit.WEEK).toEpochDays() },
  ) { page ->
    WeekCoursesGrid(
      viewModel = viewModel,
      onImportCourses = onImportCourses,
      onCreateCourse = onCreateCourse,
      scrollState = scrollState,
      date = termStartDate
        .plus(page * 7 + selectedDayOffset, DateTimeUnit.DAY),
      widthDp = widthDp,
      gridHeight = gridHeight,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
private fun WeekCoursesGrid(
  viewModel: CoursesViewModel,
  onImportCourses: () -> Unit,
  onCreateCourse: (dayIndex: Int, week: Int) -> Unit,
  scrollState: ScrollState,
  date: LocalDate,
  widthDp: Dp,
  gridHeight: Dp,
  modifier: Modifier = Modifier,
  showImportGuide: Boolean = false,
) {
  // 本周课程表格
  val today by viewModel.today.collectAsState()

  val thisWeekDates = viewModel.thisWeekDates(date)
  val coursesByDate by viewModel.coursesByDate.collectAsState()
  val thisWeekCourses = if (showImportGuide) {
    List(7) { emptyList<RenderedCourse>() }
  } else {
    thisWeekDates.map { coursesByDate[it].orEmpty() }
  }
  val isDark = isSystemInDarkTheme()

  val interactionSource = remember { MutableInteractionSource() }

  Column(
    modifier = modifier.width(widthDp)
  ) {
    Row(
      modifier = Modifier.width(widthDp),
      horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      // 表头：几月几日\n星期几
      thisWeekDates.forEach {
        Column(
          modifier = Modifier.weight(1f),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = DateTimeUtils.displayName(it.dayOfWeek, short = true),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (it == date) FontWeight.Bold else FontWeight.Normal,
          )
          Text(
            text = "${it.month.number}/${it.day}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (it == date) FontWeight.Bold else FontWeight.Normal,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (showImportGuide || thisWeekCourses.all { it.isEmpty() }) {
      // 无课程
      if (today in thisWeekDates) {
        NoCoursesSplash(date, stringResource(MR.strings.no_course_this_week), onImportCourses)
      } else {
        NoCoursesSplash(date, stringResource(MR.strings.no_course_other_week), onImportCourses)
      }
      return
    }

    Row(
      modifier = Modifier
        .width(widthDp)
        .verticalScroll(scrollState),
      horizontalArrangement = Arrangement.spacedBy(2.dp),
      verticalAlignment = Alignment.Top
    ) {
      thisWeekCourses.forEachIndexed { index, _courses ->
        WeekCoursesColumn(
          viewModel, _courses, gridHeight, isDark,
          modifier = Modifier
            .weight(1f)
            .clickable(
              interactionSource = interactionSource,
              indication = null
            ) {
              onCreateCourse(index, viewModel.thisWeek(date) /* 1-52 */)
            }
        )
      }
    }
  }
}

@Composable
fun WeekCoursesColumn(
  viewModel: CoursesViewModel,
  courses: List<RenderedCourse>,
  gridHeight: Dp,
  isDark: Boolean,
  modifier: Modifier = Modifier,
) {
  val layout by remember(courses) { mutableStateOf(preprocessDayCourses(courses)) }

  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    var lastEnd = 0

    layout.groups.forEach { group ->
      if (group.startSection > lastEnd + 1) {
        Spacer(
          modifier = Modifier.height(
            gridHeight * (group.startSection - lastEnd - 1) +
                2.dp * (group.startSection - lastEnd - 2)
          )
        )
      }

      val groupHeight =
        gridHeight * (group.endSection - group.startSection + 1) +
            2.dp * (group.endSection - group.startSection)

      if (group.laneCount == 1) {
        CourseColumnItem(viewModel, group.items.first().course, gridHeight, isDark)
      } else {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(groupHeight),
          horizontalArrangement = Arrangement.spacedBy(2.dp),
          verticalAlignment = Alignment.Top
        ) {
          repeat(group.laneCount) { laneIndex ->
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              var laneLastEnd = group.startSection - 1
              group.items
                .filter { it.laneIndex == laneIndex }
                .forEach { laneItem ->
                  val course = laneItem.course
                  if (course.startSection > laneLastEnd + 1) {
                    Spacer(
                      modifier = Modifier.height(
                        gridHeight * (course.startSection - laneLastEnd - 1) +
                            2.dp * (course.startSection - laneLastEnd - 2)
                      )
                    )
                  }
                  CourseColumnItem(viewModel, course, gridHeight, isDark)
                  laneLastEnd = course.endSection
                }
            }
          }
        }
      }

      lastEnd = group.endSection
    }

    if (lastEnd < 12) {
      Spacer(modifier = Modifier.height(gridHeight * (12 - lastEnd) + 2.dp * (12 - lastEnd - 1)))
    }
  }
}

@Composable
fun CourseColumnItem(
  viewModel: CoursesViewModel,
  course: RenderedCourse,
  gridHeight: Dp,
  isDark: Boolean,
) {
  // 课程单元格
  Box(
    modifier = Modifier
      .height((gridHeight) * (course.endSection - course.startSection + 1) + (2.dp) * (course.endSection - course.startSection))
      .fillMaxWidth()
      .background(
        color = Color(ColorUtils.toBackgroundColor(course.color, isDark)),
        shape = RoundedCornerShape(4.dp)
      )
      .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
      .clickable { viewModel.showCourseDetailDialog(course.key) },
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(4.dp),
      verticalArrangement = Arrangement.Top,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = course.name,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        color = Color(ColorUtils.toForegroundColor(course.color, isDark)),
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = course.location,
        style = MaterialTheme.typography.bodySmall,
        color = Color(ColorUtils.toForegroundColor(course.color, isDark)),
      )
    }
  }
}

@Composable
fun rememberGridHeightPercent(): MutableState<Int> {
  val kv = defaultMMKV()
  val storedValue = kv.getInt("grid_height_percent", DEFAULT_GRID_HEIGHT_PERCENT)
    .coerceIn(MIN_GRID_HEIGHT_PERCENT, MAX_GRID_HEIGHT_PERCENT)
  val state = remember { mutableIntStateOf(storedValue) }

  // 同步写入
  LaunchedEffect(state.intValue) {
    kv["grid_height_percent"] = state.intValue
  }

  return state
}

private data class DayLayoutGroup(
  val startSection: Int,
  val endSection: Int,
  val laneCount: Int,
  val items: List<GroupCourseItem>,
)

private data class GroupCourseItem(
  val course: RenderedCourse,
  val laneIndex: Int,
)

private data class DayLayout(
  val groups: List<DayLayoutGroup>,
)

private fun preprocessDayCourses(courses: List<RenderedCourse>): DayLayout {
  if (courses.isEmpty()) return DayLayout(emptyList())

  val sorted = courses.sortedWith(
    compareBy<RenderedCourse> { it.startSection }
      .thenBy { it.endSection }
      .thenBy { it.name }
  )

  val groups = mutableListOf<DayLayoutGroup>()
  var current = mutableListOf<RenderedCourse>()
  var currentGroupEnd = -1

  sorted.forEach { course ->
    if (current.isEmpty()) {
      current.add(course)
      currentGroupEnd = course.endSection
    } else if (course.startSection <= currentGroupEnd) {
      current.add(course)
      currentGroupEnd = max(currentGroupEnd, course.endSection)
    } else {
      groups.add(buildLayoutGroup(current))
      current = mutableListOf(course)
      currentGroupEnd = course.endSection
    }
  }

  if (current.isNotEmpty()) {
    groups.add(buildLayoutGroup(current))
  }

  return DayLayout(groups)
}

private fun buildLayoutGroup(courses: List<RenderedCourse>): DayLayoutGroup {
  val sorted = courses.sortedWith(
    compareBy<RenderedCourse> { it.startSection }
      .thenBy { it.endSection }
      .thenBy { it.name }
  )

  val laneEnds = mutableListOf<Int>()
  val items = mutableListOf<GroupCourseItem>()

  sorted.forEach { course ->
    var laneIndex = -1
    for (i in laneEnds.indices) {
      if (course.startSection > laneEnds[i]) {
        laneIndex = i
        break
      }
    }

    if (laneIndex == -1) {
      laneIndex = laneEnds.size
      laneEnds.add(course.endSection)
    } else {
      laneEnds[laneIndex] = course.endSection
    }

    items.add(GroupCourseItem(course, laneIndex))
  }

  return DayLayoutGroup(
    startSection = sorted.minOf { it.startSection },
    endSection = sorted.maxOf { it.endSection },
    laneCount = laneEnds.size,
    items = items,
  )
}
