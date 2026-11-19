package ink.chyk.pass

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.navigation3.runtime.*
import androidx.savedstate.serialization.*
import kotlinx.serialization.*
import kotlinx.serialization.modules.*

/**
 * 主窗口内的页面及其导航参数
 * 四个Tab的首页共用Home，导入课程、编辑课程、更多应用和设置作为各自Tab的子页面入栈
 */
@Serializable
sealed interface MainDestination : NavKey {
  @Serializable
  data class Home(val section: RootSection) : MainDestination

  /** autoStart表示进入页面后直接开始导入，用于尚未设置学期开始日期的首次导入。 */
  @Serializable
  data class ImportCourses(val autoStart: Boolean) : MainDestination

  @Serializable
  data class EditCourse(
    val courseKey: String? = null, // 已有课程的查询key；为空时新建课程
    val dayIndex: Int? = null, // 新建课程时预选的星期，0-6，周日为0
    val week: Int = 0, // 新建课程时预选的周次，从1开始；0表示不预选
  ) : MainDestination

  @Serializable
  data object MoreApps : MainDestination

  @Serializable
  data object ECardRecords : MainDestination

  @Serializable
  data object Settings : MainDestination

  @Serializable
  data object ThemeColors : MainDestination

  @Serializable
  data object OpenSourceLicenses : MainDestination
}

// 返回栈按NavKey保存页面类型和参数，Desktop和iOS的多态反序列化需要显式注册各个页面类型。
internal val mainNavigationConfiguration = SavedStateConfiguration {
  serializersModule = SerializersModule {
    polymorphic(NavKey::class) {
      subclass(MainDestination.Home::class)
      subclass(MainDestination.ImportCourses::class)
      subclass(MainDestination.EditCourse::class)
      subclass(MainDestination.MoreApps::class)
      subclass(MainDestination.ECardRecords::class)
      subclass(MainDestination.Settings::class)
      subclass(MainDestination.ThemeColors::class)
      subclass(MainDestination.OpenSourceLicenses::class)
    }
  }
}

/**
 * 四个Tab的选中状态和独立返回栈
 * 每个栈的第一项固定为该Tab首页，后续项是从这个Tab打开的子页面
 */
class MainNavigationState(
  selectedSection: MutableState<RootSection>,
  val backStacks: Map<RootSection, NavBackStack<NavKey>>,
) {
  var selected by selectedSection
    private set

  /**
   * 点击Tab时回到目标Tab首页，重复点击当前Tab也执行同样的返回逻辑
   * 如从“我的→设置”点“我的”，栈中只保留“我的”首页
   */
  fun select(section: RootSection) {
    val stack = backStacks.getValue(section)
    stack.subList(1, stack.size).clear()
    selected = section
  }

  /** 在当前Tab中打开子页面。 */
  fun navigate(destination: MainDestination) {
    backStacks.getValue(selected).add(destination)
  }

  /** 返回当前Tab的上一级页面；首页保留在栈底，主窗口退出由平台处理。 */
  fun back() {
    val stack = backStacks.getValue(selected)
    if (stack.size > 1) stack.removeAt(stack.lastIndex)
  }
}

/**
 * 保存和恢复选中的Tab及四个返回栈
 * initialSection用于首次进入主窗口，配置变更后继续使用恢复出的Tab和页面参数
 */
@Composable
fun rememberMainNavigationState(initialSection: RootSection): MainNavigationState {
  val selected = rememberSaveable { mutableStateOf(initialSection) }
  val courses = rememberNavBackStack(mainNavigationConfiguration, MainDestination.Home(RootSection.COURSES))
  val ecode = rememberNavBackStack(mainNavigationConfiguration, MainDestination.Home(RootSection.ECODE))
  val apps = rememberNavBackStack(mainNavigationConfiguration, MainDestination.Home(RootSection.APPS))
  val profile = rememberNavBackStack(mainNavigationConfiguration, MainDestination.Home(RootSection.PROFILE))
  val state = remember {
    MainNavigationState(
      selected,
      mapOf(
        RootSection.COURSES to courses,
        RootSection.ECODE to ecode,
        RootSection.APPS to apps,
        RootSection.PROFILE to profile,
      ),
    )
  }
  return state
}
