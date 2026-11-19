package ink.chyk.pass.features.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import ink.chyk.pass.components.*
import ink.chyk.pass.utils.platformVersionName
import ink.chyk.pass.resources.MR
import com.tencent.tmm.kmmresource.compose.stringResource


@Composable
fun LogoutConfirmationDialog(
  showDialog: MutableState<Boolean>,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  if (showDialog.value) {
    AlertDialog(
      onDismissRequest = {
        // 当点击外部区域或返回键时关闭对话框
        onDismiss()
      },
      title = {
        DialogTitle(MR.images.ic_fluent_person_swap_24_regular, "登出")
      },
      text = {
        Text("确定要登出当前的账号吗？")
      },
      confirmButton = {
        Button(onClick = {
          onConfirm()
          showDialog.value = false // 关闭对话框
        }) {
          Text("确认")
        }
      },
      dismissButton = {
        Button(onClick = {
          onDismiss()
          showDialog.value = false // 关闭对话框
        }) {
          Text("取消")
        }
      }
    )
  }
}


@Composable
fun AboutDialog(
  showDialog: MutableState<Boolean>,
  onDismiss: () -> Unit,
  onOpenLicenses: () -> Unit,
) {
  if (showDialog.value) {
    val versionName = platformVersionName()
    val uriHandler = LocalUriHandler.current
    val sourceUrl = stringResource(MR.strings.source_url)

    Dialog(
      onDismissRequest = onDismiss,
    ) {
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
            MR.images.ic_fluent_book_information_24_regular,
            stringResource(MR.strings.about)
          )

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = stringResource(MR.strings.about_content_1),
            style = MaterialTheme.typography.bodyMedium
          )

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = stringResource(MR.strings.about_version).replace("%1\$s", versionName),
            style = MaterialTheme.typography.bodyMedium
          )

          Spacer(modifier = Modifier.height(8.dp))
          RowButton(
            iconResource = MR.images.ic_fluent_code_24_regular,
            text = stringResource(MR.strings.about_content_source),
            clickable = true,
            onClick = {
              uriHandler.openUri(sourceUrl)
              onDismiss()
            }
          )
          RowButton(
            iconResource = MR.images.ic_fluent_book_information_24_regular,
            text = stringResource(MR.strings.open_source_licenses),
            clickable = true,
            onClick = {
              onDismiss()
              onOpenLicenses()
            }
          )
        }
      }
    }
  }
}

@Composable
fun UploadAvatarConfirmDialog(
  showDialog: MutableState<Boolean>,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  if (showDialog.value) {
    AlertDialog(
      onDismissRequest = onDismiss,
      title = {
        DialogTitle(
          MR.images.ic_fluent_person_swap_24_regular,
          stringResource(MR.strings.upload_avatar)
        )
      },
      text = {
        Text(stringResource(MR.strings.upload_avatar_confirm))
      },
      confirmButton = {
        Button(onClick = {
          onConfirm()
          showDialog.value = false // 关闭对话框
        }) {
          Text(stringResource(MR.strings.confirm))
        }
      },
      dismissButton = {
        Button(onClick = {
          onDismiss()
          showDialog.value = false // 关闭对话框
        }) {
          Text(stringResource(MR.strings.cancel))
        }
      }
    )
  }
}
