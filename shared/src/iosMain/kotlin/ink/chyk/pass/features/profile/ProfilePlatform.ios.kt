@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package ink.chyk.pass.features.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import ink.chyk.pass.ui.currentViewController
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject
import platform.posix.memcpy

private val platformUiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

@Composable
actual fun rememberUploadAvatarPicker(
  setAvatarToUpload: (ByteArray, String, String) -> Unit,
): UploadAvatarPicker {
  val picker = remember { IosAvatarPicker(setAvatarToUpload) }
  SideEffect { picker.onAvatarSelected = setAvatarToUpload }
  return picker
}

private class IosAvatarPicker(
  var onAvatarSelected: (ByteArray, String, String) -> Unit,
) : UploadAvatarPicker {
  private val delegate = IosImagePickerDelegate { bytes ->
    onAvatarSelected(bytes, "image/jpeg", "avatar.jpg")
  }

  override fun launch() {
    val viewController = currentViewController() ?: return
    val picker = UIImagePickerController().apply {
      sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
      mediaTypes = listOf("public.image")
      delegate = this@IosAvatarPicker.delegate
    }
    viewController.presentViewController(picker, animated = true, completion = null)
  }
}

private class IosImagePickerDelegate(
  val onImageSelected: (ByteArray) -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
  override fun imagePickerController(
    picker: UIImagePickerController,
    didFinishPickingMediaWithInfo: Map<Any?, *>,
  ) {
    val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
    val bytes = image?.let { UIImageJPEGRepresentation(it, 0.9)?.toByteArray() }
    picker.dismissViewControllerAnimated(true) {
      if (bytes != null) onImageSelected(bytes)
    }
  }

  override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
    picker.dismissViewControllerAnimated(true, completion = null)
  }
}

actual fun platformCopyText(label: String, text: String) {
  UIPasteboard.generalPasteboard.string = text
}

actual fun platformNotify(message: String) {
  platformUiScope.launch {
    val viewController = currentViewController() ?: return@launch
    val alert = UIAlertController.alertControllerWithTitle(
      title = null,
      message = message,
      preferredStyle = UIAlertControllerStyleAlert,
    )
    viewController.presentViewController(alert, animated = true, completion = null)
    delay(1_500)
    alert.dismissViewControllerAnimated(true, completion = null)
  }
}

actual fun platformExportCourses(calendar: String) {
  val path = NSTemporaryDirectory() + "course_schedule.ics"
  check(calendar.encodeToByteArray().toNSData().writeToFile(path, atomically = true))
  platformUiScope.launch {
    val viewController = currentViewController() ?: return@launch
    val activityController = UIActivityViewController(
      activityItems = listOf(NSURL.fileURLWithPath(path)),
      applicationActivities = null,
    )
    activityController.popoverPresentationController?.let { popover ->
      popover.sourceView = viewController.view
      popover.sourceRect = viewController.view.bounds
    }
    viewController.presentViewController(activityController, animated = true, completion = null)
  }
}

private fun ByteArray.toNSData(): NSData =
  if (isEmpty()) {
    NSData.data()
  } else {
    usePinned { pinned -> NSData.dataWithBytes(pinned.addressOf(0), size.toULong()) }
  }

private fun NSData.toByteArray(): ByteArray {
  if (length == 0uL) return ByteArray(0)
  return ByteArray(length.toInt()).also { result ->
    result.usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
  }
}
