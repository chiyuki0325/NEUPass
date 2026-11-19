package ink.chyk.pass.features.ecode

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tencent.tmm.kmmresource.compose.*
import ink.chyk.pass.*
import ink.chyk.pass.resources.*
import ink.chyk.pass.repositories.SettingsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.ranges.coerceIn
import kotlin.ranges.until

@Composable
fun ECodeScreen(viewModel: ECodeViewModel, onShowFakeECode: () -> Unit) {
    // 进入界面时调整亮度，退出时恢复；具体窗口实现由平台提供
    PlatformECodeEffects(viewModel.shouldRaiseBrightness())
    ECodeScreenContent(viewModel, onShowFakeECode)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ECodeScreenContent(viewModel: ECodeViewModel, onShowFakeECode: () -> Unit) {
    val loadingState by viewModel.loadingState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val code by viewModel.code.collectAsState()
    val userInfo by viewModel.userInfo.collectAsState()
    val redactSensitiveInfo by SettingsRepository.redactSensitiveInfo.collectAsState()
    // 开发时用固定学号和带星号的姓名展示一码通身份信息，沿用一码通的姓名遮掩样式。
    val userDescription = userInfo?.let {
        val studentId = if (redactSensitiveInfo) "20241024" else it.userCode
        val name = if (redactSensitiveInfo) "董*敏" else it.userName
        "$studentId | $name | ${it.unitName}"
    }
    val timeDescription by viewModel.timeDescription.collectAsState()

    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // 以手机宽度下的二维码尺寸为基准，避免在宽屏窗口继续放大。
        val minimumCodeSize = if (maxHeight < 420.dp) 120.dp else 210.dp
        val codeSize = minOf(maxWidth * 0.58f, maxHeight - 180.dp).coerceIn(minimumCodeSize, 220.dp)
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refreshQRCode,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Image(
                        painterResource(if (isSystemInDarkTheme()) MR.images.logo_white else MR.images.logo_black),
                        "Pass QR Code",
                        Modifier.height(48.dp).clickable(onClick = onShowFakeECode),
                        contentScale = ContentScale.FillHeight,
                    )
                    Spacer(Modifier.width(16.dp))
                    Text("一码通", style = MaterialTheme.typography.headlineLarge)
                }
                Spacer(Modifier.height(32.dp))
                when {
                    loadingState == LoadingState.SUCCESS && !code.isNullOrEmpty() ->
                        ECodeContent(code!!, codeSize, viewModel::refreshQRCode)
                    loadingState == LoadingState.LOADING -> ECodeLoading(codeSize)
                    else -> ECodeNoInternet(codeSize, viewModel::refreshQRCode)
                }
                Spacer(Modifier.height(32.dp))
                Text(
                    userDescription ?: if (loadingState == LoadingState.LOADING) {
                        stringResource(MR.strings.loading)
                    } else {
                        stringResource(MR.strings.user_info_load_failed)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    when {
                        // 成功 有二维码
                        loadingState == LoadingState.SUCCESS && !code.isNullOrEmpty() ->
                            stringResource(MR.strings.generate_time, timeDescription ?: "")
                        // 响应成功 但是没有二维码
                        loadingState == LoadingState.SUCCESS -> stringResource(MR.strings.generate_failed_unknown_server)
                        // 连接失败
                        loadingState == LoadingState.FAILED -> stringResource(MR.strings.generate_failed_network)
                        // 加载中
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }

    // 启动二维码刷新协程
    LaunchedEffect(viewModel) {
        // 协程会在页面离开组合时自动取消
        viewModel.startAutoRefreshQRCodeLoop().collect()
    }
}

@Composable
fun ECodeLoading(codeSize: androidx.compose.ui.unit.Dp = 210.dp) {
    // "加载中" 状态
    Box(
        Modifier.size(codeSize).border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) { CircularProgressIndicator() }
}

@Composable
fun ECodeContent(code: String, codeSize: androidx.compose.ui.unit.Dp = 210.dp, onClick: () -> Unit) {
    val primaryColor = Color(0xff006533)
    val backgroundColor = Color.White
    // 优化：在后台线程计算，不阻塞 UI
    val qrMatrix by produceState<QrMatrix?>(null, code) {
        value = withContext(Dispatchers.IO) { createQrMatrix(code) }
    }
    val customFont: FontFamily = fontFamilyResource(MR.fonts.iconfont.iconfont)

    Box(
        Modifier.size(codeSize).border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        qrMatrix?.let { matrix ->
            Canvas(Modifier.size(codeSize).clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick)) {
                drawRect(backgroundColor)
                val cellWidth = size.width / matrix.width
                val cellHeight = size.height / matrix.height
                for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
                    if (matrix[x, y]) drawRect(
                        primaryColor,
                        topLeft = androidx.compose.ui.geometry.Offset(x * cellWidth, y * cellHeight),
                        size = androidx.compose.ui.geometry.Size(cellWidth + 0.5f, cellHeight + 0.5f),
                    )
                }
            }
            // 中间的点和背景图标
            val iconScale = codeSize.value / 210f
            Text("●", color = primaryColor, fontFamily = customFont, fontSize = 48.sp * iconScale)
            Text("\uE6BE", color = backgroundColor, fontFamily = customFont, fontSize = 32.sp * iconScale)
        }
    }
}

// 断网
@Composable
fun ECodeNoInternet(codeSize: androidx.compose.ui.unit.Dp = 210.dp, onClick: () -> Unit) {
    Box(
        Modifier.size(codeSize).border(1.dp, Color.Gray, RoundedCornerShape(8.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painterResource(MR.images.no_internet),
            "No Internet",
            Modifier.size(48.dp),
            contentScale = ContentScale.FillHeight,
        )
    }
}

@Composable
expect fun PlatformECodeEffects(raiseBrightness: Boolean)
