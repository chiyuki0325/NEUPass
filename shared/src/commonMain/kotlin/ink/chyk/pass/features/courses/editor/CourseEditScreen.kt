package ink.chyk.pass.features.courses.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.tencent.tmm.kmmresource.compose.*
import ink.chyk.pass.models.*
import ink.chyk.pass.resources.*
import kotlinx.coroutines.*
import kotlin.math.roundToInt

@Composable
fun CourseEditScreen(
  viewModel: CourseEditViewModel,
  onBack: () -> Unit,
  onResult: (CourseEditResult) -> Unit = {},
) {
  val scrollState = rememberScrollState()
  val scope = rememberCoroutineScope()
  val saving by viewModel.saving.collectAsState()

  val courseName by viewModel.courseName.collectAsState()
  val roomName by viewModel.location.collectAsState()
  val weeks by viewModel.weeks.collectAsState()
  val weekDayDelta by viewModel.dayOfWeek.collectAsState()
  val st by viewModel.startSection.collectAsState()
  val en by viewModel.endSection.collectAsState()
  val teacher by viewModel.teacher.collectAsState()
  val credit by viewModel.credit.collectAsState()
  val valueChanged by viewModel.valueChanged.collectAsState()

  val canSave by remember {
    derivedStateOf {
      valueChanged &&
          !courseName.isNullOrBlank() &&
          !roomName.isNullOrBlank() &&
          !teacher.isNullOrBlank() &&
          credit?.toDoubleOrNull() != null &&
          weeks != 0 &&
          weekDayDelta != null &&
          st in 1..12 &&
          en in 1..12 &&
          st <= en
    }
  }

  var showDeleteDialog by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier.fillMaxSize()
  ) {
    EditHeader(viewModel, onBack)

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(scrollState)
    ) {
      // 基本信息卡片
      ItemGroup {
        CourseNameInput(viewModel)
        CourseGenreSelector(viewModel)
        RoomNameInput(viewModel)
        TeacherInput(viewModel)
        CreditInput(viewModel)
        CampusSelector(viewModel)
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 时间信息卡片
      ItemGroup {
        WeekSelector(viewModel)
        Spacer(Modifier.height(4.dp))
        WeekDeltaSelector(viewModel)
      }

      Spacer(modifier = Modifier.height(16.dp))

      ItemGroup {
        StartEndTime(viewModel)
        StartEndSelector(viewModel)
      }


      Spacer(modifier = Modifier.weight(1f))
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        // 删除和保存按钮
        val courseKey by viewModel.courseKey.collectAsState()
        if (courseKey != null) {
          TextButton(
            enabled = !saving,
            onClick = {
              showDeleteDialog = true
            },
          ) {
            Text(stringResource(MR.strings.delete))
          }
          Spacer(modifier = Modifier.width(16.dp))
        }
        Button(
          onClick = {
            // 保存
            scope.launch {
              val result = viewModel.saveCourse()
              onResult(result)
              if (result == CourseEditResult.SAVED) onBack()
            }
          },
          enabled = canSave && !saving
        ) {
          Text(stringResource(MR.strings.save))
        }
      }

    }

    if (showDeleteDialog) {
      DeleteDialog(
        onDismiss = { showDeleteDialog = false },
        onDeleteConfirmed = {
          scope.launch {
            val result = viewModel.deleteCourse()
            onResult(result)
            if (result == CourseEditResult.DELETED) onBack()
          }
        }
      )
    }
  }
}

@Composable
fun DeleteDialog(
  onDismiss: () -> Unit,
  onDeleteConfirmed: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(MR.strings.delete_course)) },
    text = { Text(stringResource(MR.strings.delete_course_confirm)) },
    confirmButton = {
      TextButton(onClick = {
        onDeleteConfirmed()
        onDismiss()
      }) {
        Text(stringResource(MR.strings.delete), color = Color.Red)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(MR.strings.cancel))
      }
    }
  )
}

@Composable
fun ItemGroup(
  groupItems: @Composable ColumnScope.() -> Unit,
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
    ) {
      groupItems()
    }
  }
}

