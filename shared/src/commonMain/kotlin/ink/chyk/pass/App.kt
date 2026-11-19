package ink.chyk.pass

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.*
import androidx.lifecycle.viewmodel.navigation3.*
import androidx.navigation3.runtime.*
import androidx.navigation3.scene.*
import androidx.navigation3.ui.*
import com.ctrip.flight.mmkv.*
import com.tencent.tmm.kmmresource.compose.*
import com.tencent.tmm.kmmresource.resource.*
import ink.chyk.pass.features.apps.*
import ink.chyk.pass.features.courses.*
import ink.chyk.pass.features.courses.editor.*
import ink.chyk.pass.features.courses.importer.*
import ink.chyk.pass.features.ecode.*
import ink.chyk.pass.features.login.*
import ink.chyk.pass.features.others.*
import ink.chyk.pass.features.profile.*
import ink.chyk.pass.features.settings.*
import ink.chyk.pass.repositories.*
import ink.chyk.pass.resources.*
import ink.chyk.pass.ui.*
import kotlinx.serialization.*

private const val ROOT_SECTION_METADATA_KEY = "ink.chyk.pass.rootSection"

@Serializable
enum class RootSection(val label: String, val route: String, val icon: ImageResource) {
  COURSES("课表", "courses", MR.images.ic_fluent_calendar_24_filled),
  ECODE("一码通", "ecode", MR.images.ic_fluent_qr_code_24_filled),
  APPS("应用", "apps", MR.images.ic_fluent_apps_24_filled),
  PROFILE("我的", "profile", MR.images.ic_fluent_person_24_filled),
}

fun rootSection(route: String?): RootSection =
  RootSection.entries.firstOrNull { it.route == route } ?: RootSection.ECODE

@Composable
fun App(
  window: AppWindow = AppWindow.Main(),
  navigator: AppWindowNavigator,
) {
  SettingsRepository.initializeLoggingSettings()

  // repo 层传过来「掉登录」时，直接跳登录
  val authStatus by LoginRepository.authStatus.collectAsState()
  LaunchedEffect(authStatus, window) {
    if (authStatus is LoginRepository.AuthStatus.LoginRequired && window != AppWindow.Login) {
      navigator.open(AppWindow.Login, AppWindowOpenMode.RESET)
    }
  }

  PlatformTheme {
    when (window) {
      AppWindow.Login -> LoginScreen(viewModel { LoginViewModel() }) {
        navigator.open(AppWindow.Main(), AppWindowOpenMode.RESET)
      }

      AppWindow.TermsOfService -> TermsOfServiceScreen {
        defaultMMKV()["tos"] = 400
        navigator.open(AppWindow.Main(), AppWindowOpenMode.RESET)
      }

      is AppWindow.Main -> MainWindow(
        window.initialSection,
        navigator,
        onOpenCampusRun = {
          navigator.open(AppWindow.CampusRun(it), AppWindowOpenMode.PUSH)
        },
      )

      is AppWindow.CampusRun -> CampusRunningScreen(window.url, navigator::close)
      AppWindow.Jwapp -> JwappScreen(viewModel { JwappViewModel() }, navigator::close)
      AppWindow.FakeECode -> FakeECode(viewModel { ECodeViewModel() }, navigator::close)
      is AppWindow.WebPage -> WebPageScreen(
        url = window.url,
        scripts = window.scripts,
        cookies = window.cookies,
        onBack = navigator::close,
      )
    }
  }
}

@Composable
private fun MainWindow(
  initialSection: RootSection,
  navigator: AppWindowNavigator,
  onOpenCampusRun: (String) -> Unit,
) {
  val mmkv = defaultMMKV()
  val hasAccount = LoginRepository.hasAccount()
  val acceptedTerms = mmkv.getInt("tos") >= 400
  if (!hasAccount || !acceptedTerms) {
    LaunchedEffect(hasAccount, acceptedTerms) {
      navigator.open(
        if (!hasAccount) AppWindow.Login else AppWindow.TermsOfService,
        AppWindowOpenMode.REPLACE,
      )
    }
    return
  }

  MainWindowContent(
    accountKey = mmkv.getString("student_id"),
    initialSection = initialSection,
    navigator = navigator,
    onOpenCampusRun = onOpenCampusRun,
  )
}

