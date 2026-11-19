package ink.chyk.pass.features.courses

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import com.tencent.tmm.kmmresource.compose.*
import com.tencent.tmm.kmmresource.resource.*
import ink.chyk.pass.components.*
import ink.chyk.pass.models.*
import ink.chyk.pass.resources.*
import kotlinx.datetime.*
import kotlin.math.*

// 本文件中存放 日视图/周视图 公用的组件

@Composable
fun NoCoursesSplash(date: LocalDate, text: String, onImportCourses: () -> Unit) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(8.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Image(
        painter = painterResource(
          listOf(
            MR.images.neko1,
            MR.images.neko2,
            MR.images.neko3
          )[date.hashCode().absoluteValue % 3]
        ),
        contentDescription = "No Courses",
        modifier = Modifier
          .weight(1f, fill = false)
          .sizeIn(maxWidth = 200.dp, maxHeight = 200.dp)
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text,
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(8.dp))
      ImportCoursesButton(onImportCourses)
    }
  }
}

@Composable
fun ColumnSeparator(text: String, dark: Boolean) {
  // 文字+分割线组件。用于早午晚课程/已考试未考试
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // 早午晚标题
    Text(
      text = text,
      style = MaterialTheme.typography.bodyMedium,
      modifier = Modifier.padding(vertical = 8.dp)
    )
    Spacer(modifier = Modifier.width(8.dp))

    // 这是一条可爱的分割线
    Surface(
      modifier = Modifier
        .height(1.dp)
        .fillMaxWidth(),
      color = if (dark) Color.DarkGray else Color.LightGray
    ) {}
  }
}

@Composable
fun CourseDetailDialog(
  it: Course,
  onDismissRequest: () -> Unit,
  onEdit: (key: String) -> Unit
) {
  val height = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
  Dialog(
    content = {
      Card(
        modifier = Modifier.heightIn(0.dp, 0.9f * height),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
      ) {
        Column(
          modifier = Modifier.padding(16.dp)
        ) {
          DialogTitle(
            MR.images.ic_fluent_calendar_template_24_regular,
            stringResource(MR.strings.course_details)
          )
          Spacer(Modifier.height(8.dp))
          Column(
            modifier = Modifier
              .weight(1f, fill = false)
              .fillMaxWidth()
              .verticalScroll(rememberScrollState())
          ) {
            CourseDetailDialogItem(
              it.name,
              MR.strings.course_name,
              MR.images.ic_fluent_book_information_24_regular
            )
            CourseDetailDialogItem(
              it.location,
              MR.strings.course_location,
              MR.images.ic_fluent_location_24_regular
            )
            CourseDetailDialogItem(
              it.teacher,
              MR.strings.course_teacher,
              MR.images.ic_fluent_person_note_24_regular
            )
            CourseDetailDialogItem(
              it.campus?.fullName ?: stringResource(MR.strings.unknown),
              MR.strings.campus,
              MR.images.ic_fluent_building_home_24_regular
            )
            CourseDetailDialogItem(
              (it.genre ?: CourseGenre.OTHER).displayName,
              MR.strings.course_icon,
              (it.genre ?: CourseGenre.OTHER).iconResource
            )
            CourseDetailDialogItem(
              it.classes ?: stringResource(MR.strings.whole_school),
              MR.strings.course_classes,
              MR.images.ic_fluent_people_team_24_regular
            )
            CourseDetailDialogItem(
              it.credit ?: stringResource(MR.strings.unknown),
              MR.strings.course_credit,
              MR.images.ic_fluent_ribbon_star_24_regular
            )
            CourseDetailDialogItem(
              it.code ?: stringResource(MR.strings.unknown),
              MR.strings.course_code,
              MR.images.ic_fluent_code_circle_24_regular
            )
            CourseDetailDialogItem(
              it.serial ?: stringResource(MR.strings.unknown),
              MR.strings.course_serial,
              MR.images.ic_fluent_balloon_24_regular
            )
          }
          Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
              .fillMaxWidth()
          ) {
            TextButton(onClick = { onEdit(it.key) }) {
              Text(stringResource(MR.strings.edit))
            }
            TextButton(onClick = onDismissRequest) {
              Text(stringResource(MR.strings.close))
            }
          }
        }
      }
    },
    onDismissRequest = onDismissRequest,
  )
}


@Composable
fun CourseDetailDialogItem(
  text: String,
  label: StringResource,
  icon: ImageResource,
) {
  OutlinedTextField(
    value = text,
    onValueChange = {},
    // enabled = false,
    readOnly = true,
    label = { Text(stringResource(label)) },
    leadingIcon = {
      Icon(
        painter = painterResource(icon),
        contentDescription = stringResource(label)
      )
    },
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 8.dp),
    singleLine = true,
  )
}
