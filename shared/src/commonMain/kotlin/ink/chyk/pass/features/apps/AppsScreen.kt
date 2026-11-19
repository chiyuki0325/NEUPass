package ink.chyk.pass.features.apps

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import ink.chyk.pass.components.*
import ink.chyk.pass.resources.MR
import com.tencent.tmm.kmmresource.compose.painterResource
import com.tencent.tmm.kmmresource.compose.stringResource
import com.tencent.tmm.kmmresource.resource.ImageResource
import kotlinx.coroutines.*
import kotlinx.serialization.*
import sh.calvin.reorderable.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@ExperimentalSerializationApi
@Composable
fun AppsScreen(
  viewModel: AppsViewModel,
  innerPadding: PaddingValues,
  onOpenCampusRun: (String) -> Unit,
  onOpenJwapp: () -> Unit,
  onMoreApps: () -> Unit,
  onOpenECard: () -> Unit,
) {
  val appCardsOrder by viewModel.appCardsOrder.collectAsState()
  var showSortAppsDialog by remember { mutableStateOf(false) }
  var isRefreshing by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  PullToRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = {
      isRefreshing = true
      scope.launch {
        val backgroundJobs = mutableListOf<Deferred<Any>>()
        if (appCardsOrder.contains("campus_run" to true)) {
          viewModel.campusRun.refreshCampusRun()
        }
        if (appCardsOrder.contains("ip_gateway" to true)) {
          backgroundJobs.add(async { viewModel.ipgw.refreshIpGateway() })
        }
        if (appCardsOrder.contains("jwapp" to true)) {
          viewModel.jwapp.refreshJwapp()
        }
        if (appCardsOrder.contains("ecard" to true)) {
          viewModel.ecard.refresh()
        }
        isRefreshing = false
        backgroundJobs.awaitAll()
      }
    },
    modifier = Modifier
      .fillMaxSize()
      .padding(innerPadding)
      .padding(start = 16.dp, top = 16.dp, end = 16.dp),
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxHeight()
    ) {
      item("header") {
        AppsHeader { showSortAppsDialog = true }
      }
      appCardsOrder.forEach {
        if (it.second) {
          item(key = it.first) {
            Spacer(modifier = Modifier.height(16.dp))
            AppCardImpl(it.first, viewModel, Modifier.animateItem(), onOpenCampusRun, onOpenJwapp, onOpenECard)
          }
        }
      }

      item("footer") {
        Spacer(modifier = Modifier.height(4.dp))
        TextButton(
          onClick = onMoreApps,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(stringResource(MR.strings.more))
        }
      }
    }
  }

  if (showSortAppsDialog) {
    SortAppsDialog(viewModel, onDismiss = { showSortAppsDialog = false })
  }
}

@ExperimentalSerializationApi
@Composable
fun AppCardImpl(
  key: String,
  viewModel: AppsViewModel,
  modifier: Modifier,
  onOpenCampusRun: (String) -> Unit,
  onOpenJwapp: () -> Unit,
  onOpenECard: () -> Unit,
) {
  Box(modifier) {
    when (key) {
      "campus_run" -> CampusRunCard(viewModel.campusRun, onOpenCampusRun)
      "jwapp" -> JwappCard(viewModel.jwapp, onOpenJwapp)
      "ecard" -> ECardCard(viewModel.ecard, onOpenECard)
      // "mail_box" -> MailBoxCard(viewModel)
      "ip_gateway" -> IpGatewayCard(viewModel.ipgw)
    }
  }
}


@Composable
fun AppsHeader(
  showSortAppsDialog: () -> Unit
) {
  // 顶部间距和标题
  Spacer(modifier = Modifier.height(32.dp))
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = stringResource(MR.strings.apps_center),
      style = MaterialTheme.typography.headlineLarge,
    )
    Spacer(modifier = Modifier.weight(1f))
    IconButton(
      onClick = showSortAppsDialog,
    ) {
      Icon(
        painter = painterResource(MR.images.ic_fluent_arrow_sort_24_regular),
        contentDescription = null,
      )
    }
  }
  Spacer(modifier = Modifier.height(16.dp))
}


@ExperimentalSerializationApi
@Composable
fun SortAppsDialog(
  viewModel: AppsViewModel,
  onDismiss: () -> Unit
) {
  // github.com/Calvin-LL/Reorderable

  @Composable
  fun appName(appId: String): String {
    return when (appId) {
      "campus_run" -> stringResource(MR.strings.campus_run)
      "jwapp" -> stringResource(MR.strings.jwapp)
      "ecard" -> stringResource(MR.strings.ecard)
      "mail_box" -> stringResource(MR.strings.student_mailbox)
      "ip_gateway" -> stringResource(MR.strings.ip_gateway)
      else -> ""
    }
  }

  @Composable
  fun appIcon(appId: String): ImageResource {
    return when (appId) {
      "campus_run" -> MR.images.ic_fluent_run_48_regular
      "jwapp" -> MR.images.ic_fluent_hat_graduation_48_regular
      "ecard" -> MR.images.ic_fluent_wallet_credit_card_24_regular
      "mail_box" -> MR.images.ic_fluent_mail_48_regular
      "ip_gateway" -> MR.images.ic_fluent_desktop_signal_24_regular
      else -> MR.images.ic_fluent_arrow_sort_24_regular
    }
  }

  var list by remember { mutableStateOf(viewModel.getAppCardsOrder()) }
  val lazyListState = rememberLazyListState()
  val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
    list = list.toMutableList().apply {
      add(to.index, removeAt(from.index))
    }
    viewModel.setAppCardsOrder(list)
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      shape = RoundedCornerShape(16.dp),
    ) {
      Column(
        modifier = Modifier
          .padding(16.dp)
          .clip(RoundedCornerShape(8.dp))
      ) {
        DialogTitle(
          MR.images.ic_fluent_arrow_sort_24_regular,
          stringResource(MR.strings.sort_apps)
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
          state = lazyListState,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(list, key = { it.first }) {
            ReorderableItem(reorderableLazyListState, key = it.first) { isDragging ->
              val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)
              Surface(
                shadowElevation = elevation,
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent,
                shape = RoundedCornerShape(8.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier
                    .draggableHandle()
                  // .padding(8.dp)
                ) {
                  val id = it.first
                  val enabled = it.second
                  Checkbox(
                    checked = enabled,
                    onCheckedChange = { newEnabled ->
                      list = list.toMutableList().apply {
                        val index = indexOfFirst { it.first == id }
                        if (index != -1) {
                          this[index] = id to newEnabled
                        }
                      }
                      viewModel.setAppCardsOrder(list)
                    }
                  )
                  Icon(
                    painter = painterResource(appIcon(it.first)),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                  )
                  Text(appName(it.first))
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 提示文字

        Column(
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = stringResource(MR.strings.reorder_details_1),
            style = MaterialTheme.typography.bodyMedium
          )
          Text(
            text = stringResource(MR.strings.reorder_details_2),
            style = MaterialTheme.typography.bodyMedium
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
          onClick = onDismiss,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(stringResource(MR.strings.done))
        }
      }
    }
  }
}
