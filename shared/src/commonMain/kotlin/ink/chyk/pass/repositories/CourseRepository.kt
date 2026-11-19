package ink.chyk.pass.repositories

import com.ctrip.flight.mmkv.defaultMMKV
import ink.chyk.pass.models.Course
import ink.chyk.pass.models.Exam
import ink.chyk.pass.models.RenderedCourse
import ink.chyk.pass.models.Trie
import ink.chyk.pass.resources.MR
import ink.chyk.pass.services.courses.createCourseCalendar
import ink.chyk.pass.services.courses.renderCourses
import ink.chyk.pass.utils.DateTimeUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.serialization.json.Json

object CourseRepository : IRepository {
  private val mmkv by lazy { defaultMMKV() }
  private val dateFormatter = DateTimeUtils.basicDateFormat
  private val writeMutex = Mutex()

  // 从资源文件读取课程分类映射，每次创建新的 Trie，不常驻缓存
  suspend fun loadGenreMap(): Trie = withContext(Dispatchers.IO) {
    Trie().apply { deserialize(MR.files.course_code_icon_map.readText()) }
  }

  // 读取学期开始日期，未导入时返回 null
  fun getTermStart(): LocalDate? =
    if (mmkv.containsKey("course_v2_term_start")) {
      LocalDate.parse(mmkv.getString("course_v2_term_start"), dateFormatter)
    } else {
      null
    }

  // 按课程 key 读取原始课程数据，不存在时返回 null
  fun getCourseByKey(key: String): Course? {
    val storageKey = "course_v2_item_$key"
    return if (mmkv.containsKey(storageKey)) {
      Json.decodeFromString<Course>(mmkv.getString(storageKey))
    } else {
      null
    }
  }

  // 读取从教务系统导入的课程
  fun getManagedCourses(): List<Course> = getCoursesByType("managed")

  // 读取用户自定义课程
  fun getCustomCourses(): List<Course> = getCoursesByType("custom")

  // 根据 managed / custom 对应的 key 集合读取课程
  private fun getCoursesByType(type: String): List<Course> =
    mmkv.getStringSet("course_v2_${type}_keys").orEmpty().map { key ->
      Json.decodeFromString<Course>(mmkv.getString(key))
    }

  // 读取指定日期已渲染的课程，没有记录时返回空列表
  fun getCoursesByDate(date: LocalDate): List<RenderedCourse> {
    val key = renderedCourseKey(date)
    return if (mmkv.containsKey(key)) {
      Json.decodeFromString<List<RenderedCourse>>(mmkv.getString(key))
    } else {
      emptyList()
    }
  }

  fun getRenderedCourses(): Map<LocalDate, List<RenderedCourse>> =
    mmkv.getStringSet("course_v2_rendered_keys").orEmpty().associate { key ->
      LocalDate.parse(key.removePrefix("course_v2_rendered_"), dateFormatter) to
        Json.decodeFromString<List<RenderedCourse>>(mmkv.getString(key))
    }

  // 读取导入的考试安排，没有记录时返回空列表
  fun getExams(): List<Exam> =
    if (mmkv.containsKey("course_v2_exams")) {
      Json.decodeFromString<List<Exam>>(mmkv.getString("course_v2_exams"))
    } else {
      emptyList()
    }

  // 替换教务课程、考试和学期开始日期，保留自定义课程并一起重新渲染
  // 用于（重新）导入教务课表
  suspend fun replaceManagedCourses(
    termStart: LocalDate,
    managed: List<Course>,
    exams: List<Exam>,
  ) = withContext(Dispatchers.IO) {
    writeMutex.withLock {
      persistCourses(termStart, managed, getCustomCourses(), exams)
    }
  }

  // 新增自定义课程并重新渲染课表
  suspend fun addCustomCourse(course: Course): Boolean = withContext(Dispatchers.IO) {
    writeMutex.withLock {
      // 尚未导入课表时，返回 false
      val termStart = getTermStart() ?: return@withLock false
      if (getCourseByKey(course.key) != null) return@withLock false
      persistCourses(termStart, getManagedCourses(), getCustomCourses() + course, getExams())
      true
    }
  }

