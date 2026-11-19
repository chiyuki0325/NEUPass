package ink.chyk.pass.features.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.tencent.tmm.kmmresource.compose.painterResource
import com.tencent.tmm.kmmresource.compose.stringResource
import ink.chyk.pass.components.SchoolTitle
import ink.chyk.pass.resources.MR
import ink.chyk.pass.ui.isWideWidth
import ink.chyk.pass.ui.platformScaffoldContentWindowInsets
import ink.chyk.pass.api.portal.login.LoginApi.LoginState

@Composable
fun LoginScreen(viewModel: LoginViewModel, openMainPage: () -> Unit = {}) {
  // 登录窗口
  val step by viewModel.step.collectAsState()
  val uiState by viewModel.uiState.collectAsState()
  var studentId by remember { mutableStateOf(viewModel.savedStudentId) }
  var password by remember { mutableStateOf(viewModel.savedPassword) }
  var passwordVisible by remember { mutableStateOf(false) }
  var smsCode by remember { mutableStateOf("") }
  val showSmsCode = step is LoginState.SmsCodeSent
  val loading = uiState == LoginUiState.Running

  val studentIdFillsRequirement by remember { derivedStateOf { studentId.length in 5..8 } }
  // 研✌️的学号可能是6/7位
  // 保险起见 5-8位都接受
  val loginEnabled = studentIdFillsRequirement && studentId.isNotEmpty() &&
    password.isNotEmpty() && (!showSmsCode || smsCode.length == 6) && !loading

  LaunchedEffect(uiState) { if (uiState == LoginUiState.Completed) openMainPage() }

  val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
  val cardModifier = if (windowSizeClass.isWideWidth) {
    Modifier.widthIn(max = 500.dp).fillMaxWidth()
  } else {
    Modifier.fillMaxWidth()
  }

  Scaffold(contentWindowInsets = platformScaffoldContentWindowInsets()) { innerPadding ->
    Box(Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
      Card(cardModifier.padding(horizontal = 16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
          SchoolTitle(stringResource(MR.strings.uniform_auth))
          StudentIdInput(studentId) { if (it.length <= 8) studentId = it }
          PasswordInput(password, { password = it }, passwordVisible) { passwordVisible = !passwordVisible }
          if (showSmsCode) {
            SmsCodeInput(smsCode) { if (it.length <= 6) smsCode = it }
            Text(
              stringResource(MR.strings.sms_code_sent),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(bottom = 8.dp),
            )
          }
          LoginButton(loading, loginEnabled) {
            if (showSmsCode) viewModel.submitSmsCode(smsCode)
            else viewModel.login(studentId, password)
          }
        }
      }
    }
  }

  (uiState as? LoginUiState.Failed)?.let { error ->
    val message = when (error.kind) {
      LoginErrorKind.LOGIN_FAILED -> error.message ?: stringResource(MR.strings.login_failed)
      LoginErrorKind.REQUEST_FAILED -> stringResource(MR.strings.request_error_content)
      LoginErrorKind.OTHER -> error.message.orEmpty()
    }
    ErrorDialog(message, viewModel::dismissError)
  }
}

@Composable
fun StudentIdInput(studentId: String, onStudentIdChange: (String) -> Unit) {
  // 学号输入框
  OutlinedTextField(
    value = studentId, onValueChange = onStudentIdChange,
    label = { Text(stringResource(MR.strings.student_id)) },
    leadingIcon = { Icon(painterResource(MR.images.ic_fluent_person_24_filled), "Student ID") },
    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
  )
}

@Composable
fun PasswordInput(password: String, onPasswordChange: (String) -> Unit, passwordVisible: Boolean, onPasswordVisibleChange: () -> Unit) {
  // 密码输入框
  OutlinedTextField(
    value = password, onValueChange = onPasswordChange,
    label = { Text(stringResource(MR.strings.password)) },
    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    leadingIcon = { Icon(painterResource(MR.images.ic_fluent_password_24_filled), "Password") },
    trailingIcon = {
      val icon = if (passwordVisible) MR.images.ic_fluent_eye_24_filled else MR.images.ic_fluent_eye_off_24_filled
      val description = stringResource(if (passwordVisible) MR.strings.password_hide else MR.strings.password_show)
      IconButton(onClick = onPasswordVisibleChange) { Icon(painterResource(icon), description) }
    }, singleLine = true,
  )
}

@Composable
fun SmsCodeInput(smsCode: String, onSmsCodeChange: (String) -> Unit) {
  // 短信验证码输入框
  OutlinedTextField(
    value = smsCode, onValueChange = onSmsCodeChange,
    label = { Text(stringResource(MR.strings.sms_code)) },
    leadingIcon = { Icon(painterResource(MR.images.ic_fluent_clipboard_number_123_24_regular), "SMS Code") },
    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
  )
}

@Composable
fun LoginButton(loading: Boolean, enabled: Boolean, onClick: () -> Unit) {
  // 登录按钮
  Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
    Button(onClick = onClick, enabled = enabled) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (loading) {
          CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp,
          )
          Spacer(Modifier.width(8.dp))
        }
        Text(stringResource(MR.strings.login))
      }
    }
  }
}

@Composable
fun ErrorDialog(errorMessage: String, onDismissRequest: () -> Unit = {}) {
  AlertDialog(
    onDismissRequest = onDismissRequest,
    title = { Text(stringResource(MR.strings.login_failed)) }, text = { Text(errorMessage) },
    confirmButton = { Button(onClick = onDismissRequest) { Text(stringResource(MR.strings.confirm)) } },
  )
}
