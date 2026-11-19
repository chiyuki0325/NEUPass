package ink.chyk.pass.api

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun createApiEngine(): HttpClientEngine = OkHttp.create()
