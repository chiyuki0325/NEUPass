package ink.chyk.pass.features.profile

import androidx.compose.runtime.Composable
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable actual fun rememberUploadAvatarPicker(setAvatarToUpload: (ByteArray, String, String) -> Unit): UploadAvatarPicker =
  object : UploadAvatarPicker { override fun launch() = Unit }
actual fun platformCopyText(label: String, text: String) { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null) }
actual fun platformNotify(message: String) = Unit
actual fun platformExportCourses(calendar: String) = Unit
