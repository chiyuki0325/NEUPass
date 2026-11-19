package ink.chyk.pass.features.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.unit.*
import ink.chyk.pass.*
import ink.chyk.pass.components.*
import ink.chyk.pass.features.others.RequestErrorScreen
import ink.chyk.pass.resources.MR
import ink.chyk.pass.repositories.CourseRepository
import ink.chyk.pass.repositories.CacheResult
import ink.chyk.pass.utils.AppLog
import kotlinx.coroutines.*
import com.tencent.tmm.kmmresource.compose.painterResource
import com.tencent.tmm.kmmresource.compose.stringResource
import com.tencent.tmm.kmmresource.resource.ImageResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
  viewModel: ProfileViewModel,
  innerPadding: PaddingValues,
  onLogout: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenLicenses: () -> Unit,
) {
  val scope = rememberCoroutineScope()
  val exportFailed = stringResource(MR.strings.export_failed)
  val cardBalanceResult by viewModel.cardBalance.collectAsState()
  val netBalanceResult by viewModel.netBalance.collectAsState()
  val cardBalance = cardBalanceResult?.value
  val netBalance = netBalanceResult?.value
  val netBalanceFloat by remember(netBalance) {
    derivedStateOf {
      netBalance?.valueString?.toFloatOrNull()
    }
  }

  val loadingState by viewModel.loadingState.collectAsState()

  val showLogoutDialog = remember { mutableStateOf(false) }
  val showAboutDialog = remember { mutableStateOf(false) }
  val showUploadAvatarDialog = remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    viewModel.refreshUserInfo()
  }

  if (loadingState == LoadingState.FAILED) {
    val networkAvailability = rememberNetworkAvailability()
    var observedOffline by remember { mutableStateOf(false) }
    var retriedAfterRecovery by remember { mutableStateOf(false) }

    LaunchedEffect(networkAvailability) {
      when (networkAvailability) {
        NetworkAvailability.Unavailable -> observedOffline = true
        NetworkAvailability.Available -> {
          if (observedOffline && !retriedAfterRecovery) {
            retriedAfterRecovery = true
            viewModel.refreshUserInfo()
          }
        }
        NetworkAvailability.Unknown -> Unit
      }
    }

    RequestErrorScreen(
      isConnected = networkAvailability == NetworkAvailability.Available,
      onRetry = viewModel::refreshUserInfo,
    )
    return
  }

  var isRefreshing by remember { mutableStateOf(false) }
  val loadComplete by remember { derivedStateOf { loadingState == LoadingState.SUCCESS } }

  PullToRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = {
      isRefreshing = true
      viewModel.refreshUserInfo {
        isRefreshing = false
      }
    },
    modifier = Modifier
      .fillMaxSize()
      .padding(innerPadding)
      .padding(top = 64.dp, start = 8.dp, end = 8.dp, bottom = 16.dp)
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
    ) {
      // 头像、名称、学号等信息
      item("header") {
        ProfileHeader(viewModel, onAvatarSelected = { showUploadAvatarDialog.value = true })
        Spacer(modifier = Modifier.height(64.dp))
      }

      // 功能列表

      // 校园卡
      item("campus_card") {
        RowButton(
          iconResource = MR.images.ic_fluent_contact_card_24_regular,
          text = stringResource(MR.strings.campus_card),
          content = if (cardBalance != null) "${stringResource(MR.strings.campus_card_balance)} ${cardBalance?.valueString} ${cardBalance?.unit}" else "",
          contentModifier = Modifier.shimmerContent(
            isLoading = cardBalanceResult is CacheResult.Cached,
            durationMillis = 1000,
            sweepWidth = 1.5f,
          ),
          clickable = false
        )
      }

      item("net_balance") {
        RowButton(
          iconResource = MR.images.ic_fluent_desktop_signal_24_regular,
          text = stringResource(MR.strings.network),
          content = if (netBalance != null) "${stringResource(MR.strings.network_balance)} ${
            netBalanceFloat?.let {
              // 保留两位小数
              if (it < 0) stringResource(MR.strings.lack_balance) else formatTwoDecimals(it.toDouble())
            }
          } ${netBalance?.unit}" else "",
          contentModifier = Modifier.shimmerContent(
            isLoading = netBalanceResult is CacheResult.Cached,
            durationMillis = 1000,
            sweepWidth = 1.5f,
          ),
          clickable = false
        )
      }

      item("recharge") {
        RowButton(
          iconResource = MR.images.ic_fluent_payment_24_regular,
          text = stringResource(MR.strings.recharge),
          clickable = true,
          enabled = loadComplete,
          onClick = { viewModel.recharge() }
        )
      }

      item("export_courses") {
        RowButton(
          iconResource = MR.images.ic_fluent_calendar_24_regular,
          text = stringResource(MR.strings.export_courses),
          // content = stringResource(R.string.export_courses_description),
          clickable = true,
          onClick = {
            scope.launch {
              try {
                platformExportCourses(CourseRepository.exportCalendarString())
              } catch (error: CancellationException) {
                throw error
              } catch (error: Exception) {
                AppLog.e("CourseCalendar", "导出课表失败：${error.message}")
                platformNotify(exportFailed)
              }
            }
          }
        )
      }

      item("logout") {
        RowButton(
          iconResource = MR.images.ic_fluent_person_swap_24_regular,
          text = stringResource(MR.strings.logout_account),
          clickable = true,
          onClick = { showLogoutDialog.value = true }
        )
      }

      item("settings") {
        RowButton(
          iconResource = MR.images.ic_fluent_settings_24_regular,
          text = stringResource(MR.strings.settings),
          clickable = true,
          onClick = {
            // 跳转到设置页面
            onOpenSettings()
          }
        )
      }

      item("about") {
        RowButton(
          iconResource = MR.images.ic_fluent_book_information_24_regular,
          text = stringResource(MR.strings.about),
          clickable = true,
          onClick = { showAboutDialog.value = true }
        )
      }
    }
  }

  LogoutConfirmationDialog(
    showDialog = showLogoutDialog,
    onConfirm = {
      viewModel.logout()
      onLogout()
    },
    onDismiss = { showLogoutDialog.value = false }
  )

  AboutDialog(
    showDialog = showAboutDialog,
    onDismiss = { showAboutDialog.value = false },
    onOpenLicenses = onOpenLicenses,
  )

  UploadAvatarConfirmDialog(
    showDialog = showUploadAvatarDialog,
    onDismiss = { showUploadAvatarDialog.value = false },
    onConfirm = {
      platformNotify("上传中…")
      showUploadAvatarDialog.value = false
      try {
        viewModel.uploadAvatar {
          platformNotify("上传完成，请等待审核通过。")
        }
      } catch (e: Exception) {
        platformNotify("上传失败。")
      }
    }
  )
}

