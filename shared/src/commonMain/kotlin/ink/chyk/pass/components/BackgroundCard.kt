package ink.chyk.pass.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BackgroundCard(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Card(
    modifier = modifier,
    colors = if (isSystemInDarkTheme()) {
      CardDefaults.cardColors()
    } else {
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    },
    shape = RoundedCornerShape(16.dp),
    content = content,
  )
}
