package ink.chyk.pass.features.courses

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import com.tencent.tmm.kmmresource.compose.painterResource
import com.tencent.tmm.kmmresource.compose.stringResource
import ink.chyk.pass.resources.MR


// 用户首次进入课程表时的引导页

@Composable
fun ImportCoursesGuide(onImportCourses: () -> Unit) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(16.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            painter = painterResource(MR.images.ic_fluent_arrow_download_24_regular),
            contentDescription = "Import Courses",
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            stringResource(MR.strings.import_courses),
            style = MaterialTheme.typography.headlineMedium
          )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(MR.strings.import_courses_splash_1))
        Text(stringResource(MR.strings.import_courses_splash_2))
        Spacer(modifier = Modifier.height(16.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center
        ) {
          ImportCoursesButton(onImportCourses)
        }
      }
    }
  }
}

@Composable
fun ImportCoursesButton(onImportCourses: () -> Unit, modifier: Modifier = Modifier) {
  Button(
    modifier = modifier,
    onClick = onImportCourses,
  ) { Text(stringResource(MR.strings.import_courses)) }
}