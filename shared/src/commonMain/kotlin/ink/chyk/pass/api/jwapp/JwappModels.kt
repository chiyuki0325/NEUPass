package ink.chyk.pass.api.jwapp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 教务系统统一响应信封 {"code": "0", "msg": "...", "datas": ...}。
// 请务必开启 IgnoreUnknownKeys：同一接口返回的字段远多于这里用到的部分。
@Serializable
internal data class JwappResponse<T>(
  val msg: String? = null,
  val code: String = "0",
  val datas: T? = null,
)

// currentUser.do 的 datas
@Serializable
data class CurrentUserResponse(
  val welcomeInfo: WelcomeInfo,
)

@Serializable
data class JwappTerm(
  val selected: Boolean?,
  val itemName: String,
  val itemCode: String,
)

@Serializable
internal data class GradeTermsResponse(
  val cxwdcjxnxq: GradeTermsPage,
)

@Serializable
data class GradeTermsPage(
  val totalSize: Int,
  val pageSize: Int,
  val rows: List<GradeTerm>,
)

@Serializable
data class GradeTerm(
  @SerialName("SFDQXNXQ") val isCurrentTerm: String,
  @SerialName("XNXQDM") val termId: String,
  @SerialName("XNXQMC") val termName: String,
)

@Serializable
internal data class GpaSummaryResponse(
  val queryPjxfjd: GpaSummary,
)

@Serializable
data class GpaSummary(
  @SerialName("ZPJXFJDNEW") val newAverageGpa: String,
  @SerialName("ZPJXFJDPM") val averageGpaRank: String,
  @SerialName("ZPJXFJD") val averageGpa: String,
)

@Serializable
internal data class GradesResponse(
  val cxwdcj: GradesPage,
)

@Serializable
data class GradesPage(
  val totalSize: Int,
  val pageSize: Int,
  val rows: List<CourseGrade>,
)

@Serializable
data class CourseGrade(
  @SerialName("WID") val id: String, // WID
  @SerialName("KCM") val courseName: String, // 课程名
  @SerialName("KCH") val courseCode: String, // 课程号
  @SerialName("XF") val credits: Double, // 学分
  @SerialName("XSZCJ") val totalGrade: String, // 总成绩
  @SerialName("JD") val gpa: String, // 绩点
  @SerialName("SFYX") val isValid: String, // 是否有效
  @SerialName("SFYX_DISPLAY") val isValidDisplay: String, // 是否有效
  @SerialName("CXCKDM") val retakeCode: String, // 重修重考
  @SerialName("CXCKDM_DISPLAY") val retakeName: String, // 重修重考
  @SerialName("XNXQDM") val termId: String, // 获得学年学期
  @SerialName("XNXQDM_DISPLAY") val termName: String, // 获得学年学期
  @SerialName("KCLBDM") val courseCategoryCode: String, // 课程类别
  @SerialName("KCLBDM_DISPLAY") val courseCategoryName: String, // 课程类别
  @SerialName("KCXZDM") val courseNatureCode: String, // 课程性质
  @SerialName("KCXZDM_DISPLAY") val courseNatureName: String, // 课程性质
  @SerialName("XGXKLBDM") val generalElectiveCategoryCode: String?, // 通识选修课类别
  @SerialName("XGXKLBDM_DISPLAY") val generalElectiveCategoryName: String, // 通识选修课类别
  @SerialName("KSLXDM") val examTypeCode: String, // 考试类型
  @SerialName("KSLXDM_DISPLAY") val examTypeName: String, // 考试类型
  @SerialName("TDKCM") val replacementCourseName: String?, // 替代课程名
  @SerialName("TDKCH") val replacementCourseCode: String?, // 替代课程号
  @SerialName("CJRDFSDM") val gradeRecognitionMethodCode: String?, // 成绩认定方式
  @SerialName("CJRDFSDM_DISPLAY") val gradeRecognitionMethodName: String, // 成绩认定方式
  @SerialName("SHZT") val reviewStatus: String?, // 审核状态
  @SerialName("FCSQ") val reviewApplication: Boolean, // 复查申请
  @SerialName("FCZ") val reviewInProgress: Boolean, // 复查中
  @SerialName("FCJG") val reviewResult: Boolean, // 复查结果
  @SerialName("FXCJ") val hasComponentGrades: Boolean, // 分项成绩
  @SerialName("WPJ") val notEvaluated: Boolean, // 未评教
  @SerialName("SFJG") val isPassed: String, // 是否及格
  @SerialName("SFJG_DISPLAY") val isPassedDisplay: String, // 是否及格
  @SerialName("KCKXQ") val canViewDetails: Boolean, // 可查看详情
  @SerialName("FCWID") val reviewId: String?, // 复查表WID
  @SerialName("WKFMS") val unavailableDescription: String?, // 未开放描述
  @SerialName("CJRDKC") val gradeRecognitionCourse: String?, // 成绩认定课程
)