@Composable
fun SmallText(
  text: String,
  color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
  fontFamily: FontFamily = FontFamily.Default,
) {
  Text(
    text = text,
    style = MaterialTheme.typography.bodySmall,
    color = color,
    fontFamily = fontFamily,
  )
  Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun WeekSelector(viewModel: CourseEditViewModel) {
  // 课程周次选择器
  val weeks by viewModel.weeks.collectAsState()
  // 显示前 20 周，点击切换

  val weeksReadable by remember {
    derivedStateOf {
      var result = ""
      var periodStart = -1
      var periodEnd = -1
      for (i in 1..20) {
        if (weeks and (1 shl (i - 1)) != 0) {
          if (periodStart == -1) {
            periodStart = i
            periodEnd = i
          } else {
            periodEnd = i
          }
        } else {
          if (periodStart != -1) {
            result += if (periodStart == periodEnd) {
              "$periodStart, "
            } else {
              "$periodStart-$periodEnd, "
            }
            periodStart = -1
            periodEnd = -1
          }
        }
      }
      if (periodStart != -1) {
        result += if (periodStart == periodEnd) {
          "$periodStart, "
        } else {
          "$periodStart-$periodEnd, "
        }
      }
      result.dropLast(2)
    }
  }

  Row {
    SmallText(stringResource(MR.strings.course_weeks))
    Spacer(Modifier.weight(1f))
    SmallText(weeksReadable)
  }

  Spacer(modifier = Modifier.height(8.dp))

  for (i in 0..1) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      for (j in 1..10) {
        val k = i * 10 + j
        val selected = weeks and (1 shl (k - 1)) != 0
        Surface(
          modifier = Modifier
            .weight(1f)
            .height(32.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable {
              viewModel.toggleWeek(k)
            },
          shape = RoundedCornerShape(4.dp),
          color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ) {
          Box(contentAlignment = Alignment.Center) { SmallText(k.toString()) }
        }
      }
    }
  }
}

private fun weekDayName(i: Int): String {
  // 0 - 6: 周日 - 周六
  return when (i) {
    0 -> "日"
    1 -> "一"
    2 -> "二"
    3 -> "三"
    4 -> "四"
    5 -> "五"
    6 -> "六"
    else -> ""
  }
}

@Composable
fun WeekDeltaSelector(viewModel: CourseEditViewModel) {
  // 选择周日到周六
  val weekDayDelta by viewModel.dayOfWeek.collectAsState()
  // 0 - 6: 周日 - 周六

  SmallText(stringResource(MR.strings.course_weekday))
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    for (i in 0..6) {
      val selected = weekDayDelta == i
      Surface(
        modifier = Modifier
          .weight(1f)
          .height(32.dp)
          .clip(RoundedCornerShape(4.dp))
          .clickable {
            viewModel.setWeekDayDelta(i)
          },
        shape = RoundedCornerShape(4.dp),
        color = when {
          selected -> MaterialTheme.colorScheme.primaryContainer
          i in 1..5 -> MaterialTheme.colorScheme.surfaceContainerHighest
          else -> MaterialTheme.colorScheme.surfaceVariant
        },
      ) {
        Box(contentAlignment = Alignment.Center) {
          SmallText(weekDayName(i))
        }
      }
    }
  }
}