@Composable
fun RowButton(
  iconResource: ImageResource,
  text: String,
  content: String? = null,
  // 如果为空字符串则使用骨架图
  clickable: Boolean = false,
  enabled: Boolean = true,
  contentModifier: Modifier = Modifier,
  onClick: () -> Unit = {}
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(48.dp)
      .padding(horizontal = 16.dp)
      .clip(RoundedCornerShape(8.dp))
      .then(if (clickable && enabled) Modifier.clickable(onClick = onClick) else Modifier),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Icon(
      painter = painterResource(iconResource),
      contentDescription = text
    )
    Spacer(modifier = Modifier.width(16.dp))
    Text(text)
    if (content != null) {
      Spacer(modifier = Modifier.width(8.dp))
      if (content.isEmpty()) {
        TextSkeleton(widthFraction = 0.4f)
      } else {
        Text(content, color = MaterialTheme.colorScheme.secondary, modifier = contentModifier)
      }
    }
    Spacer(modifier = Modifier.weight(1f))
    if (clickable) {
      Icon(
        painter = painterResource(MR.images.ic_fluent_chevron_right_20_filled),
        contentDescription = "前往",
        tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
      )
    } else {
      Box {}
    }
  }
}

private fun formatTwoDecimals(value: Double): String {
  val scaled = kotlin.math.round(value * 100).toLong()
  return "${scaled / 100}.${kotlin.math.abs(scaled % 100).toString().padStart(2, '0')}"
}
