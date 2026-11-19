package ink.chyk.pass.api.portal

import ink.chyk.pass.api.*
import io.ktor.client.call.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*

class PortalApi {
  companion object {
    private const val BASE_URL = "https://personal.neu.edu.cn/portal"
  }

  private val loginHttp = createApiClient {
    followRedirects = false
    expectSuccess = false
    install(PortalLoginPlugin)
  }

  private val http = createApiClient {
    followRedirects = false
    expectSuccess = false
    install(HttpCookies) { storage = PortalCookiesStorage }
    install(PortalPlugin)
  }

  // 头像等二进制资源不走门户信封，需要跟随重定向。
  private val resourceHttp = createApiClient {
    install(HttpCookies) { storage = PortalCookiesStorage }
  }

  suspend fun login(appTicket: String) {
    loginHttp.get("$BASE_URL/manage/common/cas_login/1") {
      parameter("redirect", BASE_URL)
      parameter("ticket", appTicket)
    }
  }

  suspend fun getUserInfo(): UserInfoResponse =
    http.get("$BASE_URL/personal/frontend/data/info").body()

  suspend fun getDataItemIds(): DataItemIdsResponse =
    http.get("$BASE_URL/personal/frontend/data/items") {
      parameter("type", "personal_data")
    }.body()

  suspend fun getDataItem(id: String): DataItemResponse =
    http.get("$BASE_URL/personal/frontend/data/detail") {
      parameter("id", id)
    }.body()

  suspend fun uploadImage(image: ByteArray, mimeType: String, fileName: String): UploadImageResponse =
    http.post("$BASE_URL/frontend/upload/image") {
      setBody(MultiPartFormDataContent(formData {
        append("category", "image")
        append("upfile", image, Headers.build {
          append(HttpHeaders.ContentType, mimeType)
          append(HttpHeaders.ContentDisposition, ContentDisposition.File
            .withParameter(ContentDisposition.Parameters.FileName, fileName).toString())
        })
      }))
    }.body()

  suspend fun updateAvatar(imageUrl: String): List<String> =
    http.put("$BASE_URL/frontend/user/update-avatar") {
      setBody(MultiPartFormDataContent(formData {
        append("avatar", imageUrl)
      }))
    }.body()

  // 用户资料里的 avatar 可能是相对路径，补全为门户地址。
  suspend fun getAvatar(path: String): ByteArray =
    resourceHttp.get(if (path.startsWith("/")) "$BASE_URL$path" else path).body()

  fun close() {
    loginHttp.close()
    http.close()
    resourceHttp.close()
  }
}
