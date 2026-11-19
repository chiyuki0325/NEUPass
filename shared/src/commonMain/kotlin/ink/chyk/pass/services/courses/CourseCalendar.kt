@file:OptIn(kotlin.time.ExperimentalTime::class, kotlin.uuid.ExperimentalUuidApi::class)

package ink.chyk.pass.services.courses

import ink.chyk.pass.models.RenderedCourse
import ink.chyk.pass.utils.DateTimeUtils
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.uuid.Uuid

/**
 * 将按日期展开的课表导出为ICS文本，交给平台侧保存或分享。
 * 每天的每条RenderedCourse对应一个独立日历事件，同一门课跨周上课也分别生成事件。
 */
fun createCourseCalendar(coursesByDate: Map<LocalDate, List<RenderedCourse>>): String = buildString {
  append("BEGIN:VCALENDAR\r\n")
  append("VERSION:2.0\r\n")
  append("PRODID:-//NEUPass//CourseSchedule//CN\r\n")
  append("CALSCALE:GREGORIAN\r\n")
  append("METHOD:PUBLISH\r\n")
  // DTSTAMP记录本次导出时间，使用UTC；末尾的Z表示UTC时区
  val timestamp = Clock.System.now().toLocalDateTime(TimeZone.UTC).format(DateTimeUtils.icsDateTimeFormat)
  coursesByDate.toList().sortedBy { it.first }.forEach { (date, courses) ->
    val datePart = date.format(DateTimeUtils.basicDateFormat)
    courses.forEach { course ->
      append("BEGIN:VEVENT\r\n")
      // 每次导出重新生成事件标识，同一课程重复导出也会得到不同UID
      append("UID:${Uuid.random().toHexString()}\r\n")
      append("DTSTAMP:${timestamp}Z\r\n")
      append("DTSTART;TZID=Asia/Shanghai:${datePart}T${course.start.replace(":", "")}00\r\n")
      append("DTEND;TZID=Asia/Shanghai:${datePart}T${course.end.replace(":", "")}00\r\n")
      append("SUMMARY:${escapeCalendarText(course.name)}\r\n")
      val location = course.campus?.let { "${course.location} ($it)" } ?: course.location
      append("LOCATION:${escapeCalendarText(location)}\r\n")
      val description = "教师: ${course.teacher}\n节次: 第${course.startSection}-${course.endSection}节"
      append("DESCRIPTION:${escapeCalendarText(description)}\r\n")
      val color = (0xFFFFFF and course.color).toString(16).uppercase().padStart(6, '0')
      append("X-APPLE-TAB-COLOR:#$color\r\n")
      append("END:VEVENT\r\n")
    }
  }
  append("END:VCALENDAR\r\n")
}

// 名称、地点和描述中的特殊字符按日历文本转义；先处理原有反斜杠，避免再次转义后续生成的转义符
private fun escapeCalendarText(text: String): String =
  text.replace("\\", "\\\\")
    .replace("\r\n", "\n")
    .replace("\r", "\n")
    .replace("\n", "\\n")
    .replace(";", "\\;")
    .replace(",", "\\,")
