package ink.chyk.pass.utils

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeUtilsTest {
  @Test
  fun formatsPersistedAndRemoteValues() {
    val date = LocalDate(2026, 3, 1)
    val dateTime = LocalDateTime(date, LocalTime(8, 5, 9))

    assertEquals("20260301", date.format(DateTimeUtils.basicDateFormat))
    assertEquals(date, DateTimeUtils.basicDateFormat.parse("20260301"))
    assertEquals("2026-03-01 08:05:09", dateTime.format(DateTimeUtils.dateTimeSecondsFormat))
    assertEquals(dateTime, DateTimeUtils.dateTimeSecondsFormat.parse("2026-03-01 08:05:09"))
    assertEquals("20260301T080509", dateTime.format(DateTimeUtils.icsDateTimeFormat))
    assertEquals(LocalTime(8, 5), DateTimeUtils.timeMinutesFormat.parse("08:05"))
  }

  @Test
  fun handlesCalendarBoundaries() {
    assertEquals(
      LocalDate(2027, 1, 1),
      LocalDate(2026, 12, 31).plus(1, DateTimeUnit.DAY)
    )
    assertEquals(
      LocalDate(2026, 3, 8),
      LocalDate(2026, 3, 1).plus(1, DateTimeUnit.WEEK)
    )
  }

  @Test
  fun convertsDatePickerUtcMilliseconds() {
    assertEquals(LocalDate(1970, 1, 1), DateTimeUtils.localDateFromUtcMilliseconds(0))
    assertEquals(LocalDate(1969, 12, 31), DateTimeUtils.localDateFromUtcMilliseconds(-1))
  }

  @Test
  fun formatsCourseWeekHeaderDate() {
    val date = LocalDate(2026, 11, 23)

    assertEquals("11/23", "${date.month.number}/${date.day}")
  }

  @Test
  fun providesChineseWeekdayNames() {
    assertEquals("星期日", DateTimeUtils.displayName(DayOfWeek.SUNDAY))
    assertEquals("周一", DateTimeUtils.displayName(DayOfWeek.MONDAY, short = true))
  }
}
