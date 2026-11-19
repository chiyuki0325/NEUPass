package ink.chyk.pass.features.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tencent.tmm.kmmresource.compose.stringResource
import ink.chyk.pass.LoadingState
import ink.chyk.pass.components.shimmerContent
import ink.chyk.pass.repositories.CacheResult
import ink.chyk.pass.resources.MR
import ink.chyk.pass.ui.success
import ink.chyk.pass.ui.warning

@Composable
fun JwappCard(mgr: JwappManager, onOpenJwapp: () -> Unit) {
  LaunchedEffect(Unit) {
    mgr.initJwapp()
  }

  val loadingState by mgr.loadingState.collectAsState()
  val gpaSummary by mgr.gpaSummary.collectAsState()
  val grades by mgr.grades.collectAsState()
  val summary = gpaSummary?.value
  val isLoading = loadingState == LoadingState.LOADING || gpaSummary is CacheResult.Cached
  val subtitle = when (loadingState) {
    LoadingState.LOADING, LoadingState.PARTIAL_SUCCESS -> AnnotatedString(stringResource(MR.strings.loading))
    LoadingState.FAILED -> AnnotatedString(stringResource(MR.strings.not_campus_network))
    LoadingState.SUCCESS -> AnnotatedString("")
  }

  AppCard(
    icon = MR.images.ic_fluent_hat_graduation_48_regular,
    title = stringResource(MR.strings.jwapp),
    subtitle = subtitle,
    subtitleMaxLines = 1,
    subtitleContent = {
      if (summary != null) {
        Row(
          modifier = Modifier.shimmerContent(
            isLoading = isLoading,
            durationMillis = 1000,
            sweepWidth = 1.5f,
          ),
        ) {
          Text("绩点 ", maxLines = 1, softWrap = false)
          Text(
            text = summary.averageGpa,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
          Text(
            text = buildAnnotatedString {
              append(" 专业排名 ")
              withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(summary.averageGpaRank)
              }
            },
            maxLines = 1,
            softWrap = false,
          )
        }
      } else {
        Text(
          text = subtitle,
          modifier = Modifier.shimmerContent(
            isLoading = isLoading,
            durationMillis = 1000,
            sweepWidth = 1.5f,
          ),
        )
      }
    },
    jumpButtonIcon = MR.images.ic_fluent_home_more_48_regular,
    jumpButtonClick = onOpenJwapp,
  ) {
    if (grades.isNotEmpty()) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        JwappGradeRow(
          courseName = "课程",
          credits = "学分",
          totalGrade = "成绩",
          gpa = "绩点",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        grades.take(4).forEach { stored ->
          val grade = stored.grade
          JwappGradeRow(
            courseName = grade.courseName,
            credits = grade.credits.toString().removeSuffix(".0"),
            totalGrade = grade.totalGrade,
            gpa = grade.gpa,
            color = MaterialTheme.colorScheme.onSurface,
            gpaColor = when (grade.gpa.firstOrNull()) {
              '4' -> MaterialTheme.colorScheme.success
              '3' -> MaterialTheme.colorScheme.warning
              '1', '2' -> MaterialTheme.colorScheme.error
              else -> MaterialTheme.colorScheme.onSurface
            },
          )
        }
      }
    }
  }
}

@Composable
private fun JwappGradeRow(
  courseName: String,
  credits: String,
  totalGrade: String,
  gpa: String,
  color: Color,
  gpaColor: Color = color,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      text = courseName,
      color = color,
      fontSize = 15.sp,
      lineHeight = 18.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )
    listOf(credits, totalGrade, gpa).forEachIndexed { index, value ->
      Text(
        text = value,
        color = if (index == 2) gpaColor else color,
        fontSize = 15.sp,
        lineHeight = 18.sp,
        textAlign = TextAlign.End,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.width(48.dp),
      )
    }
  }
}