@Serializable
data class WelcomeInfo(
  // 当前学期编号（如 2025-2026-1），即「学年学期代码」
  val xnxqdm: String,
)

// getMyScheduledCampus.do 的 datas
@Serializable
internal data class ScheduledCampusResponse(
  val getMyScheduledCampus: List<ScheduledCampus>,
)

@Serializable
data class ScheduledCampus(
  val name: String,
  val id: String,
)

// getTermWeeks.do 的 datas 直接是周的数组
@Serializable
data class TermWeek(
  val startDate: String,
  val name: String,
  val serialNumber: Int,
)

// getMyScheduleDetail.do 的 datas
@Serializable
internal data class GetScheduleDetailResponse(
  val getMyScheduleDetail: ScheduleDetail,
)

@Serializable
data class ScheduleDetail(
  val notArrangeList: List<JwappCourseDetail>,
  val practiceList: List<JwappCourseDetail>,
  val arrangedList: List<JwappCourseDetail>,
)

@Serializable
data class JwappCourseDetail(
  val courseName: String,
  val courseCode: String? = "暂无",
  val courseSerialNo: String? = "暂无",
  val teachClassId: String? = "暂无",
  val byCode: String? = "暂无",
  val credit: String? = "位置",

  // 时间与地点
  val dayOfWeek: Int? = null,
  val beginTime: String? = null,
  val endTime: String? = null,
  val beginSection: Int? = null,
  val endSection: Int? = null,
  val campusName: String? = null,
  val placeName: String? = null,

  // 教学人员与周期
  val weeksAndTeachers: String? = null,
  val teachingTarget: String? = null,
  val teachClassName: String? = null,

  // 列表与详情
  val cellDetail: List<CellDetail> = emptyList(),
  val titleDetail: List<String> = emptyList(),
  val cellWeekTeacherClassroomDetail: List<CellDetail> = emptyList(),
  val titleWeekTeacherClassroomDetail: List<String> = emptyList(),
  val tags: List<CourseDetailTag> = emptyList(),

  val color: String? = null,
)

@Serializable
data class CellDetail(
  val text: String,
)

@Serializable
data class CourseDetailTag(
  val text: String,
  val color: String? = null,
)

// queryMyExamArrangeMent.do 的 datas
@Serializable
internal data class QueryExamArrangementResponse(
  val queryMyExamArrangeMent: ExamArrangement,
)

@Serializable
data class ExamArrangement(
  val arranged: List<ExamDetail>,
)

@Serializable
data class ExamDetail(
  @SerialName("KCM") val name: String,
  @SerialName("KCH") val code: String,
  @SerialName("KSLXDM_DISPLAY") val type: String,
  @SerialName("KSSJ") val start: String,
  @SerialName("JSSJ") val end: String,
  @SerialName("KSRQ") val date: String,
  @SerialName("JASMC") val location: String,
  @SerialName("ZWH") val seat: Int,
  @SerialName("KSSM") val remark: String?,
)