@Composable
@OptIn(ExperimentalSerializationApi::class)
private fun MainWindowContent(
  accountKey: String?,
  initialSection: RootSection,
  navigator: AppWindowNavigator,
  onOpenCampusRun: (String) -> Unit,
) {
  val eCodeViewModel = viewModel(key = "ecode:$accountKey") { ECodeViewModel() }
  val appsViewModel = viewModel(key = "apps:$accountKey") { AppsViewModel() }
  val profileViewModel = viewModel(key = "profile:$accountKey") { ProfileViewModel() }
  val coursesViewModel = viewModel(key = "courses:$accountKey") { CoursesViewModel() }
  key(accountKey) {
    val navigation = rememberMainNavigationState(initialSection)
    val selected = navigation.selected
    val isWideWidth = currentWindowAdaptiveInfo().windowSizeClass.isWideWidth
    Scaffold(
      contentWindowInsets = platformScaffoldContentWindowInsets(),
      bottomBar = {
        // 移动端使用底部导航栏
        if (!isWideWidth) {
          RootNavigationBar(selected, navigation::select)
        }
      },
    ) { innerPadding ->
      Row(Modifier.fillMaxSize()) {
        // 平板端使用侧边导航栏
        if (isWideWidth) {
          RootNavigationRail(selected, navigation::select)
        }
        val provider = entryProvider<NavKey> {
          entry<MainDestination.Home>(
            metadata = { mapOf(ROOT_SECTION_METADATA_KEY to it.section) },
          ) { destination ->
            when (destination.section) {
              RootSection.ECODE -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
              ) {
                ECodeScreen(eCodeViewModel) {
                  navigator.open(AppWindow.FakeECode, AppWindowOpenMode.PUSH)
                }
              }
              RootSection.APPS -> AppsScreen(
                appsViewModel, innerPadding, onOpenCampusRun,
                onOpenJwapp = { navigator.open(AppWindow.Jwapp, AppWindowOpenMode.PUSH) },
                onMoreApps = { navigation.navigate(MainDestination.MoreApps) },
                onOpenECard = { navigation.navigate(MainDestination.ECardRecords) },
              )
              RootSection.COURSES -> CoursesScreen(
                coursesViewModel, innerPadding,
                onImportCourses = {
                  navigation.navigate(MainDestination.ImportCourses(coursesViewModel.termStartDate.value == null))
                },
                onCreateCourse = { dayIndex, week ->
                  navigation.navigate(MainDestination.EditCourse(dayIndex = dayIndex, week = week))
                },
                onEditCourse = { courseKey ->
                  coursesViewModel.hideCourseDetailDialog()
                  navigation.navigate(MainDestination.EditCourse(courseKey = courseKey))
                },
              )
              RootSection.PROFILE -> ProfileScreen(
                profileViewModel,
                innerPadding,
                onLogout = { navigator.open(AppWindow.Login, AppWindowOpenMode.RESET) },
                onOpenSettings = { navigation.navigate(MainDestination.Settings) },
                onOpenLicenses = { navigation.navigate(MainDestination.OpenSourceLicenses) },
              )
            }
          }
          entry<MainDestination.MoreApps> {
            MoreAppsScreen(
              appsViewModel, innerPadding, onOpenCampusRun, navigation::back,
              onOpenJwapp = { navigator.open(AppWindow.Jwapp, AppWindowOpenMode.PUSH) },
              onOpenECard = { navigation.navigate(MainDestination.ECardRecords) },
            )
          }
          entry<MainDestination.ECardRecords> {
            val recordsViewModel = viewModel { ECardRecordsViewModel() }
            ECardRecordsScreen(recordsViewModel, innerPadding, navigation::back)
          }
          entry<MainDestination.Settings> {
            Box(Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding)) {
              SettingsScreen(
                onBack = navigation::back,
                onOpenThemeColors = { navigation.navigate(MainDestination.ThemeColors) },
                onOpenWebPage = { navigator.open(AppWindow.WebPage(it), AppWindowOpenMode.PUSH) },
              )
            }
          }
          entry<MainDestination.OpenSourceLicenses> {
            Box(Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding)) {
              OpenSourceLicensesScreen(onBack = navigation::back)
            }
          }
          entry<MainDestination.ThemeColors> {
            Box(Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding)) {
              ThemeColorScreen(onBack = navigation::back)
            }
          }
          entry<MainDestination.ImportCourses> { destination ->
            val importViewModel = viewModel { ImportCoursesViewModel() }
            Box(Modifier.fillMaxSize().padding(innerPadding)) {
              ImportCoursesScreen(
                importViewModel,
                autoStart = destination.autoStart,
                onClose = navigation::back,
                onImported = coursesViewModel::reloadCourses,
              )
            }
          }
          entry<MainDestination.EditCourse> { destination ->
            val editViewModel = viewModel {
              CourseEditViewModel(
                courseKeyArg = destination.courseKey,
                dayIndexArg = destination.dayIndex,
                initialWeeks = if (destination.week in 1..31) 1 shl (destination.week - 1) else 0,
              )
            }
            Box(Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
              CourseEditScreen(
                editViewModel,
                onBack = navigation::back,
                onResult = { result ->
                  if (result == CourseEditResult.SAVED || result == CourseEditResult.DELETED) {
                    coursesViewModel.reloadCourses()
                  }
                },
              )
            }
          }
        }
        val entries = navigation.backStacks.mapValues { (section, stack) ->
          key(section) {
            rememberDecoratedNavEntries(
              backStack = stack,
              entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
              ),
              entryProvider = provider,
            )
          }
        }
        NavDisplay(
          entries = entries.getValue(selected),
          modifier = Modifier.weight(1f).fillMaxHeight(),
          onBack = navigation::back,
          transitionSpec = { mainWindowTransition(forward = true, vertical = isWideWidth) },
          popTransitionSpec = { mainWindowTransition(forward = false, vertical = isWideWidth) },
          predictivePopTransitionSpec = { mainWindowTransition(forward = false, vertical = isWideWidth) },
        )
      }
    }
  }
}

