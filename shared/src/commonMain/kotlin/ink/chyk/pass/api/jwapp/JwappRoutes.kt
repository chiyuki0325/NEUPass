package ink.chyk.pass.api.jwapp

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.json.*

internal const val JWAPP_BASE_URL = "https://jwxt.neu.edu.cn/jwapp"

/**
 * 教务业务路由的基类，定义了所有教务相关的 API 调用。
 * [http] 由子类决定是直连客户端还是 WebVPN 客户端
 */
internal abstract class JwappRoutes(
  private val http: HttpClient,
  private val baseUrl: String = JWAPP_BASE_URL,
) {
  suspend fun getCurrentUser(): CurrentUserResponse =
    http.get("$baseUrl/sys/homeapp/api/home/currentUser.do").body()

  suspend fun getCurrentTermId(): String = getCurrentUser().welcomeInfo.xnxqdm

  suspend fun getTerms(): List<JwappTerm> =
    http.get("$baseUrl/sys/homeapp/api/home/kb/xnxq.do").body()

  // 获取有成绩信息的学期列表，用于查询成绩
  suspend fun getGradeTerms(): GradeTermsPage =
    http.post("$baseUrl/sys/cjzhcxapp/modules/wdcj/cxwdcjxnxq.do")
      .body<GradeTermsResponse>().cxwdcjxnxq

  suspend fun getGpaSummary(): GpaSummary =
    http.post("$baseUrl/sys/cjzhcxapp/api/wdcj/queryPjxfjd.do")
      .body<GpaSummaryResponse>().queryPjxfjd

  suspend fun getGrades(termId: String? = null, termName: String? = null): GradesPage {
    val querySetting = buildJsonArray {
      add(buildJsonObject {
        put("name", "XNXQDM")
        put("caption", "获得学年学期")
        put("linkOpt", "AND")
        put("builderList", "cbl_m_List")
        put("builder", "m_value_equal")
        put("value", termId)
        put("value_display", termName)
      })
    }
    return http.submitForm(
      "$baseUrl/sys/cjzhcxapp/modules/wdcj/cxwdcj.do",
      formParameters = Parameters.build {
        if (termId != null && termName != null) {
          append("querySetting", querySetting.toString())
        }
      },
    ).body<GradesResponse>().cxwdcj
  }

  suspend fun getScheduledCampuses(termId: String): List<ScheduledCampus> =
    http.submitForm("$baseUrl/sys/kbapp/api/wdkbcx/getMyScheduledCampus.do") {
      parameter("XNXQDM", termId)
    }.body<ScheduledCampusResponse>().getMyScheduledCampus

  suspend fun getTermWeeks(termId: String): List<TermWeek> =
    http.get("$baseUrl/sys/homeapp/api/home/getTermWeeks.do") {
      parameter("termCode", termId)
    }.body()

  suspend fun getScheduleDetail(termId: String, campusId: String): ScheduleDetail =
    http.submitForm("$baseUrl/sys/kbapp/api/wdkbcx/getMyScheduleDetail.do") {
      parameter("XNXQDM", termId)
      parameter("XQDM", campusId)
    }.body<GetScheduleDetailResponse>().getMyScheduleDetail

  suspend fun getExamArrangement(termId: String): ExamArrangement =
    http.submitForm("$baseUrl/sys/wdkwapp/api/wdks/queryMyExamArrangeMent.do") {
      parameter("XNXQDM", termId)
    }.body<QueryExamArrangementResponse>().queryMyExamArrangeMent
}
