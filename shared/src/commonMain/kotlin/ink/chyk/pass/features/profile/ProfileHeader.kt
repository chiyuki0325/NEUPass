package ink.chyk.pass.features.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import ink.chyk.pass.api.portal.*
import ink.chyk.pass.components.*
import ink.chyk.pass.repositories.SettingsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*


@Composable
fun ProfileHeader(
  viewModel: ProfileViewModel,
  onAvatarSelected: () -> Unit,
) {
  // 大头，学号等

  val userInfo by viewModel.userInfo.collectAsState()
  val redactSensitiveInfo by SettingsRepository.redactSensitiveInfo.collectAsState()
  // 开发时用固定的姓名、学号和邮箱展示个人资料，便于截图时隐藏真实身份。
  val displayName = if (redactSensitiveInfo) "董慧敏" else userInfo?.name.orEmpty()
  val displayStudentId = if (redactSensitiveInfo) "20241024" else userInfo?.xgh.orEmpty()
  val displayEmail = if (redactSensitiveInfo) "20241024@stu.neu.edu.cn" else userInfo?.email.orEmpty()

  // 头像由门户 API 拉取，字节统一在 common 解码。
  val avatar by produceState<ImageBitmap?>(null, userInfo?.avatar) {
    val path = userInfo?.avatar?.takeIf { it.isNotEmpty() } ?: return@produceState
    try {
      viewModel.loadAvatar(path).collect { result ->
        runCatching { result.value.decodeToImageBitmap() }.getOrNull()?.let { value = it }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      // 网络失败时保留已解码的缓存或骨架。
    }
  }

  // 上传头像选择器
  val picker = rememberUploadAvatarPicker { byteArray, mimeType, fileName ->
    viewModel.setAvatarToUpload(byteArray, mimeType, fileName)
    onAvatarSelected()
  }

  var avatarHeight by remember { mutableIntStateOf(0) }
  val heightDp = with(androidx.compose.ui.platform.LocalDensity.current) {
    avatarHeight.toDp().takeIf { it > 0.dp } ?: 120.dp
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp)
      .onSizeChanged {
        avatarHeight = it.height
      }
  ) {
    if (userInfo != null) {
      // 头像
      Avatar(
        picker = picker,
        avatar = avatar,
        heightDp = heightDp,
      )
      Spacer(modifier = Modifier.width(16.dp))
      // 文字
      Column {
        Text(
          text = displayName,
          style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "${userInfo!!.depart} ${userInfo!!.identity}\n学号: $displayStudentId\n邮箱: $displayEmail",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.clickable {
            platformCopyText(userInfo!!.xgh, userInfo!!.xgh)
            platformNotify("已复制学号")
          },
          maxLines = 3,
          overflow = TextOverflow.Ellipsis
        )
      }
    } else {
      ProfileHeaderSkeleton()
    }
  }
}


@Composable
fun Avatar(
  picker: UploadAvatarPicker,
  avatar: ImageBitmap?,
  heightDp: Dp = 120.dp,
) {
  Box(
    modifier = Modifier
      .height(heightDp)
      .clip(RoundedCornerShape(8.dp))
      .clickable { picker.launch() }
  ) {
    if (avatar != null) {
      Image(
        bitmap = avatar,
        contentDescription = "Avatar",
        modifier = Modifier.height(heightDp),
        contentScale = ContentScale.Fit,
      )
    } else {
      AvatarSkeleton()
    }
  }
}

@Composable
fun ProfileHeaderSkeleton() {
  val nameStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
  val bodyStyle = MaterialTheme.typography.bodyMedium

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(IntrinsicSize.Min)
  ) {
    // 头像占位
    AvatarSkeleton()

    Spacer(modifier = Modifier.width(16.dp))

    Column {
      // 姓名行：匹配 24.sp Bold 的高度
      TextSkeleton(style = nameStyle, width = 120.dp)

      Spacer(modifier = Modifier.height(8.dp))

      // 详情信息块：匹配 bodyMedium 的高度
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // 部门 + 身份
        TextSkeleton(style = bodyStyle, widthFraction = 0.75f)
        // 学号
        TextSkeleton(style = bodyStyle, widthFraction = 0.5f)
        // 邮箱
        TextSkeleton(style = bodyStyle, widthFraction = 0.85f)
      }
    }
  }
}

@Composable
fun AvatarSkeleton() {
  Box(
    modifier = Modifier
      .width(96.dp)
      .fillMaxHeight()
      .clip(RoundedCornerShape(8.dp))
      .shimmerBackground()
  )
}
