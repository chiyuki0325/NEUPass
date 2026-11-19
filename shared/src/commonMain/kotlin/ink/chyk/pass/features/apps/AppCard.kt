package ink.chyk.pass.features.apps

import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.tencent.tmm.kmmresource.compose.painterResource
import com.tencent.tmm.kmmresource.resource.ImageResource

@Composable
    /** AppsScreen 中应用卡片的基础组件
     * @param content 卡片内容
     */
fun AppCard(
  icon: ImageResource,
  title: String,
  subtitle: AnnotatedString,
  hasJumpButton: Boolean = true,
  jumpButtonEnabled: Boolean = true,
  jumpButtonIcon: ImageResource? = null,
  jumpButtonClick: () -> Unit = {},
  subtitleMaxLines: Int = Int.MAX_VALUE,
  subtitleContent: (@Composable () -> Unit)? = null,
  content: @Composable () -> Unit,
) {
  val isDark = isSystemInDarkTheme()
  Card(
    colors = if (isDark) CardDefaults.cardColors() else { CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer) },
    modifier = Modifier.animateContentSize(),
  ) {
    Column(
      modifier = Modifier
        .padding(16.dp)
        .fillMaxWidth(),
    ) {

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        // 应用图标
        Icon(
          painter = painterResource(icon),
          contentDescription = null,
          modifier = Modifier.size(48.dp),
        )
        Column(
          modifier = Modifier.weight(1f),
        ) {
          // 应用标题
          Text(
            title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
          )
          // 第二行 下面的灰字
          if (subtitleContent != null) {
            ProvideTextStyle(
              MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
              )
            ) {
              subtitleContent()
            }
          } else {
            Text(
              subtitle,
              color = MaterialTheme.colorScheme.onSurface,
              fontSize = 15.sp,
              maxLines = subtitleMaxLines,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        if (hasJumpButton) {
          key(jumpButtonEnabled) {
            IconButton(
              enabled = jumpButtonEnabled,
              onClick = jumpButtonClick,
              colors = IconButtonColors(
                contentColor = MaterialTheme.colorScheme.onPrimary,
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = MaterialTheme.colorScheme.onSecondary,
                disabledContainerColor = MaterialTheme.colorScheme.secondary,
              ),
              modifier = Modifier.size(48.dp),
            ) {
              Icon(
                painter = painterResource(requireNotNull(jumpButtonIcon)),
                contentDescription = title,
                modifier = Modifier.size(32.dp),
              )
            }
          }
        }
        // 第一行结束
      }

      // 第二行开始为各个应用的内容
      content()
    }
  }
}
