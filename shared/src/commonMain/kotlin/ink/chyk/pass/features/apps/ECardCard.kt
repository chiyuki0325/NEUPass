package ink.chyk.pass.features.apps

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tencent.tmm.kmmresource.compose.stringResource
import ink.chyk.pass.LoadingState
import ink.chyk.pass.components.shimmerContent
import ink.chyk.pass.resources.MR
import ink.chyk.pass.utils.DateTimeUtils
import kotlinx.datetime.format

@Composable
fun ECardCard(manager: ECardManager, onOpen: () -> Unit) {
  val loadingState by manager.loadingState.collectAsState()
  val records by manager.records.collectAsState()
  LaunchedEffect(manager) { manager.init() }
  val subtitle = stringResource(
    if (loadingState == LoadingState.FAILED) MR.strings.ecard_records_failed
    else MR.strings.ecard_records_description
  )

  AppCard(
    icon = MR.images.ic_fluent_wallet_credit_card_24_regular,
    title = stringResource(MR.strings.ecard),
    subtitle = AnnotatedString(subtitle),
    subtitleContent = {
      Text(
        text = subtitle,
        modifier = Modifier.shimmerContent(
          isLoading = loadingState == LoadingState.LOADING || loadingState == LoadingState.PARTIAL_SUCCESS,
          durationMillis = 1000,
          sweepWidth = 1.5f,
        ),
      )
    },
    jumpButtonIcon = MR.images.ic_fluent_chevron_right_20_filled,
    jumpButtonClick = onOpen,
  ) {
    if (records.isNotEmpty() || loadingState == LoadingState.SUCCESS) {
      Spacer(Modifier.height(12.dp))
      if (records.isEmpty()) {
        Text(stringResource(MR.strings.ecard_records_empty))
      } else {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          ECardRecentRow(
            merchant = stringResource(MR.strings.ecard_records_machine),
            time = stringResource(MR.strings.ecard_records_time),
            amount = stringResource(MR.strings.ecard_records_amount),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          records.forEach { record ->
            ECardRecentRow(
              merchant = record.machine.ifBlank { "—" },
              time = "${record.time.date.toString().substring(5).replace('-', '/')} ${record.time.time.format(DateTimeUtils.timeMinutesFormat)}",
              amount = if ("圈存划账成功" in record.description) "+${record.amount}" else record.amount,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ECardRecentRow(
  merchant: String,
  time: String,
  amount: String,
  color: Color,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = merchant,
      color = color,
      fontSize = 15.sp,
      lineHeight = 18.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )
    listOf(time, amount).forEachIndexed { index, value ->
      Text(
        text = value,
        color = color,
        fontSize = 15.sp,
        lineHeight = 18.sp,
        textAlign = TextAlign.End,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.width(if (index == 0) 108.dp else 80.dp),
      )
    }
  }
}
