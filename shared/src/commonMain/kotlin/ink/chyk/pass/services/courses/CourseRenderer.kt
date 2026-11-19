package ink.chyk.pass.services.courses

import ink.chyk.pass.models.Campus
import ink.chyk.pass.models.Course
import ink.chyk.pass.models.CourseGenre
import ink.chyk.pass.models.RenderedCourse
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus

// start/end section 展开成两校区实际时间
private val classStartTimeHunnan = listOf(
  "08:30", "09:25", "10:30", "11:25", "14:00", "14:55",
  "16:00", "16:55", "18:30", "19:25", "20:20", "21:15",
)
private val classEndTimeHunnan = listOf(
  "09:15", "10:10", "11:15", "12:10", "14:45", "15:40",
  "16:45", "17:40", "19:15", "20:10", "21:05", "22:00",
)
private val classStartTimeNanhu = listOf(
  "08:00", "08:55", "10:00", "10:55", "14:00", "14:55",
  "16:00", "16:55", "18:30", "19:25", "20:20", "21:15",
)
private val classEndTimeNanhu = listOf(
  "08:45", "09:40", "10:45", "11:40", "14:45", "15:40",
  "16:45", "17:40", "19:15", "20:10", "21:05", "22:00",
)

fun courseStartTime(campus: Campus?, section: Int): String =
  (if (campus == Campus.HUNNAN) classStartTimeHunnan else classStartTimeNanhu)[section - 1]

fun courseEndTime(campus: Campus?, section: Int): String =
  (if (campus == Campus.HUNNAN) classEndTimeHunnan else classEndTimeNanhu)[section - 1]

fun renderCourses(
  termStart: LocalDate,
  courses: List<Course>,
): Map<LocalDate, List<RenderedCourse>> {
  val firstSunday = termStart.plus(7 - termStart.dayOfWeek.isoDayNumber, DateTimeUnit.DAY)
  val coursesByDate = mutableMapOf<LocalDate, MutableList<RenderedCourse>>()
  courses.forEach { course ->
    val renderedCourse = RenderedCourse(
      name = course.name,
      location = course.location,
      campus = course.campus?.campusName ?: "未安排",
      start = courseStartTime(course.campus, course.startSection),
      end = courseEndTime(course.campus, course.endSection),
      period = course.period,
      genre = course.genre ?: CourseGenre.OTHER,
      startSection = course.startSection,
      endSection = course.endSection,
      key = course.key,
      color = course.color,
      teacher = course.teacher,
    )
    // 展开周次，每周对应一个 RenderedCourse
    var weeks = course.weeks
    var week = 0
    while (weeks != 0) {
      if (weeks and 1 == 1) {
        val date = firstSunday.plus(week, DateTimeUnit.WEEK).plus(course.dayOfWeek, DateTimeUnit.DAY)
        coursesByDate.getOrPut(date) { mutableListOf() }.add(renderedCourse)
      }
      weeks = weeks ushr 1
      week++
    }
  }
  return coursesByDate.mapValues { (_, dailyCourses) -> dailyCourses.sortedBy { it.startSection } }
}
