package ink.chyk.pass

import android.app.Application
import android.content.Context
import com.ctrip.flight.mmkv.initialize
import com.tencent.tmm.kmmresource.KmmResource

class NEUPassApplication: Application() {
  companion object {
    // 不管了！只要用得谨慎一点就不会漏
    @Suppress("StaticFieldLeak")
    lateinit var instance: NEUPassApplication
      private set
    @Suppress("StaticFieldLeak")
    lateinit var context: Context
      private set

  }

  override fun onCreate() {
    super.onCreate()
    initialize(this)
    KmmResource.initialize(this)
    instance = this
    context = applicationContext
  }
}