private fun AnimatedContentTransitionScope<Scene<NavKey>>.mainWindowTransition(
  forward: Boolean,
  vertical: Boolean,
): ContentTransform {
  val initialSection = initialState.rootSection
  val targetSection = targetState.rootSection
  val slideForward = if (initialSection != targetSection) {
    targetSection.ordinal > initialSection.ordinal
  } else {
    forward
  }
  if (vertical) {
    return slideInVertically(
      initialOffsetY = { if (slideForward) it else -it },
      animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium),
    ) togetherWith slideOutVertically(
      targetOffsetY = { if (slideForward) -it else it },
      animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium),
    )
  }
  return slideInHorizontally(
    initialOffsetX = { if (slideForward) it else -it },
    animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium),
  ) togetherWith slideOutHorizontally(
    targetOffsetX = { if (slideForward) -it else it },
    animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium),
  )
}

private val Scene<NavKey>.rootSection: RootSection
  get() = (previousEntries.firstOrNull() ?: entries.first()).metadata.getValue(ROOT_SECTION_METADATA_KEY) as RootSection

@Composable
private fun RootNavigationBar(selected: RootSection, onSelect: (RootSection) -> Unit) {
  NavigationBar(windowInsets = platformNavigationBarWindowInsets()) {
    Row(Modifier.padding(horizontal = 8.dp)) {
      RootSection.entries.forEach { item ->
        NavigationBarItem(
          selected = item == selected,
          label = { Text(item.label) },
          icon = { Icon(painterResource(item.icon), item.label) },
          onClick = { onSelect(item) },
        )
      }
    }
  }
}

@Composable
private fun RootNavigationRail(selected: RootSection, onSelect: (RootSection) -> Unit) {
  NavigationRail(modifier = Modifier.padding(top = 8.dp)) {
    RootSection.entries.forEachIndexed { index, item ->
      NavigationRailItem(
        selected = item == selected,
        label = { Text(item.label) },
        icon = { Icon(painterResource(item.icon), item.label) },
        alwaysShowLabel = true,
        onClick = { onSelect(item) },
      )
      if (index != RootSection.entries.lastIndex) {
        Spacer(Modifier.weight(1f, fill = false).heightIn(max = 16.dp))
      }
    }
  }
}

@Composable
expect fun PlatformTheme(content: @Composable () -> Unit)
