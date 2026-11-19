package ink.chyk.pass.features.courses.editor

import androidx.lifecycle.*
import com.tencent.tmm.kmmresource.resource.*
import dev.darkokoa.pangu.*
import ink.chyk.pass.models.*
import ink.chyk.pass.repositories.*
import ink.chyk.pass.resources.*
import ink.chyk.pass.services.courses.*
import ink.chyk.pass.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class CourseEditViewModel(
  val courseKeyArg: String? = null,
  val dayIndexArg: Int? = null,
  initialWeeks: Int = 0,
) : ViewModel() {
  private val _title = MutableStateFlow<StringResource>(
    if (courseKeyArg == null) MR.strings.new_course else MR.strings.edit_course,
  )
  val title: StateFlow<StringResource> = _title

  private val _courseKey = MutableStateFlow(courseKeyArg)
  val courseKey: StateFlow<String?> = _courseKey

  private val _courseItem = MutableStateFlow<Course?>(null)
  val courseItem: StateFlow<Course?> = _courseItem

  private val _dayOfWeek = MutableStateFlow(dayIndexArg)
  val dayOfWeek: StateFlow<Int?> = _dayOfWeek

  private val _courseName = MutableStateFlow<String?>(null)
  val courseName: StateFlow<String?> = _courseName

  private val _location = MutableStateFlow<String?>(null)
  val location: StateFlow<String?> = _location

  private val _courseGenre = MutableStateFlow<CourseGenre?>(null)
  val courseGenre: StateFlow<CourseGenre?> = _courseGenre

  private val _weeks = MutableStateFlow(initialWeeks)
  val weeks: StateFlow<Int> = _weeks

  private val _period = MutableStateFlow<CoursePeriod?>(null)
  val period: StateFlow<CoursePeriod?> = _period

  private val _startSection = MutableStateFlow(1)
  val startSection: StateFlow<Int> = _startSection

  private val _startTime = MutableStateFlow<String?>(null)
  val startTime: StateFlow<String?> = _startTime

  private val _endSection = MutableStateFlow(2)
  val endSection: StateFlow<Int> = _endSection

  private val _endTime = MutableStateFlow<String?>(null)
  val endTime: StateFlow<String?> = _endTime

  private val _campus = MutableStateFlow<Campus?>(null)
  val campus: StateFlow<Campus?> = _campus

  private val _teacher = MutableStateFlow<String?>(null)
  val teacher: StateFlow<String?> = _teacher

  private val _credit = MutableStateFlow<String?>(null)
  val credit: StateFlow<String?> = _credit

  private val _valueChanged = MutableStateFlow(false)
  val valueChanged: StateFlow<Boolean> = _valueChanged
  private val _saving = MutableStateFlow(false)
  val saving: StateFlow<Boolean> = _saving

  init {
    if (_courseKey.value != null) {
      val item = CourseRepository.getCourseByKey(requireNotNull(_courseKey.value))
      if (item != null) {
        _courseItem.value = item
        _courseName.value = item.name
        _location.value = item.location
        _weeks.value = item.weeks
        _period.value = item.period
        _startSection.value = item.startSection
        _startTime.value = courseStartTime(item.campus, item.startSection)
        _endSection.value = item.endSection
        _endTime.value = courseEndTime(item.campus, item.endSection)
        _campus.value = item.campus
        _dayOfWeek.value = item.dayOfWeek
        _courseGenre.value = item.genre
        _teacher.value = item.teacher
        _credit.value = item.credit
      }
    } else {
      updateSt()
      updateEn()
    }
  }

  fun setCourseName(name: String) {
    _courseName.value = name
    _valueChanged.value = true
  }

  fun setRoomName(name: String) {
    _location.value = name
    _valueChanged.value = true
  }

  fun setCampus(campus: Campus?) {
    _campus.value = campus
    _valueChanged.value = true
    updateSt()
    updateEn()
  }

  fun setTeacher(teacher: String) {
    _teacher.value = teacher
    _valueChanged.value = true
  }

  fun setCredit(credit: String) {
    // 限制只能输入合法的学分
    var hasDecimalPoint = false
    credit.forEach {
      if (it == '.') {
        if (hasDecimalPoint) {
          // 已经有一个小数点了，不能再有了
          return
        }
        hasDecimalPoint = true
      } else if (!it.isDigit()) {
        // 不是数字也不是小数点，不能输入
        return
      }
    }
    _credit.value = credit
    _valueChanged.value = true
  }

  fun setSt(st: Int) {
    _startSection.value = st
    _valueChanged.value = true
    updateSt()
  }

  private fun updateSt() {
    _startTime.value = courseStartTime(_campus.value, _startSection.value)
  }

  fun setEn(en: Int) {
    _endSection.value = en
    _valueChanged.value = true
    updateEn()
  }

  private fun updateEn() {
    _endTime.value = courseEndTime(_campus.value, _endSection.value)

    val period = if (_endSection.value <= 4) {
      // 早间课程
      CoursePeriod.MORNING
    } else if (_endSection.value <= 8) {
      // 下午课程
      CoursePeriod.AFTERNOON
    } else {
      // 晚间课程
      CoursePeriod.EVENING
    }
    _period.value = period
  }

  fun toggleWeek(index: Int) {
    // 把 weeks 的第 n-1 位取反
    val currentWeeks = _weeks.value
    val newWeeks = currentWeeks xor (1 shl (index - 1))
    _weeks.value = newWeeks
    _valueChanged.value = true
  }

  fun setWeekDayDelta(dayIndex: Int) {
    _dayOfWeek.value = dayIndex
    _valueChanged.value = true
  }

  fun setCourseGenre(genre: CourseGenre) {
    _courseGenre.value = genre
    _valueChanged.value = true
  }

  suspend fun saveCourse(): CourseEditResult {
    // 验证数据
    if (_saving.value || _courseName.value.isNullOrBlank()) {
      return CourseEditResult.SAVE_FAILED
    }
    if (_location.value.isNullOrBlank()) {
      return CourseEditResult.SAVE_FAILED
    }
    if (_credit.value?.contains("e") == true || (_credit.value?.toDoubleOrNull() == null)) {
      return CourseEditResult.SAVE_FAILED
    }
    if (_weeks.value == 0 || _dayOfWeek.value !in 0..6 ||
      _startSection.value !in 1..12 || _endSection.value !in _startSection.value..12
    ) return CourseEditResult.SAVE_FAILED

    val previous = _courseItem.value
    if (_courseKey.value != null && previous == null) return CourseEditResult.SAVE_FAILED
    val course = Course(
      key = previous?.key.orEmpty(),
      name = Pangu.spacingText(requireNotNull(_courseName.value)),
      location = Pangu.spacingText(requireNotNull(_location.value)),
      weeks = _weeks.value,
      dayOfWeek = requireNotNull(_dayOfWeek.value),
      period = _period.value ?: CoursePeriod.MORNING,
      startSection = _startSection.value,
      endSection = _endSection.value,
      genre = _courseGenre.value,
      campus = _campus.value,
      teacher = Pangu.spacingText(_teacher.value.orEmpty()),
      credit = _credit.value,
      color = previous?.color ?: ColorUtils.calcColor(requireNotNull(_courseName.value)),
      code = previous?.code,
      serial = previous?.serial,
      classes = previous?.classes,
    )
    _saving.value = true
    return try {
      if (previous != null) {
        if (CourseRepository.updateCourse(course)) CourseEditResult.SAVED else CourseEditResult.SAVE_FAILED
      } else {
        val customCourse = course.copy(key = courseKey(course))
        if (CourseRepository.getCourseByKey(customCourse.key) != null) {
          CourseEditResult.ALREADY_EXISTS
        } else if (CourseRepository.addCustomCourse(customCourse)) {
          CourseEditResult.SAVED
        } else {
          CourseEditResult.SAVE_FAILED
        }
      }
    } catch (error: CancellationException) {
      throw error
    } catch (error: Exception) {
      AppLog.e("CourseEdit", "保存课程失败：${error.message}")
      CourseEditResult.SAVE_FAILED
    } finally {
      _saving.value = false
    }
  }

  suspend fun deleteCourse(): CourseEditResult {
    val key = _courseKey.value ?: return CourseEditResult.DELETE_FAILED
    if (_saving.value) return CourseEditResult.DELETE_FAILED
    _saving.value = true
    return try {
      if (CourseRepository.deleteCourse(key)) CourseEditResult.DELETED else CourseEditResult.DELETE_FAILED
    } catch (error: CancellationException) {
      throw error
    } catch (error: Exception) {
      AppLog.e("CourseEdit", "删除课程失败：${error.message}")
      CourseEditResult.DELETE_FAILED
    } finally {
      _saving.value = false
    }
  }

  private fun courseKey(course: Course): String = buildString {
    arrayOf<Any>(course.name, course.weeks, course.location, course.startSection, course.dayOfWeek)
      .forEach { append(it.hashCode().toUInt().toString(16)) }
  }
}

enum class CourseEditResult {
  SAVED,
  DELETED,
  ALREADY_EXISTS,
  SAVE_FAILED,
  DELETE_FAILED,
}