@Composable
fun StartEndTime(viewModel: CourseEditViewModel) {
  // 课程开始和结束时间显示
  val startTime by viewModel.startTime.collectAsState()
  val endTime by viewModel.endTime.collectAsState()
  // "08:00", "09:35"
  Row(
    modifier = Modifier.fillMaxWidth(),
  ) {
    SmallText(stringResource(MR.strings.course_start))
    Spacer(Modifier.width(6.dp))
    SmallText(startTime ?: "--:--", fontFamily = fontFamilyResource(MR.fonts.roboto_numeric.roboto_numeric))
    Spacer(Modifier.weight(1f))
    SmallText(endTime ?: "--:--", fontFamily = fontFamilyResource(MR.fonts.roboto_numeric.roboto_numeric))
    Spacer(Modifier.width(6.dp))
    SmallText(stringResource(MR.strings.course_end))
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartEndSelector(viewModel: CourseEditViewModel) {
  // 课程开始和结束节次选择器
  val st by viewModel.startSection.collectAsState()
  val en by viewModel.endSection.collectAsState()
  val startInteractionSource = remember { MutableInteractionSource() }
  val endInteractionSource = remember { MutableInteractionSource() }
  val colors = SliderDefaults.colors()

  Spacer(modifier = Modifier.height(8.dp))
  RangeSlider(
    value = (st - 1).toFloat()..en.toFloat(),
    onValueChange = { range ->
      val startBoundary = range.start.roundToInt()
      val endBoundary = range.endInclusive.roundToInt()
      var newSt = startBoundary + 1
      var newEn = endBoundary
      if (newSt > newEn) {
        if (startBoundary != st - 1) {
          newSt = newEn
        } else {
          newEn = newSt
        }
      }
      if (newSt != st) viewModel.setSt(newSt)
      if (newEn != en) viewModel.setEn(newEn)
    },
    modifier = Modifier.fillMaxWidth(),
    valueRange = 0f..12f,
    colors = colors,
    startInteractionSource = startInteractionSource,
    endInteractionSource = endInteractionSource,
    startThumb = {
      SliderDefaults.Thumb(
        interactionSource = startInteractionSource,
        colors = colors,
        thumbSize = DpSize(4.dp, 32.dp),
      )
    },
    endThumb = {
      SliderDefaults.Thumb(
        interactionSource = endInteractionSource,
        colors = colors,
        thumbSize = DpSize(4.dp, 32.dp),
      )
    },
    steps = 11,
  )
  Row(Modifier.fillMaxWidth()) {
    for (section in 1..12) {
      Text(
        text = section.toString(),
        modifier = Modifier.weight(1f),
        color = if (section in st..en) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.Center,
      )
    }
  }
  Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun CourseNameInput(viewModel: CourseEditViewModel) {
  // 课程名称输入框
  val courseName by viewModel.courseName.collectAsState()

  OutlinedTextField(
    value = courseName ?: "",
    onValueChange = viewModel::setCourseName,
    label = { Text(stringResource(MR.strings.course_name)) },
    leadingIcon = {
      Icon(
        painter = painterResource(MR.images.ic_fluent_book_information_24_regular),
        contentDescription = "Course Name"
      )
    },
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 8.dp),
    singleLine = true,
  )
}

@Composable
fun TeacherInput(viewModel: CourseEditViewModel) {
  // 授课教师输入框
  val teacher by viewModel.teacher.collectAsState()

  OutlinedTextField(
    value = teacher ?: "",
    onValueChange = viewModel::setTeacher,
    label = { Text(stringResource(MR.strings.course_teacher)) },
    leadingIcon = {
      Icon(
        painter = painterResource(MR.images.ic_fluent_person_note_24_regular),
        contentDescription = "Teacher Name"
      )
    },
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 8.dp),
    singleLine = true,
  )
}

@Composable
fun CreditInput(viewModel: CourseEditViewModel) {
  // 课程学分输入框
  val credit by viewModel.credit.collectAsState()

  OutlinedTextField(
    value = credit ?: "",
    onValueChange = viewModel::setCredit,
    label = { Text(stringResource(MR.strings.course_credit)) },
    leadingIcon = {
      Icon(
        painter = painterResource(MR.images.ic_fluent_ribbon_star_24_regular),
        contentDescription = "Course Credit"
      )
    },
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 8.dp),
    singleLine = true,
  )
}

