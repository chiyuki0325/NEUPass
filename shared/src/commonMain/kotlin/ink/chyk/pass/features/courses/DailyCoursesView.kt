package ink.chyk.pass.features.courses

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import com.tencent.tmm.kmmresource.compose.*
import com.tencent.tmm.kmmresource.resource.*
import ink.chyk.pass.components.*
import ink.chyk.pass.models.*
import ink.chyk.pass.resources.*
import ink.chyk.pass.ui.*
import ink.chyk.pass.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import kotlin.math.*

@Composable
fun DailyCoursesView(
  viewModel: CoursesViewModel,
  dateState: StateFlow<LocalDate>,
  switchView: (CurrentCourseView) -> Unit,
  isActive: Boolean,
  onEditCourse: (String) -> Unit,
  onImportCourses: () -> Unit,
) {
  var showWeekJumpDialog by remember { mutableStateOf(false) }
  val navigation by viewModel.pagerNavigation.collectAsState()
  val userDateChangeEnabled = navigation.dateChangeEnabled
  val selectedCourse by viewModel.selectedCourse.collectAsState()

  Column(
    modifier = Modifier.fillMaxSize()
  ) {
    Column(
      modifier = Modifier.weight(1f)
    ) {
      // 日期和每日一言
      TodayTitle(viewModel, dateState, switchView)
      Spacer(modifier = Modifier.height(16.dp))

      // 课程卡片封装组件
      CoursesCardOuter(
        viewModel = viewModel,
        onImportCourses = onImportCourses,
        dateState = dateState,
        isActive = isActive,
      )
    }
    // 底部选择器
    Spacer(modifier = Modifier.height(12.dp))
    DaySelector(
      viewModel = viewModel,
      showJumpDialog = { showWeekJumpDialog = true },
      dateState = dateState,
      userDateChangeEnabled = isActive && userDateChangeEnabled,
    )
  }

  // 日期跳转对话框
  if (showWeekJumpDialog) {
    WeekJumpDialog(
      viewModel = viewModel,
      onDismissRequest = { showWeekJumpDialog = false },
      userDateChangeEnabled = isActive && userDateChangeEnabled,
    )
  }

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
fun CoursesCardOuter(
  viewModel: CoursesViewModel,
  onImportCourses: () -> Unit,
  dateState: StateFlow<LocalDate>,
  isActive: Boolean,
) {
  val date by dateState.collectAsState()
  val termStartDate = viewModel.termStartDate.collectAsState().value
  val termEndDate = viewModel.termEndDate.collectAsState().value

  if (termStartDate == null || termEndDate == null) {
    // 无法获取学期开始日，即用户未导入课表
    CoursesCard(
      viewModel = viewModel,
      onImportCourses = onImportCourses,
      date = date,
      modifier = Modifier.fillMaxSize(),
      showImportGuide = true,
    )
    return
  }

  val clampedDate = date.coerceIn(termStartDate, termEndDate)
  if (clampedDate != date) {
    // 用户日期超出第21周，跳导入
    CoursesCard(
      viewModel = viewModel,
      onImportCourses = onImportCourses,
      date = date,
      modifier = Modifier.fillMaxSize(),
      showImportGuide = true,
    )
    return
  }

  val pagerState = rememberCoursesPagerState(
    viewModel = viewModel,
    termStartDate = termStartDate,
    daysPerPage = 1,
    isActive = isActive,
  )
  val navigation by viewModel.pagerNavigation.collectAsState()

  HorizontalPager(
    state = pagerState,
    modifier = Modifier.fillMaxSize(),
    userScrollEnabled = isActive && navigation.userScrollEnabled,
    pageSpacing = 4.dp,
    beyondViewportPageCount = 1,
    verticalAlignment = Alignment.Top,
    flingBehavior = PagerDefaults.flingBehavior(
      state = pagerState,
      pagerSnapDistance = PagerSnapDistance.atMost(1),
    ),
    key = { page -> termStartDate.plus(page, DateTimeUnit.DAY).toEpochDays() },
  ) { page ->
    CoursesCard(
      viewModel = viewModel,
      onImportCourses = onImportCourses,
      date = termStartDate.plus(page, DateTimeUnit.DAY),
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
fun TodayTitle(
  viewModel: CoursesViewModel,
  dateState: StateFlow<LocalDate>,
  switchView: (CurrentCourseView) -> Unit
) {
  val date by dateState.collectAsState()
  val quote = Hitokoto.getQuote()
  val titleStyle = MaterialTheme.typography.headlineMedium
  val textMeasurer = rememberTextMeasurer()
  val fullTitle = stringResource(
    MR.strings.date,
    date.month.number,
    date.day,
    viewModel.getWeekday(date),
  )
  val shortTitle = stringResource(
    MR.strings.date,
    date.month.number,
    date.day,
    viewModel.getWeekday(date, true)
  )

  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    BoxWithConstraints(modifier = Modifier.weight(1f)) {
      val fullTitleWidth = textMeasurer.measure(
        text = fullTitle,
        style = titleStyle,
        softWrap = false,
        maxLines = 1,
      ).size.width
      Text(
        text = if (fullTitleWidth <= constraints.maxWidth) fullTitle else shortTitle,
        style = titleStyle,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(
          minFontSize = 14.sp,
          maxFontSize = titleStyle.fontSize,
        ),
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
      onClick = { switchView(CurrentCourseView.WEEK) }
    ) {
      Icon(
        painter = painterResource(MR.images.ic_fluent_calendar_month_24_regular),
        contentDescription = "周视图",
      )
    }
  }

  Spacer(modifier = Modifier.height(8.dp))
  quote?.let { FoldInTextAnimation(it) }
}

@Composable
fun CoursesCard(
  viewModel: CoursesViewModel,
  onImportCourses: () -> Unit,
  date: LocalDate,
  modifier: Modifier = Modifier,
  showImportGuide: Boolean = false,
) {
  // 课表卡片
  val today by viewModel.today.collectAsState()
  val coursesByDate by viewModel.coursesByDate.collectAsState()
  val todayCourses = if (showImportGuide) emptyList() else coursesByDate[date].orEmpty()

  BackgroundCard(
    modifier = modifier
      .fillMaxWidth()
      .padding(0.dp),
  ) {
    Column(
      modifier = Modifier
        .padding(16.dp)
        .fillMaxWidth()
    ) {
      if (todayCourses.isEmpty()) {
        if (date == today) {
          // 今天没有课
          NoCoursesSplash(date, stringResource(MR.strings.no_course_today), onImportCourses)
        } else {
          NoCoursesSplash(date, stringResource(MR.strings.no_course_other_day), onImportCourses)
        }
      } else {
        TodayCoursesList(viewModel, todayCourses)
      }
    }
  }
}


@Composable
fun FoldInTextAnimation(quote: Pair<String, String>) {
  var isVisible by remember { mutableStateOf(false) }

  // 触发动画
  LaunchedEffect(Unit) {
    isVisible = true
  }

  Column(
    modifier = Modifier.fillMaxWidth()
  ) {
    AnimatedVisibility(
      visible = isVisible,
      enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start), // 从左往右展开
      exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start) // 从右往左折叠
    ) {
      Text(
        text = quote.first,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.animateContentSize() // 内容大小变化的动画
      )
    }

    AnimatedVisibility(
      visible = isVisible,
      enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
      exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start)
    ) {
      Text(
        text = "—— ${quote.second}",
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray,
        modifier = Modifier.animateContentSize()
      )
    }
  }
}

@Composable
fun TodayCoursesList(
  viewModel: CoursesViewModel,
  todayCourses: List<RenderedCourse>,
  dark: Boolean = isSystemInDarkTheme()
) {
  var previousPeriod: CoursePeriod? = null  // 用于判断是否需要显示早午晚标题

  var lastEnd = -1

  LazyColumn(
    modifier = Modifier.fillMaxWidth()
  ) {
    todayCourses.forEach {
      if (it.period != previousPeriod) {
        // 课程区间变化了，显示早午晚标题
        item {
          ColumnSeparator(
            when (it.period) {
              CoursePeriod.MORNING -> stringResource(MR.strings.morning_courses)
              CoursePeriod.AFTERNOON -> stringResource(MR.strings.afternoon_courses)
              CoursePeriod.EVENING -> stringResource(MR.strings.evening_courses)
            },
            dark
          )
        }
        previousPeriod = it.period
      }

      val isOverlapped = it.startSection <= lastEnd

      item {
        CourseItem(
          it,
          modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .clickable { viewModel.showCourseDetailDialog(it.key) },
          isOverlapped
        )
        Spacer(modifier = Modifier.height(8.dp))
      }

      lastEnd = it.endSection
    }
  }
}

@Composable
fun CourseItem(
  it: RenderedCourse,
  modifier: Modifier = Modifier,
  isOverlapped: Boolean = false,
) {
  val isDark = isSystemInDarkTheme()

  Box(
    modifier = modifier
      .height(64.dp)
      .fillMaxWidth()
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
        // 课程开始和结束时间
        Column(modifier = Modifier.width(IntrinsicSize.Min)) {
          Text(
            it.start,
            fontFamily = fontFamilyResource(MR.fonts.roboto_numeric.roboto_numeric),
            fontWeight = FontWeight.Medium,
            color = if (isOverlapped) Color.Red else Color.Unspecified
          )
          Text(
            it.end,
            fontFamily = fontFamilyResource(MR.fonts.roboto_numeric.roboto_numeric),
            fontWeight = FontWeight.Medium,
            color = if (isOverlapped) Color.Red else Color.Unspecified
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        // 课程颜色
        Surface(
          modifier = Modifier
            .width(4.dp)
            .height(40.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp)),
          // color = Color(HashColorHelper.calcColor(it.name))
          color = Color(ColorUtils.toBlockColor(it.color))
        ) {}
        Spacer(modifier = Modifier.width(8.dp))
        // 使用剩余权重，截断多余文本，显示省略号
        Column(modifier = Modifier.weight(1f)) {
          // 课程名称
          Text(
            (if (isOverlapped) stringResource(MR.strings.course_overlapped) + " " else "") + it.name,
            style = TextStyle(
              fontWeight = if (isOverlapped) FontWeight.ExtraBold else FontWeight.Bold,
              fontSize = 20.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          // 课程地点，教师
          Text(
            buildItemRowAnnotated(it.location, it.teacher, it.campus),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
      Spacer(modifier = Modifier.width(8.dp))
      Icon(
        painter = painterResource(it.genre.iconResource),
        contentDescription = "Course Genre Icon",
      )
    }
  }
}

@Composable
fun DaySelector(
  viewModel: CoursesViewModel,
  showJumpDialog: () -> Unit,
  dateState: StateFlow<LocalDate>,
  userDateChangeEnabled: Boolean,
  dark: Boolean = isSystemInDarkTheme(),
) {
  val isWideWidth = currentWindowAdaptiveInfo().windowSizeClass.isWideWidth
  val date by dateState.collectAsState()
  val coursesByDate by viewModel.coursesByDate.collectAsState()
  val thisWeekDates = viewModel.thisWeekDates(date)
  val selectedIndex = thisWeekDates.indexOf(date)
  val previousWeek = viewModel.previousWeekOrNull(date)
  val nextWeek = viewModel.nextWeekOrNull(date)

  val density = LocalDensity.current
  val interactionSource = remember { MutableInteractionSource() }

  Column(
    modifier = Modifier
      .fillMaxWidth()
  ) {
    BoxWithConstraints(
      modifier = Modifier.fillMaxWidth()
    ) {
      val fullWidth = constraints.maxWidth
      val itemSizePx = fullWidth / 7f
      val itemSizeDp = with(density) { itemSizePx.toDp() }
      // 只限制日期行的高度，宽屏下保留下方的翻周导航
      val itemHeight = if (isWideWidth) minOf(itemSizeDp, 48.dp) else itemSizeDp

      // 高亮层偏移量（动画驱动）
      val highlightOffset by animateFloatAsState(
        targetValue = selectedIndex * itemSizePx,
        animationSpec = tween(durationMillis = 200),
        label = "DaySelectorHighlightOffset"
      )

      // 背景高亮块
      Box(
        modifier = Modifier
          .size(width = itemSizeDp, height = itemHeight)
          .offset { IntOffset(highlightOffset.roundToInt(), 0) }
          .padding(2.dp)
          .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
          .background(if (dark) Color.DarkGray else Color.LightGray)
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        thisWeekDates.forEach { thatDate ->
          val courseCount = coursesByDate[thatDate].orEmpty().size

          Box(
            modifier = Modifier
              .weight(1f)
              .height(itemHeight)
              .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
              .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = userDateChangeEnabled,
              ) {
                viewModel.navigateTo(thatDate)
              },
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                thatDate.day.toString(),
                style = MaterialTheme.typography.bodyMedium,
              )
              Text(
                "${
                  DateTimeUtils.displayName(thatDate.dayOfWeek, short = true)
                } $courseCount",
                style = TextStyle(fontSize = 8.sp)
              )
            }
          }
        }
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      PrevNextButton(
        icon = MR.images.ic_fluent_chevron_left_20_filled,
        enabled = userDateChangeEnabled && previousWeek != null,
        onClick = { previousWeek?.let(viewModel::navigateTo) }
      )
      val thisWeekNum = viewModel.thisWeek(date)
      Text(
        stringResource(
          if (viewModel.isToday(date)) MR.strings.current_week
          else MR.strings.current_week_navigated,
          thisWeekNum,
        ),

        modifier = Modifier.clickable(enabled = userDateChangeEnabled) {
          showJumpDialog()
        }
      )

      if (!viewModel.isToday(date)) {
        Text(
          modifier = Modifier.clickable(enabled = userDateChangeEnabled) {
            viewModel.backToday()
          },
          text = stringResource(MR.strings.week_jump_today),
          color = MaterialTheme.colorScheme.secondary,
          fontWeight = FontWeight.Bold
        )
      }

      PrevNextButton(
        icon = MR.images.ic_fluent_chevron_right_20_filled,
        enabled = userDateChangeEnabled && nextWeek != null,
        onClick = { nextWeek?.let(viewModel::navigateTo) }
      )
    }
  }
}

@Composable
fun PrevNextButton(
  icon: ImageResource,
  enabled: Boolean,
  onClick: () -> Unit
) {
  IconButton(
    enabled = enabled,
    colors = IconButtonDefaults.iconButtonColors(
      disabledContentColor = LocalContentColor.current,
    ),
    onClick = onClick
  ) {
    Icon(
      painter = painterResource(icon),
      contentDescription = "Prev/Next Button",
    )
  }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekJumpDialog(
  viewModel: CoursesViewModel,
  onDismissRequest: () -> Unit,
  userDateChangeEnabled: Boolean,
) {
  val datePickerState = rememberDatePickerState(
    selectableDates = object : SelectableDates {
      override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val selectedDate = DateTimeUtils.localDateFromUtcMilliseconds(utcTimeMillis)
        return viewModel.isDateInTerm(selectedDate)
      }
    }
  )
  val selectedDate = datePickerState.selectedDateMillis?.let {
    DateTimeUtils.localDateFromUtcMilliseconds(it)
  }

  // 切换至第几周的对话框
  Dialog(
    properties = DialogProperties(usePlatformDefaultWidth = false),
    onDismissRequest = onDismissRequest
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth(),
      shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    ) {
      Column(
        modifier = Modifier
          .padding(16.dp)
          .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
      ) {
        DialogTitle(
          MR.images.ic_fluent_calendar_24_regular,
          stringResource(MR.strings.week_jump)
        )

        Spacer(modifier = Modifier.height(8.dp))

        DatePicker(
          state = datePickerState,
          showModeToggle = false,
          modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
        )


        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(
            enabled = userDateChangeEnabled,
            onClick = {
              viewModel.backToday()
              onDismissRequest()
            }
          ) {
            Text(stringResource(MR.strings.week_jump_today))
          }
          Spacer(modifier = Modifier.width(8.dp))
          TextButton(
            onClick = {
              onDismissRequest()
            }
          ) {
            Text(stringResource(MR.strings.cancel))
          }
          Spacer(modifier = Modifier.width(8.dp))
          TextButton(
            enabled = userDateChangeEnabled,
            onClick = {
              if (selectedDate != null && viewModel.isDateInTerm(selectedDate)) {
                viewModel.navigateTo(selectedDate)
                onDismissRequest()
              } else {
                onDismissRequest()
              }
            }
          ) {
            Text(stringResource(MR.strings.confirm))
          }
        }
      }
    }
  }
}

fun buildItemRowAnnotated(location: String, teacher: String, campus: String?): AnnotatedString {
  return buildAnnotatedString {
    withStyle(style = SpanStyle(fontSize = 14.sp)) {
      withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
        append(location)
      }
      if (teacher.isNotEmpty()) {
        append(" - ")
        append(teacher)
      }
    }
    if (campus != null) {
      withStyle(
        style = SpanStyle(
          fontSize = 14.sp,
          color = Color.Gray
        )
      ) {
        append("（")
        append(campus)
        append("）")
      }
    }
  }
}
