package ink.chyk.pass.services.courses

import ink.chyk.pass.models.Exam
import ink.chyk.pass.models.RenderedExam
import ink.chyk.pass.utils.AppLog
import ink.chyk.pass.utils.DateTimeUtils
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

fun renderExams(exams: List<Exam>): List<RenderedExam> = exams.map { exam ->
  val date = runCatching {
    LocalDate.parse(exam.date)
  }.getOrElse {
    // 解析失败则视为未知日期，放在最后
    AppLog.e("ExamRenderer", "Date parse failed: ${exam.date}")
    LocalDate(2099, 1, 1)
  }

  val (start, end) = runCatching {
    val startTime = LocalTime.parse(exam.start, DateTimeUtils.timeMinutesFormat)
    val endTime = LocalTime.parse(exam.end, DateTimeUtils.timeMinutesFormat)
    LocalDateTime(date, startTime) to LocalDateTime(date, endTime)
  }.getOrElse {
    // 解析失败则视为未知时间，放在最后
    AppLog.e("ExamRenderer", "Time parse failed: ${exam.start}-${exam.end}")
    LocalDateTime(date, LocalTime(0, 0)) to LocalDateTime(date, LocalTime(0, 0))
  }

  RenderedExam(
    name = exam.name,
    type = exam.type,
    date = date,
    start = start,
    end = end,
    location = exam.location,
    seat = exam.seat,
    remark = exam.remark,
    genre = exam.genre,
    color = exam.color,
  )
}
