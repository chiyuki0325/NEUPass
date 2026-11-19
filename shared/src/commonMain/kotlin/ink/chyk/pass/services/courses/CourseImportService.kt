package ink.chyk.pass.services.courses

import ink.chyk.pass.repositories.CourseRepository
import ink.chyk.pass.repositories.JwappRepository
import ink.chyk.pass.utils.AppLog
import ink.chyk.pass.utils.DateTimeUtils
import kotlinx.coroutines.*

data class CourseImportMessage(
  val text: String,
  val showBullet: Boolean = true,
)

object CourseImportService {
  suspend fun importCourses(
    onOutput: (CourseImportMessage) -> Unit = {},
  ) {
    fun output(message: String, showBullet: Boolean = true) {
      AppLog.i("CourseImport", message)
      onOutput(CourseImportMessage(message, showBullet))
    }

    output("正在连接教务系统…")
    val termId = JwappRepository.getCurrentTermId()
    val firstWeek = JwappRepository.getTermWeeks(termId).minByOrNull { it.serialNumber }
      ?: error("获取学期周数信息失败，未找到第一周信息")
    val termStart = DateTimeUtils.dateTimeSecondsFormat.parse(firstWeek.startDate).date
    output("当前是 $termId 学期，起始日为 $termStart", false)
    val campuses = JwappRepository.getScheduledCampuses(termId)
    output("本学期排课校区：${campuses.joinToString("、") { it.name }.ifEmpty { "无" }}", false)

    output("正在获取课程安排…")
    val schedule = JwappRepository.getScheduleDetail(termId, "")

    if (schedule.arrangedList.isNotEmpty()) {
      val previewCourse = schedule.arrangedList.first()

      output("获取到《${previewCourse.courseName}》等 ${schedule.arrangedList.size} 门课程。", false)
    } else {
      output("暂未获取到已排课课程。", false)
    }

    if (schedule.notArrangeList.isNotEmpty()) {
      output("注意！以下课程尚未排课，请在排课后重新导入课表：")
      schedule.notArrangeList.forEach {
        output(it.courseName, false)
      }
    }

    output("正在获取考试安排…")
    val exams = JwappRepository.getExamArrangement(termId)
    output("获取到 ${exams.arranged.size} 次已安排的考试。", false)

    output("正在解析课程和考试安排…")
    val parser = CourseImportParser.create()
    val courses = parser.parseCourses(schedule.arrangedList)
    val parsedExams = parser.parseExams(exams.arranged, courses)
    output("解析完成，共 ${courses.size} 条课程安排、${parsedExams.size} 次考试", false)
    currentCoroutineContext().ensureActive()
    CourseRepository.replaceManagedCourses(termStart, courses, parsedExams)
    output("本学期课表导入完成。")
  }
}
