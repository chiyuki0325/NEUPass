@file:OptIn(kotlin.time.ExperimentalTime::class)

package ink.chyk.pass.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

object DateTimeUtils {
  val basicDateFormat = LocalDate.Formats.ISO_BASIC

  val dateTimeSecondsFormat = LocalDateTime.Format {
    date(LocalDate.Formats.ISO)
    char(' ')
    hour()
    char(':')
    minute()
    char(':')
    second()
  }

  val timeMinutesFormat = LocalTime.Format {
    hour()
    char(':')
    minute()
  }

  val icsDateTimeFormat = LocalDateTime.Format {
    date(LocalDate.Formats.ISO_BASIC)
    char('T')
    hour()
    minute()
    second()
  }

  fun currentLocalDateTime(): LocalDateTime =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

  fun currentLocalDate(): LocalDate = currentLocalDateTime().date

  fun currentLocalTime(): LocalTime = currentLocalDateTime().time

  fun localDateFromUtcMilliseconds(milliseconds: Long): LocalDate {
    val dayMillis = 86_400_000L
    val truncatedDays = milliseconds / dayMillis
    val epochDays = if (milliseconds < 0 && milliseconds % dayMillis != 0L) truncatedDays - 1 else truncatedDays
    return LocalDate.fromEpochDays(epochDays.toInt())
  }

  fun displayName(dayOfWeek: DayOfWeek, short: Boolean = false): String = when (dayOfWeek) {
    DayOfWeek.MONDAY -> if (short) "周一" else "星期一"
    DayOfWeek.TUESDAY -> if (short) "周二" else "星期二"
    DayOfWeek.WEDNESDAY -> if (short) "周三" else "星期三"
    DayOfWeek.THURSDAY -> if (short) "周四" else "星期四"
    DayOfWeek.FRIDAY -> if (short) "周五" else "星期五"
    DayOfWeek.SATURDAY -> if (short) "周六" else "星期六"
    DayOfWeek.SUNDAY -> if (short) "周日" else "星期日"
  }
}
