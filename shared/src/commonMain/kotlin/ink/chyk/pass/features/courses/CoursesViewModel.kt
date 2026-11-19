package ink.chyk.pass.features.courses

import androidx.lifecycle.ViewModel
import ink.chyk.pass.repositories.CourseRepository as Repo
import ink.chyk.pass.models.Course
import ink.chyk.pass.models.RenderedCourse
import ink.chyk.pass.models.RenderedExam
import ink.chyk.pass.services.courses.renderExams
import ink.chyk.pass.utils.DateTimeUtils
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*

data class ExamState(
  val todo: List<RenderedExam>,
  val done: List<RenderedExam>,
) {
  val size: Int get() = todo.size + done.size
}

class CoursesViewModel : ViewModel() {
  companion object {
    const val TERM_WEEK_COUNT = 21
    const val TERM_DAY_COUNT = TERM_WEEK_COUNT * 7
  }

  // 今日日期
  private val _today = MutableStateFlow(DateTimeUtils.currentLocalDate())
  val today: StateFlow<LocalDate> = _today
  private val _date = MutableStateFlow(today.value)
  val date: StateFlow<LocalDate> = _date.asStateFlow()

  private val _pagerNavigation = MutableStateFlow(CoursePagerNavigation())
  internal val pagerNavigation = _pagerNavigation.asStateFlow()
  private var nextDateRequestId = 0L

  private var renderedExams: List<RenderedExam> = emptyList()
  private val _coursesByDate = MutableStateFlow<Map<LocalDate, List<RenderedCourse>>>(emptyMap())
  val coursesByDate = _coursesByDate.asStateFlow()
  private val _exams = MutableStateFlow(ExamState(emptyList(), emptyList()))
  val exams = _exams.asStateFlow()

  // 学期开始日期
  private val _termStartDate = MutableStateFlow<LocalDate?>(null)
  val termStartDate: StateFlow<LocalDate?> = _termStartDate

  // 学期结束日期
  private val _termEndDate = MutableStateFlow(calculateTermEndDate(_termStartDate.value))
  val termEndDate: StateFlow<LocalDate?> = _termEndDate

  // 当前弹出详情窗口的课程
  private val _selectedCourse = MutableStateFlow<Course?>(null)
  val selectedCourse: StateFlow<Course?> = _selectedCourse

  init {
    reloadCourses()
  }

  fun getCoursesByDate(date: LocalDate): List<RenderedCourse> =
    coursesByDate.value[date].orEmpty()

  // 获取「星期几」文案
  fun getWeekday(date: LocalDate, short: Boolean = false): String = DateTimeUtils.displayName(date.dayOfWeek, short)

  // 获取第几周
  fun thisWeek(date: LocalDate): Int {
    val startDate = checkNotNull(termStartDate.value) { "学期开始日期不存在" }
    val week = ((date.toEpochDays() - startDate.toEpochDays()) / 7 + 1).toInt()
    return if (week < 1) 1 else week
  }

  // 获取当周周日到周六的日期，用于周课表和底部日期栏
  fun thisWeekDates(date: LocalDate): List<LocalDate> {
    val weekStart = date.minus(date.dayOfWeek.isoDayNumber % 7, DateTimeUnit.DAY)
    return (0..6).map { weekStart.plus(it, DateTimeUnit.DAY) }
  }

  // 判断日期是否在学期范围内（1-21 周）
  fun isDateInTerm(date: LocalDate): Boolean {
    val termStart = termStartDate.value ?: return false
    val termEnd = termEndDate.value ?: return false
    return date in termStart..termEnd
  }

  fun previousWeekOrNull(date: LocalDate): LocalDate? =
    date.minus(1, DateTimeUnit.WEEK).takeIf(::isDateInTerm)

  fun nextWeekOrNull(date: LocalDate): LocalDate? =
    date.plus(1, DateTimeUnit.WEEK).takeIf(::isDateInTerm)

  fun navigateTo(date: LocalDate) {
    if (!_pagerNavigation.value.dateChangeEnabled) return
    requestDate(date)
  }

  fun backToday() = navigateTo(_today.value)

  internal fun onPagerEntered() {
    _pagerNavigation.value = CoursePagerNavigation()
  }

  internal fun onNavigationFinished(request: CourseDateRequest) {
    if (_pagerNavigation.value.request != request) return
    _pagerNavigation.value = CoursePagerNavigation()
  }

  internal fun onPagerDragStarted() {
    if (!_pagerNavigation.value.userScrollEnabled) return
    _pagerNavigation.value = CoursePagerNavigation(CoursePagerPhase.DRAGGING)
  }

  internal fun onPagerDragStopped() {
    if (_pagerNavigation.value.phase != CoursePagerPhase.DRAGGING) return
    _pagerNavigation.value = CoursePagerNavigation(CoursePagerPhase.SETTLING)
  }

  internal fun onPagerScroll(
    isScrollInProgress: Boolean,
    targetPage: Int,
    daysPerPage: Int,
  ) {
    // 拖动状态变化时调用
    if (_pagerNavigation.value.phase != CoursePagerPhase.SETTLING) return
    if (!isDateInTerm(_date.value)) return
    val termStart = _termStartDate.value ?: return
    val dayOffset = termStart.daysUntil(_date.value) % daysPerPage
    _date.value = termStart.plus(
      targetPage * daysPerPage + dayOffset,
      DateTimeUnit.DAY,
    )
    if (!isScrollInProgress) {
      _pagerNavigation.value = CoursePagerNavigation()
    }
  }

  private fun requestDate(date: LocalDate) {
    _date.value = date
    _pagerNavigation.value = if (isDateInTerm(date)) {
      CoursePagerNavigation(
        phase = CoursePagerPhase.NAVIGATING,
        request = CourseDateRequest(++nextDateRequestId, date),
      )
    } else {
      CoursePagerNavigation()
    }
  }

  // 是否为今天，用于控制跳回今天按钮展示
  fun isToday(date: LocalDate): Boolean = date == _today.value

  // 有些用户会把 app 长时间放在后台，因为我们 app 很小，也不会被系统自动杀
  // 故切屏后更新今天日期，避免跨天后仍显示昨天的课程
  fun updateTime() {
    val now = DateTimeUtils.currentLocalDateTime()
    if (_date.value == _today.value && now.date != _today.value) requestDate(now.date)
    _today.value = now.date
    val (todo, done) = renderedExams.partition { it.start > now }
    _exams.value = ExamState(todo, done)
  }

  /**
   * （重新）加载本地课表和考试安排。
   * 首次加载课表、导入课表成功、新增或修改课程保存成功、删除课程成功后调用。
   */
  fun reloadCourses() {
    val startDate = Repo.getTermStart()
    _termStartDate.value = startDate
    _termEndDate.value = calculateTermEndDate(startDate)
    _coursesByDate.value = Repo.getRenderedCourses()
    renderedExams = renderExams(Repo.getExams())
    updateTime()
  }

  // 点开课程后，显示课程详情弹窗
  fun showCourseDetailDialog(key: String) {
    val course = Repo.getCourseByKey(key) ?: return
    _selectedCourse.value = course
  }

  fun hideCourseDetailDialog() {
    _selectedCourse.value = null
  }

  private fun calculateTermEndDate(startDate: LocalDate?): LocalDate? =
    startDate
      ?.plus(TERM_DAY_COUNT, DateTimeUnit.DAY)
      ?.minus(1, DateTimeUnit.DAY)

}
