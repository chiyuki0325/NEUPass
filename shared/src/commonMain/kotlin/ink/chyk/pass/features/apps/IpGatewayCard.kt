package ink.chyk.pass.features.apps

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.*
import ink.chyk.pass.*
import ink.chyk.pass.resources.MR
import com.tencent.tmm.kmmresource.compose.painterResource
import com.tencent.tmm.kmmresource.compose.stringResource
import com.tencent.tmm.kmmresource.resource.StringResource
import kotlinx.serialization.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*

private fun Long.toMB(): String {
  // 将字节转换为 MB
  return "${formatTwoDecimals(this / 1024.0 / 1024.0)} MB"
}

private fun formatTwoDecimals(value: Double): String {
  val scaled = kotlin.math.round(value * 100).toLong()
  return "${scaled / 100}.${kotlin.math.abs(scaled % 100).toString().padStart(2, '0')}"
}

private val fontSize = 15.sp

@ExperimentalSerializationApi
@Composable
fun IpGatewayCard(
  mgr: IpGatewayManager
) {
  // 深澜网关认证卡片

  val loadingState by mgr.ipGatewayState.collectAsState()
  val gatewayInfo by mgr.gatewayInfo.collectAsState()
  var onlineDevices by remember { mutableIntStateOf(0) } // 在线设备数

  LaunchedEffect(Unit) {
    // 加载网关数据
    mgr.initIpGateway()
  }

  // 监听 isConnected 状态，变化时 initIpGateway
  PlatformNetworkReconnectEffect { mgr.initIpGateway() }

  AppCard(
    icon = MR.images.ic_fluent_desktop_signal_24_regular,
    title = stringResource(MR.strings.ip_gateway),
    subtitle = buildAnnotatedString {
      when (loadingState) {
        LoadingState.LOADING -> append(stringResource(MR.strings.loading))
        LoadingState.FAILED -> append(stringResource(MR.strings.not_campus_network))
        LoadingState.SUCCESS -> when (gatewayInfo?.isOnline) {
          true -> {
            onlineDevices = gatewayInfo?.onlineInfo?.onlineDeviceTotal?.toInt() ?: 0
            append(stringResource(MR.strings.current_online_ip))
            append(": ")
            append(gatewayInfo?.onlineInfo?.onlineIp ?: stringResource(MR.strings.loading))
          }

          false -> append(stringResource(MR.strings.gateway_logged_out))
          null -> append(stringResource(MR.strings.request_failed_title))
        }

        LoadingState.PARTIAL_SUCCESS -> append(stringResource(MR.strings.failed_to_fetch_user_data))
      }
    },
    hasJumpButton = true,
    jumpButtonIcon = if (gatewayInfo?.isOnline == true) MR.images.ic_fluent_link_dismiss_24_filled
    else MR.images.ic_fluent_link_add_24_filled,
    jumpButtonClick = {
      if (gatewayInfo?.isOnline == true) {
        // 断开网关
        mgr.logoutIpGateway()
      } else {
        // 登录网关
        mgr.loginIpGateway()
      }
    },
    jumpButtonEnabled = loadingState == LoadingState.SUCCESS
  ) {
    if (loadingState == LoadingState.SUCCESS && gatewayInfo?.isOnline == true) {
      // 加载成功后显示网络详细信息
      Spacer(modifier = Modifier.height(8.dp))

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .animateContentSize(),
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          InfoLine(
            MR.strings.traffic_used,
            gatewayInfo?.onlineInfo?.sumBytes?.toMB() ?: stringResource(MR.strings.loading)
          )
          InfoLine(
            MR.strings.traffic_left,
            gatewayInfo?.onlineInfo?.remainBytes?.toMB() ?: stringResource(MR.strings.loading)
          )
        }
        InfoLine(
          MR.strings.network_balance,
          "${formatTwoDecimals(gatewayInfo?.onlineInfo?.userBalance ?: 0.0)} ${stringResource(MR.strings.yuan)}"
        )

        Text(fontSize = fontSize, text = gatewayInfo?.onlineInfo?.billingName ?: "")

        if (onlineDevices > 1) {
          // 共几台设备在线
          Text(buildAnnotatedString {
            withStyle(SpanStyle(fontSize = fontSize)) {
              append(stringResource(MR.strings.devices_online_1))
              append(' ')
              withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(onlineDevices.toString())
              }
              append(' ')
              append(stringResource(MR.strings.devices_online_2))
            }
          })
        }
      }
    }
  }
}

// 一行信息
@Composable
private fun InfoLine(k: StringResource, v: String) {
  Text(buildAnnotatedString {
    withStyle(SpanStyle(fontSize = fontSize)) {
      withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(stringResource(k)) }
      append(" ")
      append(v)
    }
  })
}
