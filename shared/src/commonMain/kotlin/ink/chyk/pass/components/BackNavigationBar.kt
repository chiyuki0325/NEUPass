package ink.chyk.pass.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.tencent.tmm.kmmresource.compose.painterResource
import ink.chyk.pass.resources.MR

@Composable
fun BackNavigationBar(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  title: String? = null,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
      Icon(painterResource(MR.images.ic_fluent_chevron_left_20_filled), "Back")
    }
    if (title != null) {
      Text(title, style = MaterialTheme.typography.headlineLarge)
    }
  }
}