@Composable
fun RoomNameInput(viewModel: CourseEditViewModel) {
  // 课程地点输入框
  val roomName by viewModel.location.collectAsState()

  OutlinedTextField(
    value = roomName ?: "",
    onValueChange = viewModel::setRoomName,
    label = { Text(stringResource(MR.strings.course_location)) },
    leadingIcon = {
      Icon(
        painter = painterResource(MR.images.ic_fluent_location_24_regular),
        contentDescription = "Room Name"
      )
    },
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 8.dp),
    singleLine = true,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusSelector(viewModel: CourseEditViewModel) {
  // 选择校区
  val campus by viewModel.campus.collectAsState()
  // NANHU, HUNNAN, null

  var expanded by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded },
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 8.dp)
  ) {
    OutlinedTextField(
      readOnly = true,
      value = when (campus) {
        null -> ""
        else -> campus!!.campusName + stringResource(MR.strings.campus)
      },
      onValueChange = {},
      label = { Text(stringResource(MR.strings.campus)) },
      leadingIcon = {
        Icon(
          painter = painterResource(MR.images.ic_fluent_building_home_24_regular),
          contentDescription = "Campus"
        )
      },
      trailingIcon = {
        ExposedDropdownMenuDefaults.TrailingIcon(
          expanded = expanded
        )
      },
      modifier = Modifier
        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
        .fillMaxWidth(),
      singleLine = true,
    )
    ExposedDropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = Modifier
        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
    ) {
      DropdownMenuItem(
        text = { Text(Campus.NANHU.campusName) },
        onClick = {
          viewModel.setCampus(Campus.NANHU)
          expanded = false
        }
      )
      DropdownMenuItem(
        text = { Text(Campus.HUNNAN.campusName) },
        onClick = {
          viewModel.setCampus(Campus.HUNNAN)
          expanded = false
        }
      )
      DropdownMenuItem(
        text = { Text(stringResource(MR.strings.unknown)) },
        onClick = {
          viewModel.setCampus(null)
          expanded = false
        }
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseGenreSelector(viewModel: CourseEditViewModel) {
  // 选择课群
  val courseGenre by viewModel.courseGenre.collectAsState()
  val displayedGenre = courseGenre ?: CourseGenre.OTHER

  var expanded by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded },
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 8.dp)
  ) {
    OutlinedTextField(
      readOnly = true,
      value = displayedGenre.displayName,
      onValueChange = {},
      label = { Text(stringResource(MR.strings.course_icon)) },
      leadingIcon = {
        Icon(
          painter = painterResource(displayedGenre.iconResource),
          contentDescription = "Course Genre"
        )
      },
      trailingIcon = {
        ExposedDropdownMenuDefaults.TrailingIcon(
          expanded = expanded
        )
      },
      modifier = Modifier
        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
        .fillMaxWidth(),
      singleLine = true,
    )
    ExposedDropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = Modifier
        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
    ) {
      CourseGenre.entries.forEach {
        DropdownMenuItem(
          text = { Text(it.displayName) },
          leadingIcon = {
            Icon(
              painter = painterResource(it.iconResource),
              contentDescription = it.displayName
            )
          },
          onClick = {
            viewModel.setCourseGenre(it)
            expanded = false
          }
        )
      }
    }
  }
}

@Composable
private fun EditHeader(viewModel: CourseEditViewModel, onBack: () -> Unit) {
  // 顶部间距和标题
  val title by viewModel.title.collectAsState()
  val valueChanged by viewModel.valueChanged.collectAsState()

  // Spacer(modifier = Modifier.height(32.dp))
  // 间距放不下了。
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier.absoluteOffset(x = (-16).dp)
  ) {
    IconButton(
      onClick = onBack,
      modifier = Modifier.size(48.dp),
    ) {
      Icon(
        painter = painterResource(MR.images.ic_fluent_chevron_left_20_filled),
        contentDescription = "Back",
      )
    }
    Text(
      text = stringResource(title),
      style = MaterialTheme.typography.headlineMedium,
    )
    if (valueChanged) {
      Text(
        " *",
        style = MaterialTheme.typography.headlineMedium,
      )
      Spacer(modifier = Modifier.weight(1f))
      Text(stringResource(MR.strings.remember_to_save))
    }
  }
  Spacer(modifier = Modifier.height(16.dp))
}
