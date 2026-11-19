package ink.chyk.pass

import kotlinx.coroutines.*

open class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)

// 连接超时、DNS、断网等传输层失败；与 HTTP 层面的 RequestFailedException 区分，便于换路重试
class TransportException(message: String, cause: Throwable? = null) : NetworkException(message, cause)

open class LoginFailedException(msg: String) : Exception("登录失败: $msg")
class RequestFailedException(url: String) : NetworkException("请求失败: $url")
class PasswordIncorrectException : LoginFailedException("密码错误")

// 可重试的会话失败（如 CASTGC->ST 失败），由repo层（如LoginRepository）内部静默重登处理
open class TicketFailedException(a: String = "获取 ticket 失败") : Exception(a)
// 需要用户重新登录：UI 会跳登录页。
// 继承 CancellationException，让业务协程安静结束，不触发崩溃处理器，业务方无需捕获。
class LoginRequiredException(message: String) : CancellationException(message)
class TicketExpiredException : TicketFailedException("ticket 过期")
class SessionExpiredException(a: String? = null) : Exception("session 过期 $a")
class EHallPortalException(a: String? = null): Exception("一号通门户报错 $a")
