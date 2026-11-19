package ink.chyk.pass.features.courses

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.ctrip.flight.mmkv.defaultMMKV
import ink.chyk.pass.api.createApiClient
import ink.chyk.pass.utils.AppLog
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 课表页的一言句库。
 * 客户端通过上游提供的链接下载句子，在设备上转换并缓存，供后续离线展示。
 * 这种获取方式依据一言官方使用须知中的超链接调用例外：
 * https://sentences-bundle.hitokoto.cn/
 */
internal object Hitokoto {
  private const val CACHE_KEY = "hitokoto_quotes"
  // 两个地址均由一言官方列出，优先使用Pages，单个分类下载失败时回退到jsDelivr。
  private val sources = listOf(
    "https://sentences-bundle.hitokoto.cn/",
    "https://cdn.jsdelivr.net/gh/hitokoto-osc/sentences-bundle@latest/",
  )
  // 沿用原句库的分类：动画、漫画、游戏、原创、影视、诗词、哲学和网易云。
  private val categories = listOf("a", "b", "c", "e", "h", "i", "k", "j")
  private val mmkv by lazy { defaultMMKV() }
  private val json = Json { ignoreUnknownKeys = true }
  private val mutex = Mutex()
  private val http by lazy {
    createApiClient {
      install(HttpTimeout) {
        requestTimeoutMillis = 20_000
        connectTimeoutMillis = 8_000
      }
    }
  }
  private var quotes: List<Pair<String, String>>? = null

  @Composable
  fun getQuote(): Pair<String, String>? {
    // 每次进入组合时选一句，重组保持不变；加载期间或失败时返回null，课表照常显示。
    val quote by produceState<Pair<String, String>?>(null) {
      try {
        value = loadQuotes().randomOrNull()
      } catch (exception: CancellationException) {
        throw exception
      } catch (exception: Exception) {
        AppLog.w("Hitokoto", "句库加载失败：${exception.message}")
      }
    }
    return quote
  }

  private suspend fun loadQuotes(): List<Pair<String, String>> = withContext(Dispatchers.IO) {
    // 多个调用共享同一次句库加载，依次尝试内存、MMKV和网络。
    mutex.withLock {
      quotes?.let { return@withLock it }
      if (mmkv.containsKey(CACHE_KEY)) {
        try {
          val cached = json.decodeFromString<List<List<String>>>(mmkv.getString(CACHE_KEY))
            .map { it[0] to it[1] }
          if (cached.isNotEmpty()) {
            quotes = cached
            return@withLock cached
          }
        } catch (_: Exception) {
          mmkv.removeValueForKey(CACHE_KEY)
        }
      }
      // 所有分类下载成功后再保存整份缓存，后续启动可直接离线使用。
      val downloaded = categories.flatMap { downloadCategory(it) }
      check(downloaded.isNotEmpty()) { "句库为空" }
      mmkv[CACHE_KEY] = json.encodeToString(downloaded.map { listOf(it.first, it.second) })
      quotes = downloaded
      downloaded
    }
  }

  private suspend fun downloadCategory(category: String): List<Pair<String, String>> {
    for ((index, source) in sources.withIndex()) {
      try {
        val converted = convertSentences(http.get("${source}sentences/$category.json").bodyAsText())
        check(converted.isNotEmpty()) { "分类$category 为空" }
        return converted
      } catch (exception: CancellationException) {
        throw exception
      } catch (exception: Exception) {
        if (index == sources.lastIndex) throw exception
        AppLog.w("Hitokoto", "分类$category 下载失败，尝试备用源：${exception.message}")
      }
    }
    error("没有可用句库源")
  }

  // 展示数据为“正文、出处”；上游from是作品或来源，from_who是可空的作者署名。
  internal fun convertSentences(text: String): List<Pair<String, String>> =
    json.decodeFromString<List<Sentence>>(text).map { sentence ->
      val origin = sentence.fromWho?.let { "$it, ${sentence.from}" } ?: sentence.from
      sentence.hitokoto to origin
    }

  @Serializable
  private data class Sentence(
    val hitokoto: String,
    val from: String,
    @SerialName("from_who") val fromWho: String? = null,
  )
}
