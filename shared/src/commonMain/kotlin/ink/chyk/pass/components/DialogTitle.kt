package ink.chyk.pass.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import ink.chyk.pass.resources.MR
import com.tencent.tmm.kmmresource.compose.painterResource
import com.tencent.tmm.kmmresource.resource.ImageResource

@Composable
fun DialogTitle(icon: ImageResource, text: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(painterResource(icon), contentDescription = text)
    Text(text, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 8.dp))
  }
}