package ink.chyk.pass.api.campus_run

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CampusRunSession(
  val uid: String,
  val token: String,
  @SerialName("school_id") val schoolId: String,
  @SerialName("term_id") val termId: String,
  @SerialName("course_id") val courseId: Int,
  @SerialName("class_id") val classId: Int,
  @SerialName("student_num") val studentNum: String,
  @SerialName("card_id") val cardId: String,
) {
  internal fun toRequest(nonce: String, timestamp: Long): CampusRunRequest =
    CampusRunRequest(
      uid = uid,
      token = token,
      schoolId = schoolId,
      termId = termId,
      courseId = courseId,
      classId = classId,
      studentNum = studentNum,
      cardId = cardId,
      nonce = nonce,
      timestamp = timestamp,
    )
}

@Serializable
internal data class CampusRunRequest(
  val uid: String,
  val token: String,
  @SerialName("school_id") val schoolId: String,
  @SerialName("term_id") val termId: String,
  @SerialName("course_id") val courseId: Int,
  @SerialName("class_id") val classId: Int,
  @SerialName("student_num") val studentNum: String,
  @SerialName("card_id") val cardId: String,
  val timestamp: Long,
  val version: Int = 1,
  val nonce: String,
  val ostype: String = "5",
  val role: Int = 1,
)
