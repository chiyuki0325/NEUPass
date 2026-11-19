package ink.chyk.pass.features.courses

import com.ctrip.flight.mmkv.defaultMMKV
import com.ctrip.flight.mmkv.initialize
import ink.chyk.pass.utils.DateTimeUtils
import java.nio.file.Files
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.format
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoursesNavigationTest {
  companion object {
    init {
      initialize(Files.createTempDirectory("neupass-course-navigation-").toString())
    }
  }

  private val termStart = DateTimeUtils.currentLocalDate().minus(14, DateTimeUnit.DAY)
  private lateinit var viewModel: CoursesViewModel

  @BeforeTest
  fun setUp() {
    defaultMMKV()["course_v2_term_start"] = termStart.format(DateTimeUtils.basicDateFormat)
    viewModel = CoursesViewModel()
  }

  @Test
  fun latestClickOwnsNavigationUntilItsAnimationCompletes() {
    viewModel.onPagerEntered()
    viewModel.navigateTo(termStart.plus(20, DateTimeUnit.DAY))
    val first = assertNotNull(viewModel.pagerNavigation.value.request)
    viewModel.backToday()
    val latest = assertNotNull(viewModel.pagerNavigation.value.request)

    viewModel.onPagerScroll(false, 20, 1)
    viewModel.onNavigationFinished(first)
    assertEquals(viewModel.today.value, viewModel.date.value)
    assertEquals(latest, viewModel.pagerNavigation.value.request)
    assertTrue(viewModel.pagerNavigation.value.dateChangeEnabled)
    assertFalse(viewModel.pagerNavigation.value.userScrollEnabled)

    viewModel.navigateTo(viewModel.today.value)
    val repeated = assertNotNull(viewModel.pagerNavigation.value.request)
    assertNotEquals(latest.id, repeated.id)
    viewModel.onNavigationFinished(latest)
    assertEquals(repeated, viewModel.pagerNavigation.value.request)
    viewModel.onNavigationFinished(repeated)
    assertEquals(CoursePagerPhase.IDLE, viewModel.pagerNavigation.value.phase)
  }

  @Test
  fun gestureUpdatesSelectionWithoutIssuingAnotherNavigation() {
    viewModel.onPagerEntered()
    val initialDate = viewModel.date.value
    viewModel.onPagerDragStarted()
    viewModel.onPagerScroll(true, 15, 1)
    viewModel.navigateTo(termStart)
    assertEquals(initialDate, viewModel.date.value)
    assertFalse(viewModel.pagerNavigation.value.dateChangeEnabled)

    viewModel.onPagerDragStopped()
    viewModel.onPagerScroll(true, 15, 1)
    assertEquals(termStart.plus(15, DateTimeUnit.DAY), viewModel.date.value)
    assertNull(viewModel.pagerNavigation.value.request)
    assertFalse(viewModel.pagerNavigation.value.dateChangeEnabled)

    viewModel.onPagerDragStarted()
    viewModel.onPagerDragStopped()
    viewModel.onPagerScroll(false, 14, 1)
    assertEquals(initialDate, viewModel.date.value)
    assertEquals(CoursePagerPhase.IDLE, viewModel.pagerNavigation.value.phase)
  }

  @Test
  fun weekGesturePreservesTheSelectedDayWithinTheWeek() {
    viewModel.navigateTo(termStart.plus(16, DateTimeUnit.DAY))
    viewModel.onPagerEntered()
    viewModel.onPagerDragStarted()
    viewModel.onPagerDragStopped()
    viewModel.onPagerScroll(false, 3, 7)

    assertEquals(termStart.plus(23, DateTimeUnit.DAY), viewModel.date.value)
    assertNull(viewModel.pagerNavigation.value.request)
  }

  @Test
  fun enteringPagerResetsUnfinishedNavigationAndGesturesWithoutChangingSelection() {
    viewModel.onPagerEntered()
    viewModel.navigateTo(termStart.plus(20, DateTimeUnit.DAY))
    viewModel.onPagerEntered()
    assertEquals(CoursePagerPhase.IDLE, viewModel.pagerNavigation.value.phase)
    assertNull(viewModel.pagerNavigation.value.request)
    assertEquals(termStart.plus(20, DateTimeUnit.DAY), viewModel.date.value)

    viewModel.onPagerDragStarted()
    viewModel.onPagerEntered()
    assertTrue(viewModel.pagerNavigation.value.dateChangeEnabled)
    viewModel.onPagerDragStarted()
    viewModel.onPagerDragStopped()
    viewModel.onPagerEntered()
    assertEquals(CoursePagerPhase.IDLE, viewModel.pagerNavigation.value.phase)
    assertEquals(termStart.plus(20, DateTimeUnit.DAY), viewModel.date.value)
  }

  @Test
  fun dateOutsideTermDoesNotLeaveNavigationLocked() {
    viewModel.onPagerEntered()
    viewModel.navigateTo(termStart.plus(20, DateTimeUnit.DAY))
    val request = assertNotNull(viewModel.pagerNavigation.value.request)
    val outsideTerm = termStart.minus(1, DateTimeUnit.DAY)
    viewModel.navigateTo(outsideTerm)
    viewModel.onNavigationFinished(request)

    assertEquals(outsideTerm, viewModel.date.value)
    assertEquals(CoursePagerPhase.IDLE, viewModel.pagerNavigation.value.phase)
    viewModel.backToday()
    viewModel.onPagerEntered()
    assertEquals(viewModel.today.value, viewModel.date.value)
  }
}
