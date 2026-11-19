package ink.chyk.pass.features.profile

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import ink.chyk.pass.resources.MR

@Composable
actual fun rememberUploadAvatarPicker(
  setAvatarToUpload: (
    byteArray: ByteArray,
    mimeType: String,
    fileName: String
  ) -> Unit,
): UploadAvatarPicker {
  val context = LocalContext.current
  val resolver = context.contentResolver

  val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia())
  {
      uri ->
    if (uri != null) {
      Log.d("PhotoPicker", "Selected URI: $uri")
      // 上传头像
      resolver.openInputStream(uri).use { stream ->
        val byteArray = stream?.readBytes()
        if (byteArray != null) {
          val mimeType = resolver.getType(uri)!!
          // 生成文件名
          val fileName =
            "avatar${uri.pathSegments.last()}.${mimeType.substringAfter("/")}".replace(
              "jpeg",
              "jpg"
            )
          setAvatarToUpload(byteArray, mimeType, fileName)
        } else {
          Toast.makeText(
            context,
            MR.strings.cannot_read_selected_image.getString(context),
            Toast.LENGTH_SHORT
          ).show()
        }
      }
    }
  }

  return object: UploadAvatarPicker {
    override fun launch() {
      picker.launch(
        PickVisualMediaRequest(
          ActivityResultContracts.PickVisualMedia.ImageOnly
        )
      )
    }
  }
}
