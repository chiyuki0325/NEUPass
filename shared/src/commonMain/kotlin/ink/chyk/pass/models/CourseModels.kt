package ink.chyk.pass.models

import com.tencent.tmm.kmmresource.resource.ImageResource
import ink.chyk.pass.resources.MR
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * 本地存储的课程数据模型
 * 按「课程安排」为单位存储
 * 同一门课程的不同安排会生成不同的 Course 实例
 * 如某门课在周二和周四都有课，则会生成两条不同 Course，dayOfWeek、section、key 不同
 */
@Serializable
data class Course(
  val key: String,            // 根据哈希码生成的查询 key
  val name: String,
  val location: String,       // 展开好的地点，如 南湖校区 一号 A103，何世礼 A104
  val campus: Campus?,
  val weeks: Int,             // bitmap，第 0 位对应第 1 周以此类推，参见 CourseImportParser.kt:parseWeeks
  val dayOfWeek: Int,         // 0-6，周日为 0
  val startSection: Int,      // 开始节次，1-12
  val endSection: Int,        // 结束节次。start=1 end=2 表示第 1-2 节
  val code: String? = null,   // A 开头课程编号
  val serial: String? = null, // A 开头开课轮次编号
  val credit: String? = null, // 学分
  val classes: String? = null,// 上课班级
  val teacher: String,
  val color: Int,             // 教务系统下发或根据哈希生成
  val genre: CourseGenre? = null,
  val period: CoursePeriod,
)


/**
 * 从 Course 渲染而来的结果，持久化储存，用于 UI 直接展示
 * 按「天」为单位存储，一天有多条课程数据
 * 日期是 repo 的 key，RenderedCourse 是 value，所以不用再存储日期字段
 */
@Serializable
data class RenderedCourse(
  val name: String,
  val location: String,
  val campus: String?,
  val start: String,
  val end: String,
  val period: CoursePeriod,
  val genre: CourseGenre,
  val startSection: Int,
  val endSection: Int,
  val key: String,  // 用于摸到对应的 Course
  val color: Int,
  val teacher: String,
)

enum class CoursePeriod {
  MORNING, AFTERNOON, EVENING
}

enum class CourseGenre(
  val displayName: String,
  val iconResource: ImageResource,
) {
  SCIENCE("数学与自然科学类", MR.images.ic_fluent_math_format_professional_24_regular),
  HUMANITIES("人文社会科学类", MR.images.ic_fluent_book_open_globe_24_regular),
  GENERAL("通识选修类", MR.images.ic_fluent_lightbulb_filament_24_regular),
  PROFESSIONAL("专业方向类", MR.images.ic_fluent_arrow_trending_wrench_24_regular),
  EXPERIMENTAL("实践类", MR.images.ic_fluent_beaker_edit_24_regular),
  OTHER("其他", MR.images.ic_fluent_whiteboard_24_regular),
}

enum class Campus(
  val campusName: String,
  val fullName: String,
) {
  NANHU("南湖", "南湖校区"),
  HUNNAN("浑南", "浑南校区"),
}

@Serializable
data class Exam(
  val name: String,
  val type: String,
  val date: String,
  val start: String,
  val end: String,
  val location: String,
  val seat: String,
  val remark: String?,
  val genre: CourseGenre,
  val color: Int,
)

// JSON 中不好存储 *Local* Date/Time，所以在 Exam 中使用 String，渲染后再转为 LocalDate/LocalDateTime
data class RenderedExam(
  val name: String,
  val type: String,
  val date: LocalDate,
  val start: LocalDateTime,
  val end: LocalDateTime,
  val location: String,
  val seat: String,
  val remark: String?,
  val genre: CourseGenre,
  val color: Int,
)
