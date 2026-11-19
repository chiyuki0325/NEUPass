package ink.chyk.pass.features.courses

import kotlinx.datetime.LocalDate

/** 课表翻页的控制阶段，由ViewModel根据日期操作和Pager反馈更新。 */
internal enum class CoursePagerPhase {
  IDLE,       // 没有进行中的导航或手势
  NAVIGATING, // 执行日期格、翻周、“回到今天”等外部操作发起的导航
  DRAGGING,   // 手指拖动中，选中日期保持不变
  SETTLING,   // 松手后的fling或吸附，选中日期跟随目标页更新
}

/** 外部日期导航命令类 */
internal data class CourseDateRequest(
  val id: Long, // ViewModel内递增的请求编号
  val date: LocalDate,
)

/** 日／周视图共享的翻页控制状态
 * 日期操作和Pager手势的可用性由当前阶段推导 */
internal data class CoursePagerNavigation(
  val phase: CoursePagerPhase = CoursePagerPhase.IDLE,
  val request: CourseDateRequest? = null, // NAVIGATING阶段持有当前导航请求，其余阶段为null
) {
  // 外部导航期间允许继续选日期，新请求接管动画；手势拖动及吸附期间由Pager决定日期。
  val dateChangeEnabled: Boolean
    get() = phase == CoursePagerPhase.IDLE || phase == CoursePagerPhase.NAVIGATING

  // 外部导航期间锁住Pager手势，手势吸附期间仍可再次按住拖动。
  val userScrollEnabled: Boolean
    get() = phase != CoursePagerPhase.NAVIGATING
}