  // 按key更新课程，保留其归属，并重新渲染课表；学期或课程不存在时返回false
  suspend fun updateCourse(course: Course): Boolean = withContext(Dispatchers.IO) {
    writeMutex.withLock {
      val termStart = getTermStart() ?: return@withLock false
      val managed = getManagedCourses()
      val custom = getCustomCourses()
      if ((managed + custom).none { it.key == course.key }) return@withLock false
      persistCourses(
        termStart,
        managed.map { if (it.key == course.key) course else it },
        custom.map { if (it.key == course.key) course else it },
        getExams(),
      )
      true
    }
  }

  // 删除该key对应的整条课程安排，包括所有上课日期的渲染结果；学期或课程不存在时返回false
  suspend fun deleteCourse(key: String): Boolean = withContext(Dispatchers.IO) {
    writeMutex.withLock {
      val termStart = getTermStart() ?: return@withLock false
      val managed = getManagedCourses()
      val custom = getCustomCourses()
      if ((managed + custom).none { it.key == key }) return@withLock false
      persistCourses(
        termStart,
        managed.filterNot { it.key == key },
        custom.filterNot { it.key == key },
        getExams(),
      )
      true
    }
  }

  // 将当前保存的全部每日课表导出为ICS文本，文件保存和分享由平台侧完成
  suspend fun exportCalendarString(): String = withContext(Dispatchers.IO) {
    writeMutex.withLock {
      createCourseCalendar(getRenderedCourses())
    }
  }

  /**
   * 保存完整课表，并将教务课程和自定义课程一起渲染为每日数据。
   * 修改周次、日期或删除课程后，原日期可能已经没有课，因此连同旧日期索引一起替换。
   * 调用方持有writeMutex，串行执行读取、修改和保存。
   */
  private fun persistCourses(
    termStart: LocalDate,
    managed: List<Course>,
    custom: List<Course>,
    exams: List<Exam>,
  ) {
    val renderedCourses = renderCourses(termStart, managed + custom)
    val encodedManaged = managed.associate { "course_v2_item_${it.key}" to Json.encodeToString(it) }
    val encodedCustom = custom.associate { "course_v2_item_${it.key}" to Json.encodeToString(it) }
    val encodedRenderedCourses = renderedCourses.mapKeys { (date, _) -> renderedCourseKey(date) }
      .mapValues { (_, dailyCourses) -> Json.encodeToString(dailyCourses) }
    val encodedExams = Json.encodeToString(exams)

    mmkv.getStringSet("course_v2_managed_keys").orEmpty().forEach(mmkv::removeValueForKey)
    mmkv.getStringSet("course_v2_custom_keys").orEmpty().forEach(mmkv::removeValueForKey)
    mmkv.getStringSet("course_v2_rendered_keys").orEmpty().forEach(mmkv::removeValueForKey)
    encodedManaged.forEach { (key, value) -> mmkv[key] = value }
    encodedCustom.forEach { (key, value) -> mmkv[key] = value }
    encodedRenderedCourses.forEach { (key, value) -> mmkv[key] = value }
    mmkv["course_v2_managed_keys"] = encodedManaged.keys
    mmkv["course_v2_custom_keys"] = encodedCustom.keys
    mmkv["course_v2_rendered_keys"] = encodedRenderedCourses.keys
    mmkv["course_v2_term_start"] = termStart.format(dateFormatter)
    mmkv["course_v2_exams"] = encodedExams
  }

  // 清除课程、渲染结果、考试和学期信息
  override fun clearAllData() {
    listOf("managed", "custom", "rendered").forEach { type ->
      val key = "course_v2_${type}_keys"
      mmkv.getStringSet(key).orEmpty().forEach(mmkv::removeValueForKey)
      mmkv.removeValueForKey(key)
    }
    mmkv.removeValueForKey("course_v2_term_start")
    mmkv.removeValueForKey("course_v2_exams")
  }

  // 生成指定日期的渲染结果存储 key
  private fun renderedCourseKey(date: LocalDate): String =
    "course_v2_rendered_${date.format(dateFormatter)}"
}
