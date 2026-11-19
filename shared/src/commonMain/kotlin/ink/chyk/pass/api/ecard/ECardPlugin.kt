package ink.chyk.pass.api.ecard

import ink.chyk.pass.*
import io.ktor.client.plugins.api.*
import io.ktor.client.statement.*
import io.ktor.http.*

// 服务端 machineKey 轮换后，持久化的 VIEWSTATE 会因 MAC 校验失败返回 500。
internal class ECardViewStateExpiredException : NetworkException("ecard viewstate 已失效")

// 自助查询平台未登录时会 302 到统一认证，业务页面本身没有独立的错误码。
val ECardPlugin = createClientPlugin("ECardPlugin") {
  onResponse { response ->
    val status = response.status.value
    if (status in 300..399 || status == 401 || status == 403) {
      throw SessionExpiredException("backend returned HTTP $status")
    }
    if (status == 500) {
      val body = response.bodyAsText()
      if (body.contains("验证视图状态 MAC 失败") ||
        body.contains("Validation of viewstate MAC failed", ignoreCase = true)
      ) {
        throw ECardViewStateExpiredException()
      }
      throw RequestFailedException(response.call.request.url.toString())
    }
    if (!response.status.isSuccess()) {
      throw RequestFailedException(response.call.request.url.toString())
    }
  }
}
