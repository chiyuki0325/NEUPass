package ink.chyk.pass.models

import ink.chyk.pass.api.jwapp.CourseGrade
import kotlinx.serialization.Serializable

@Serializable
data class StoredGrade(
  val grade: CourseGrade,
  val storedAt: Long,
)
