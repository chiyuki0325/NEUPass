package ink.chyk.pass.features.apps

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tencent.tmm.kmmresource.compose.stringResource
import ink.chyk.pass.api.ecard.ECardBankRecord
import ink.chyk.pass.resources.MR
import ink.chyk.pass.utils.DateTimeUtils
import kotlinx.datetime.format

@Composable
internal fun ECardTableContainer(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(modifier.fillMaxWidth(), content = content)
}

@Composable
internal fun ECardTableHeader() {
  ECardTableRow(
    merchant = stringResource(MR.strings.ecard_records_machine),
    time = stringResource(MR.strings.ecard_records_time),
    amount = stringResource(MR.strings.ecard_records_amount),
    isHeader = true,
  )
}

@Composable
internal fun ECardTableRecord(record: ECardBankRecord) {
  ECardTableRow(
    merchant = record.machine.ifBlank { "—" },
    time = "${record.time.date.toString().replace('-', '/')}\n${record.time.time.format(DateTimeUtils.timeMinutesFormat)}",
    amount = if ("圈存划账成功" in record.description) "+${record.amount}" else record.amount,
    description = record.description,
  )
}

@Composable
private fun ECardTableRow(
  merchant: String,
  time: String,
  amount: String,
  description: String = "",
  isHeader: Boolean = false,
) {
  val color = if (isHeader) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
  val fontWeight = if (isHeader) FontWeight.Medium else FontWeight.Normal
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        merchant,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = fontWeight,
        color = color,
      )
      if (description.isNotBlank()) {
        Text(
          description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    Text(
      time,
      modifier = Modifier.width(88.dp),
      style = MaterialTheme.typography.bodySmall,
      textAlign = TextAlign.Center,
      fontWeight = fontWeight,
      color = if (isHeader) color else MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      amount,
      modifier = Modifier.width(76.dp),
      style = MaterialTheme.typography.bodyMedium,
      textAlign = TextAlign.End,
      fontWeight = if (isHeader) fontWeight else FontWeight.Medium,
      color = color,
    )
  }
  HorizontalDivider()
}
