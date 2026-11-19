package ink.chyk.pass.api

import io.ktor.client.engine.HttpClientEngine

// 各 API client 共用的 Ktor engine。
// API client 仅为薄层，公用 engine 可以实现复用连接池等性能优化。
internal val sharedApiEngine: HttpClientEngine by lazy { createApiEngine() }

internal expect fun createApiEngine(): HttpClientEngine
