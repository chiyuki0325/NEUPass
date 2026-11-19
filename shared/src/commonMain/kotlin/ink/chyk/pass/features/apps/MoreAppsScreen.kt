package ink.chyk.pass.features.apps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tencent.tmm.kmmresource.compose.painterResource
import com.tencent.tmm.kmmresource.compose.stringResource
import ink.chyk.pass.components.BackNavigationBar
import ink.chyk.pass.resources.MR
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun MoreAppsScreen(
  viewModel: AppsViewModel,
  innerPadding: PaddingValues,
  onOpenCampusRun: (String) -> Unit,
  onBack: () -> Unit,
  onOpenJwapp: () -> Unit,
  onOpenECard: () -> Unit,
) {
  val appCardsOrder by viewModel.appCardsOrder.collectAsState()
  val chunkedAppLinks = AppLinks.links.chunked(4)

  Column(
    Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Spacer(Modifier.height(24.dp))
    BackNavigationBar(
      onBack = onBack,
      title = stringResource(MR.strings.more_apps),
    )

    LazyColumn {
      // 被隐藏的小程序应用卡片
      appCardsOrder.forEach {
        if (!it.second) item(key = it.first) {
          Spacer(Modifier.height(16.dp))
          AppCardImpl(it.first, viewModel, Modifier.animateItem(), onOpenCampusRun, onOpenJwapp, onOpenECard)
        }
      }
      item { Spacer(Modifier.height(24.dp)) }
      item {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
          Text(
            stringResource(MR.strings.web_apps_vpn),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
      item { Spacer(Modifier.height(16.dp)) }
      items(chunkedAppLinks) { rowAppLinks ->
        Row(
          Modifier.fillMaxWidth().padding(horizontal = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          rowAppLinks.forEach { app ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              IconButton(
                onClick = { viewModel.openWebApp(app.app) },
                colors = IconButtonColors(
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                  containerColor = MaterialTheme.colorScheme.primary,
                  disabledContentColor = MaterialTheme.colorScheme.onSecondary,
                  disabledContainerColor = MaterialTheme.colorScheme.secondary,
                ),
                modifier = Modifier.size(56.dp),
              ) {
                Icon(painterResource(app.icon), stringResource(app.name), Modifier.size(32.dp))
              }
              Spacer(Modifier.height(4.dp))
              Text(stringResource(app.name), style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
        Spacer(Modifier.height(16.dp))
      }
    }
  }
}
