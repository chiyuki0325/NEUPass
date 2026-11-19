package ink.chyk.pass.services.courses

import dev.darkokoa.pangu.Pangu
import ink.chyk.pass.api.jwapp.ExamDetail
import ink.chyk.pass.api.jwapp.JwappCourseDetail
import ink.chyk.pass.models.Campus
import ink.chyk.pass.models.CourseGenre
import ink.chyk.pass.models.Course
import ink.chyk.pass.models.CoursePeriod
import ink.chyk.pass.models.Exam
import ink.chyk.pass.models.Trie
import ink.chyk.pass.repositories.CourseRepository
import ink.chyk.pass.utils.ColorUtils

/**
 * 将教务接口返回的已排课表和考试安排转换为本地 Course/Exam 数据模型。
 */
internal class CourseImportParser private constructor(
  private val genres: Trie,
) {
  companion object {
    // genre map：采用字典树实现，存储课程编号 A0xxx 到课群（课程分类）的映射
    // 新教务不再提供查询课群的接口，因此我们暂时本地存一份
    // 数据来自旧树维教务（eams）
    // TODO：培养计划接口能拿到课群吗？

    // loadGenreMap 是异步 IO，因此本类必须异步创建
    suspend fun create(): CourseImportParser =
      CourseImportParser(CourseRepository.loadGenreMap())
  }

  private fun getCourseGenre(code: String): CourseGenre? =
    genres[code]?.let { CourseGenre.entries.getOrNull(it) }

  // 提取第几周 / 是否单双周（某些学院有单双周的情况）
  private val weekRegex = Regex("""(\d+)(?:-(\d+))?周(?:\(([单双])\))?""")
  // 用于去除教师信息中多余的中括号标记
  private val teacherRegex = Regex("""\[.*?]""")

  fun parseCourses(details: List<JwappCourseDetail>): List<Course> =
    details.flatMap { detail ->
      var name = Pangu.spacingText(detail.courseName)

      // 以结束节次划分上午、下午或晚间
      val startSection = requireNotNull(detail.beginSection)
      val endSection = requireNotNull(detail.endSection)
      val period = when {
        endSection <= 4 -> CoursePeriod.MORNING
        endSection <= 8 -> CoursePeriod.AFTERNOON
        else -> CoursePeriod.EVENING
      }

      // 教师信息取斜杠后的文本，去除方括号标记；没有教师信息时保留为空字符串。
      val teacher = Pangu.spacingText(
        detail.weeksAndTeachers?.substringAfter("/")?.replace(teacherRegex, "")?.trim().orEmpty()
      )

      // 课程单元格颜色，优先采用教务系统下发的颜色，未提供或无法解析时根据课程名称生成颜色
      val color = detail.color?.let {
        runCatching { parseColor(it) }.getOrNull()
      } ?: ColorUtils.calcColor(name)

      val campus = Campus.entries.find { it.fullName == detail.campusName }

      // 实验课由名称开头的 [实] 标记识别。
      val isExperiment = name.startsWith("[实]")

      // titleDetail 为教务系统里课程 tooltip，为多行纯文本
      // 首行作为标题跳过，其余项为课程排课安排

      // NEUPass 数据建模把同一门课程的不同排课视为不同的 Course 对象
      // 如：某一门课程可能在周二上午和周四下午都有排课
      // 我们就分别存两个不同 Course

      // 从教务排课数据中，筛选出包含有效周次的条目，再在同一条详情内按周次位图合并，周次相同时保留最后一条。
      detail.titleDetail.drop(1).mapNotNull { arrangement ->
        val weeks = parseWeeks(arrangement)
        if (weeks == 0) return@mapNotNull null

        // 有校区名称时，取其后的内容作为地点并移除重复校区名（如 浑南校区 一号A103 -> 一号A103）
        // 否则取最后一个空格后的文本
        var location = if (detail.campusName != null) {
          arrangement.substringAfter(detail.campusName).replace(detail.campusName, "").trim()
        } else {
          arrangement.substringAfterLast(" ").trim()
        }

        // 对 location 进行后处理，如 教A103* -> 何世礼 A103
        location = Pangu.spacingText(
          location.ifEmpty { "未安排地点" }.replace("*", "")
        ).let {
          if (campus == Campus.NANHU && !isExperiment) fixNanhuLocation(it) else it
        }

        name = if (isExperiment) fixExperimentalName(name) else name

        // [停课] 等标签
        detail.tags.forEach { name = "[${it.text}] $name" }

        val dayOfWeek = requireNotNull(detail.dayOfWeek) % 7

        // 采用哈希码生成课程 key（以 course_v2_item_$key 存储）
        val key = buildString {
          listOf(name, weeks, location, startSection, dayOfWeek)
            .forEach { append(it.hashCode().toUInt().toString(16)) }
        }

        Course(
          key = key,
          name = name,
          location = location,
          campus = campus,
          weeks = weeks,
          dayOfWeek = dayOfWeek,
          startSection = startSection,
          endSection = endSection,
          code = detail.courseCode,
          serial = detail.courseSerialNo,
          credit = detail.credit,
          classes = detail.teachingTarget?.split("，")?.sorted()?.joinToString(", ") ?: "全校",
          teacher = teacher,
          // 停课安排保留记录但显示为灰色
          color = if (location == "停课") 0xFF888888.toInt() else color,
          genre = if (isExperiment) CourseGenre.EXPERIMENTAL // 实验小烧瓶图标
          else getCourseGenre(detail.courseCode.orEmpty()),  // 课群图标
          period = period,
        )
      }.associateBy { it.weeks }.values
    }

  // 课程解析完成后，再按课程代码关联考试，以复用课程的校区和颜色信息。
  fun parseExams(
    details: List<ExamDetail>,
    courses: List<Course>,
  ): List<Exam> {
    val courseAssoc = courses.associateBy { it.code }

    return details.map { detail ->
      val course = courseAssoc[detail.code]
      val location = Pangu.spacingText(detail.location)
      Exam(
        name = detail.name,
        type = detail.type,
        date = detail.date,
        start = detail.start,
        end = detail.end,
        location = if (course?.campus == Campus.NANHU) fixNanhuLocation(location) else location,
        seat = detail.seat.toString(),
        remark = detail.remark,
        genre = getCourseGenre(detail.code) ?: CourseGenre.OTHER,
        color = course?.color ?: ColorUtils.calcColor(detail.name),
      )
    }.sortedWith(compareBy<Exam> { it.date }.thenBy { it.start })
  }

  // 以 bitmap 格式存储课程，第 0 位对应第 1 周以此类推
  private fun parseWeeks(arrangement: String): Int {
    var weeks = 0
    weekRegex.findAll(arrangement).forEach { match ->
      // 输入形如 1-5单
      val start = match.groupValues[1].toInt()
      val end = match.groupValues[2].takeIf { it.isNotEmpty() }?.toInt() ?: start
      val type = match.groupValues[3]  // 单双周
      for (week in start..end) {
        if (type == "单" && week % 2 == 0 || type == "双" && week % 2 != 0) continue
        weeks = weeks or (1 shl (week - 1))
      }
    }
    return weeks
  }

  // 展开南湖校区场地简称
  private fun fixNanhuLocation(location: String): String {
    val parts = location.split(" ")
    if (parts.size < 2) return location
    val building = when (parts.first()) {
      "机" -> "机电"
      "采" -> "采矿"
      "逸" -> "逸夫"
      "冶" -> "冶金"
      "教" -> "何世礼"
      else -> parts.first()
    }
    return "$building ${parts.drop(1).joinToString(" ")}"
  }

  // 去掉 [实] 前缀后，使用“课程名 - 项目名”的展示形式
  private fun fixExperimentalName(name: String): String {
    val parts = name.substring(3).trim().split(" - ")
    val courseName = parts.first()
    val experimentName = parts.drop(1).joinToString(" - ")
    return if (experimentName.startsWith(courseName)) experimentName
    else "$courseName - $experimentName"
  }

  // 教务系统发下来的颜色色值是 css 格式，这里展开成安卓/compose适用格式
  private fun parseColor(color: String): Int {
    val value = color.removePrefix("#")
    val argb = when (value.length) {
      3 -> "FF" + value.map { "$it$it" }.joinToString("")
      4 -> value.map { "$it$it" }.joinToString("")
      6 -> "FF$value"
      8 -> value
      else -> error("Unsupported color: $color")
    }
    return argb.toLong(16).toInt()
  }
}
