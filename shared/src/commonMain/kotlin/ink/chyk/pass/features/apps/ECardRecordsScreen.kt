package ink.chyk.pass.features.apps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tencent.tmm.kmmresource.compose.stringResource
import ink.chyk.pass.api.ecard.ECardWallet
import ink.chyk.pass.components.BackNavigationBar
import ink.chyk.pass.resources.MR

@Composable
fun ECardRecordsScreen(
  viewModel: ECardRecordsViewModel,
  innerPadding: PaddingValues,
  onBack: () -> Unit,
) {
  val state by viewModel.state.collectAsState()

  Column(
    Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Spacer(Modifier.height(24.dp))
    BackNavigationBar(onBack = onBack, title = stringResource(MR.strings.ecard_records))
    PrimaryTabRow(selectedTabIndex = state.wallet.ordinal) {
      ECardWallet.entries.forEach { wallet ->
        Tab(
          selected = state.wallet == wallet,
          onClick = { viewModel.selectWallet(wallet) },
          text = {
            Text(stringResource(
              if (wallet == ECardWallet.MAIN) MR.strings.ecard_main_wallet else MR.strings.ecard_subsidy_wallet
            ))
          },
        )
      }
    }

    key(state.wallet) {
      val listState = rememberLazyListState()
      LaunchedEffect(listState, state.isLoading, state.failed, state.hasMore, state.records.size) {
        if (state.isLoading || state.failed || !state.hasMore) return@LaunchedEffect
        snapshotFlow {
          val layout = listState.layoutInfo
          layout.visibleItemsInfo.lastOrNull()?.index == layout.totalItemsCount - 1
        }.collect { atBottom ->
          if (atBottom) viewModel.loadMore()
        }
      }

      ECardTableContainer(Modifier.weight(1f)) {
        ECardTableHeader()
        LazyColumn(
          state = listState,
          modifier = Modifier.weight(1f),
          contentPadding = PaddingValues(vertical = 8.dp),
        ) {
          items(state.records) { record ->
            ECardTableRecord(record)
          }
          item {
            Column(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              when {
                state.isLoading -> CircularProgressIndicator()
                state.failed -> {
                  Text(stringResource(MR.strings.ecard_records_failed))
                  TextButton(onClick = viewModel::loadMore) {
                    Text(stringResource(MR.strings.retry))
                  }
                }
                state.records.isEmpty() -> Text(stringResource(MR.strings.ecard_records_empty))
                !state.hasMore -> Text(
                  stringResource(MR.strings.ecard_records_end),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    }
  }
}
