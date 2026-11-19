package ink.chyk.pass.repositories

import com.ctrip.flight.mmkv.defaultMMKV
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

private const val CACHE_KEY_PREFIX = "repo_cache_"

@PublishedApi
internal fun cacheStorageKey(key: String) = "$CACHE_KEY_PREFIX$key"

@PublishedApi
internal val diskCache by lazy { defaultMMKV() }

// 一级内存缓存；存的是解析/解码后的对象，避免重复 JSON 解析与磁盘读取。
@PublishedApi
internal val memoryCache = MutableStateFlow<Map<String, Any?>>(emptyMap())

/**
 * 缓存读取结果，调用方据此区分拿到的数据是缓存值还是本次拉取的新值。
 */
sealed interface CacheResult<out T> {
  val value: T

  /** 内存或磁盘上已有的缓存值。 */
  data class Cached<T>(override val value: T) : CacheResult<T>

  /** 本次 action 拉取到的新值。 */
  data class Fresh<T>(override val value: T) : CacheResult<T>
}

inline fun <reified T> cached(
  key: String,
  crossinline action: suspend () -> @Serializable T
): Flow<CacheResult<T>> {
  // 序列化的类型必须在 flow 外面先定好
  // 不能 decodeFromString<T> 这样子
  // T 进不去！
  val serializer = serializer<T>()

  return flow {
    val storageKey = cacheStorageKey(key)

    // --- 阶段一：尝试读取缓存（内存 -> 磁盘） ---
    val memory = memoryCache.value
    if (memory.containsKey(storageKey)) {
      // 这里假设上层不会写错 cache key
      @Suppress("UNCHECKED_CAST")
      emit(CacheResult.Cached(memory[storageKey] as T))
    } else {
      val cachedString = diskCache.getString(storageKey)
      if (!cachedString.isNullOrEmpty()) {
        try {
          val cachedData = Json.decodeFromString(serializer, cachedString)
          memoryCache.update { it + (storageKey to cachedData) }
          emit(CacheResult.Cached(cachedData))
        } catch (e: Exception) {
          // 如果旧缓存格式对不上（比如改了字段），就忽略错误，直接去拿新的
          e.printStackTrace()
        }
      }
    }

    // --- 阶段二：获取最新数据 (动真格的) ---
    try {
      val networkData = action()

      // 发射新数据给 UI
      emit(CacheResult.Fresh(networkData))

      // --- 阶段三：更新缓存 ---
      // 使用 serializer 序列化
      val jsonString = Json.encodeToString(serializer, networkData)
      memoryCache.update { it + (storageKey to networkData) }
      diskCache[storageKey] = jsonString

    } catch (e: Exception) {
      // 如果网络失败了，这里可以选择抛出异常，让 UI 层显示错误
      // 或者如果不抛出，UI 就一直停留在“阶段一”的缓存数据上
      throw e
    }
  }
}

// 二进制缓存：直接存 MMKV 的 ByteArray，不经过 JSON。
fun cachedBytes(
  key: String,
  load: suspend () -> ByteArray,
): Flow<CacheResult<ByteArray>> = flow {
  val storageKey = cacheStorageKey(key)

  val memory = memoryCache.value
  if (memory.containsKey(storageKey)) {
    emit(CacheResult.Cached(memory[storageKey] as ByteArray))
  } else {
    diskCache.getByteArray(storageKey)?.let { bytes ->
      memoryCache.update { it + (storageKey to bytes) }
      emit(CacheResult.Cached(bytes))
    }
  }

  val bytes = load()
  emit(CacheResult.Fresh(bytes))
  memoryCache.update { it + (storageKey to bytes) }
  diskCache[storageKey] = bytes
}

/** 清空所有由 [cached] / [cachedBytes] 写入的缓存。 */
fun clearAllCache() {
  memoryCache.update { emptyMap() }
  diskCache.allKeys()
    .filter { it.startsWith(CACHE_KEY_PREFIX) }
    .forEach(diskCache::removeValueForKey)
}
