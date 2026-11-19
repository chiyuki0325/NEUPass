@file:OptIn(kotlin.time.ExperimentalTime::class)

package ink.chyk.pass.api.campus_run

import ink.chyk.pass.*
import ink.chyk.pass.api.CampusApp
import ink.chyk.pass.api.createApiClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlin.time.Clock

private val LOGIN_URL = CampusApp.CAMPUS_RUN.callbackUrl
private const val API_HOST = "https://tybzhtypt.neu.edu.cn/v3/api.php"
private const val START_RUN_URL =
  "https://tybzhtypt.neu.edu.cn/bdlp_h5_fitness_test/view/db/#/pages/run/index"

// 步道乐跑 API
class CampusRunApi {
  // 登录客户端：处理统一认证回调与会话重定向。
  private val loginHttp = createApiClient {
    followRedirects = false
    expectSuccess = false
  }

  // 业务客户端：步道乐跑业务请求
  private val http = createApiClient {
    followRedirects = false
    expectSuccess = false
    install(CampusRunPlugin)
  }

  suspend fun login(appTicket: String): CampusRunSession {
    val ticketResponse = loginHttp.get(LOGIN_URL) {
      parameter("ticket", appTicket)
    }
    if (ticketResponse.status != HttpStatusCode.Found) {
      if (ticketResponse.status.value >= 500) throw RequestFailedException(LOGIN_URL)
      throw TicketExpiredException()
    }

    // 以 ST 作为 PHPSESSID 再次访问回调，从重定向地址中提取会话参数。
    val response = loginHttp.get(LOGIN_URL) {
      cookie("PHPSESSID", appTicket)
    }
    if (response.status.value >= 500) throw RequestFailedException(LOGIN_URL)
    val location = response.headers[HttpHeaders.Location]
    if (response.status.value !in 300..399 || location == null) throw TicketExpiredException()
    val query = parseQueryString(location.substringAfter('?'))
    return CampusRunSession(
      uid = query["uid"] ?: throw TicketExpiredException(),
      token = query["token"] ?: throw TicketExpiredException(),
      schoolId = query["school_id"] ?: throw TicketExpiredException(),
      termId = query["term_id"] ?: throw TicketExpiredException(),
      courseId = query["course_id"]?.toIntOrNull() ?: throw TicketExpiredException(),
      classId = query["class_id"]?.toInt() ?: 0,
      studentNum = query["student_num"] ?: throw TicketExpiredException(),
      cardId = query["card_id"] ?: throw TicketExpiredException(),
    )
  }

  // 获取首页的用户资料、学校信息与功能入口。
  suspend fun getIndex(session: CampusRunSession): WpIndexResponse =
    http.post("$API_HOST/WpIndex/Index") { setBody(session) }.body()

  // 获取跑步区域、跑步须知、本周里程与今日跑步人数。
  suspend fun getBeforeRun(session: CampusRunSession): BeforeRunResponse =
    http.post("$API_HOST/Run2/beforeRunV260") { setBody(session) }.body()

  // 获取当前学期的跑步次数、里程与记录列表。
  suspend fun getTermRunRecord(session: CampusRunSession): GetTermRunRecordResponse =
    http.post("$API_HOST/WpRun/getTermRunRecord") { setBody(session) }.body()

  // 拼接乐跑 H5 跑步页面地址，携带会话参数、秒级时间戳与登录类型。
  fun getStartRunUrl(session: CampusRunSession): String {
    val query = parameters {
      append("uid", session.uid)
      append("token", session.token)
      append("school_id", session.schoolId)
      append("term_id", session.termId)
      append("course_id", session.courseId.toString())
      append("class_id", session.classId.toString())
      append("student_num", session.studentNum)
      append("card_id", session.cardId)
      append("timestamp", (Clock.System.now().toEpochMilliseconds() / 1000).toString())
      append("login_type", "4")
    }.formUrlEncode()
    return "$START_RUN_URL?$query"
  }

  // 释放登录与业务客户端资源。
  fun close() {
    loginHttp.close()
    http.close()
  }
}
