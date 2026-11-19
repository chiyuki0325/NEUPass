package ink.chyk.pass.api

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

internal actual fun createApiEngine(): HttpClientEngine = Darwin.create()
