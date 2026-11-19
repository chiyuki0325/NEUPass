package ink.chyk.pass

import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ink.chyk.pass.features.profile.platformExportCourses
import java.io.FileInputStream
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarExportTest {
  @Test
  fun writesIcsAndOpensCalendarChooser() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val context = instrumentation.targetContext
    context.cacheDir.listFiles { file ->
      file.name.startsWith("course_schedule_") && file.extension == "ics"
    }?.forEach { it.delete() }

    platformExportCourses()
    instrumentation.waitForIdleSync()

    val calendarFile = context.cacheDir.listFiles { file ->
      file.name.startsWith("course_schedule_") && file.extension == "ics"
    }?.single()
    assertTrue(calendarFile?.readText()?.startsWith("BEGIN:VCALENDAR") == true)

    val activities = instrumentation.uiAutomation
      .executeShellCommand("dumpsys activity activities")
      .readText()
    assertTrue(
      activities.contains("ResolverActivity") ||
        activities.contains("ChooserActivity") ||
        activities.contains("com.android.calendar")
    )
  }
}

private fun ParcelFileDescriptor.readText(): String =
  FileInputStream(fileDescriptor).bufferedReader().use { it.readText() }
