package ink.chyk.pass.features.courses

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/**
 * 日／周视图分别创建coursesPagerState
 * 负责执行ViewModel传来的导航请求，并向ViewModel反馈手势与滚动状态。
 * Pager的实际位置由UI持有，选中日期和翻页控制权由ViewModel协调。
 */
@Composable
internal fun rememberCoursesPagerState(
  viewModel: CoursesViewModel,
  termStartDate: LocalDate,
  daysPerPage: Int,
  isActive: Boolean,
): PagerState = key(viewModel, termStartDate, daysPerPage) {
  val date = viewModel.date.collectAsState()
  val pageCount = CoursesViewModel.TERM_DAY_COUNT / daysPerPage
  fun pageFor(date: LocalDate): Int =
    (termStartDate.daysUntil(date) / daysPerPage).coerceIn(0, pageCount - 1)

  // 新Pager从共享的选中日期建立初始位置，后续跳转由导航请求驱动。
  val pagerState = rememberPagerState(
    initialPage = pageFor(date.value),
    pageCount = { pageCount },
  )
  val navigation by viewModel.pagerNavigation.collectAsState()

  // 切换视图时，淡出的旧视图停止连接；进入的视图重置为空闲并对齐选中日期。
  LaunchedEffect(pagerState, isActive) {
    if (!isActive) return@LaunchedEffect
    viewModel.onPagerEntered()
    pagerState.scrollToPage(pageFor(viewModel.date.value))
    coroutineScope {
      launch {
        // 新请求取消旧动画并从当前位置接管；取消的动画不会走到完成回调。
        viewModel.pagerNavigation.map { it.request }.distinctUntilChanged().collectLatest { request ->
          if (request == null) return@collectLatest
          // 当前页码相同也可能尚有偏移，由Pager判断是否已经对齐目标页。
          pagerState.animateScrollToPage(pageFor(request.date))
          viewModel.onNavigationFinished(request)
        }
      }
      launch {
        pagerState.interactionSource.interactions.collect { interaction ->
          when (interaction) {
            is DragInteraction.Start -> viewModel.onPagerDragStarted()
            // 手指松开或手势取消后仍可能继续吸附，由滚动反馈报告最终结束。
            is DragInteraction.Stop, is DragInteraction.Cancel -> viewModel.onPagerDragStopped()
          }
        }
      }
      launch {
        // 如从周一拖向周二，松手前后可能都在滚动，目标页也都是周二。
        // 阶段变为SETTLING也参与观察，日期栏才能在松手后选中周二，而不用等页面停稳。
        // ViewModel只接受SETTLING阶段的反馈；完整操作过程见docs/courses/课表翻页状态梳理.md。
        snapshotFlow {
          Triple(pagerState.isScrollInProgress, pagerState.targetPage, navigation.phase)
        }.collect { (isScrollInProgress, targetPage, _) ->
          viewModel.onPagerScroll(isScrollInProgress, targetPage, daysPerPage)
        }
      }
    }
  }

  pagerState
}
