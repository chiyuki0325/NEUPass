package ink.chyk.pass.features.profile

import androidx.compose.runtime.Composable

interface UploadAvatarPicker { fun launch() }

@Composable
expect fun rememberUploadAvatarPicker(
  setAvatarToUpload: (ByteArray, String, String) -> Unit,
): UploadAvatarPicker

expect fun platformCopyText(label: String, text: String)
expect fun platformNotify(message: String)
expect fun platformExportCourses(calendar: String)
