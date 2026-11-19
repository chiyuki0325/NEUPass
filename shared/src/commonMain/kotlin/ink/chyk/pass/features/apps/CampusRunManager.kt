package ink.chyk.pass.features.apps

import androidx.compose.ui.graphics.*
import ink.chyk.pass.*
import ink.chyk.pass.api.campus_run.*
import ink.chyk.pass.resources.MR
import ink.chyk.pass.utils.AppLog
import com.tencent.tmm.kmmresource.resource.StringResource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.Pair
import ink.chyk.pass.repositories.CampusRunRepository as Repo

class CampusRunManager(
  private val scope: CoroutineScope  // 由 ViewModel 提供 viewModelScope
) {
  // 加载状态：校园跑
  private var _campusRunState = MutableStateFlow(LoadingState.LOADING)
  val campusRunState: StateFlow<LoadingState> = _campusRunState

  // 当前学期名称
  private var _termName = MutableStateFlow("")
  val termName: StateFlow<String> = _termName

  // 存储乐跑 API 返回的数据
  private var _beforeRun = MutableStateFlow<BeforeRunResponse?>(null)
  val beforeRun: StateFlow<BeforeRunResponse?> = _beforeRun
  private var _termRunRecord = MutableStateFlow<GetTermRunRecordResponse?>(null)
  val termRunRecord: StateFlow<GetTermRunRecordResponse?> = _termRunRecord

  private val t1 = arrayOf(
    MR.strings.run_t1_1, MR.strings.run_t1_2, MR.strings.run_t1_3,
  )
  private val t2 = arrayOf(
    MR.strings.run_t2_1, MR.strings.run_t2_2, MR.strings.run_t2_3,
  )
  private val t3 = arrayOf(
    MR.strings.run_t3_1, MR.strings.run_t3_2, MR.strings.run_t3_3,
  )
  private val t4 = arrayOf(
    MR.strings.run_t4_1, MR.strings.run_t4_2, MR.strings.run_t4_3,
  )
  private val t5 = arrayOf(
    MR.strings.run_t5_1, MR.strings.run_t5_2, MR.strings.run_t5_3,
    MR.strings.run_t5_4, MR.strings.run_t5_5,
  )


  fun initCampusRun() {
    if (_campusRunState.value == LoadingState.SUCCESS) return
    scope.launch {
      _initCampusRun()
    }
  }

  suspend fun refreshCampusRun() {
    val job = scope.launch {
      _initCampusRun()
    }
    job.join()
  }

  suspend fun _initCampusRun() = withContext(Dispatchers.IO) {
    try {
      _campusRunState.value = LoadingState.LOADING
      // 新建立一个协程作用域，在任何一个请求失败时直接取消剩余的请求，避免不必要的资源浪费
      coroutineScope {
        // repo 层面已增加双重检查，所以可以火力全开
        val dIndex = async { Repo.getIndex() }
        val dBeforeRun = async { Repo.getBeforeRun() }
        val dTermRunRecord = async { Repo.getTermRunRecord() }
        _termName.value = dIndex.await()?.term_name ?: "服务器返回错误"
        _campusRunState.value = LoadingState.PARTIAL_SUCCESS
        _beforeRun.value = dBeforeRun.await()
        _termRunRecord.value = dTermRunRecord.await()
        _campusRunState.value = LoadingState.SUCCESS
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      AppLog.e("AppsViewModel", "initCampusRun: ${e.message}")
      _campusRunState.value = LoadingState.FAILED
    }
  }

  fun startCampusRun(open: (String) -> Unit) {
    // 进入校园跑小程序
    // 采用腾讯 X5 浏览器内核模拟小程序运行环（封装好的 CampusRunningActivity）

    scope.launch {
      open(Repo.getStartRunUrl())
    }
  }

  fun distanceColorAndTextPair(
    distance: String?,
  ): Pair<Color, StringResource> {
    // 返回对应的颜色和随机抽取的文字
    val distanceDouble = distance?.toDoubleOrNull()
    return when {
      distanceDouble == null -> Color(255, 0, 0) to t1.random()
      distanceDouble < 20 -> Color(255, 0, 0) to t1.random()
      distanceDouble < 40 -> Color(255, 165, 0) to t2.random()
      distanceDouble < 80 -> Color(0, 165, 255) to t3.random()
      distanceDouble < 96 -> Color(0, 255, 165) to t4.random()
      else -> Color(20, 255, 20) to t5.random()
    }
  }
}
