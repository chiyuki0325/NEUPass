package ink.chyk.pass.features.profile

import android.content.*
import android.widget.Toast
import androidx.core.content.FileProvider
import ink.chyk.pass.NEUPassApplication
import ink.chyk.pass.resources.MR
import java.io.File

actual fun platformCopyText(label: String, text: String) {
  val clipboard = NEUPassApplication.context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
  clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
actual fun platformNotify(message: String) = Toast.makeText(NEUPassApplication.context, message, Toast.LENGTH_SHORT).show()
actual fun platformExportCourses(calendar: String) {
  val context = NEUPassApplication.context
  try {
    val file = File.createTempFile("course_schedule_", ".ics", context.cacheDir).apply {
      writeText(calendar)
    }
    context.startActivity(createCalendarExportIntent(context, file))
  } catch (_: Exception) {
    Toast.makeText(context, MR.strings.export_failed.getString(context), Toast.LENGTH_SHORT).show()
  }
}

internal fun createCalendarExportIntent(context: Context, file: File): Intent {
  val uri = FileProvider.getUriForFile(
    context,
    "${context.packageName}.fileprovider",
    file
  )
  val openCalendar = Intent(Intent.ACTION_VIEW).apply {
    setDataAndType(uri, "text/calendar")
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
  return Intent.createChooser(openCalendar, "导出课程到系统日历").apply {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  }
}
