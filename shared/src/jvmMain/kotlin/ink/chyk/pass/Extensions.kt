package ink.chyk.pass

import kotlinx.coroutines.*

fun String.replaceLast(oldValue: String, newValue: String): String {
  val index = this.lastIndexOf(oldValue)
  if (index == -1) return this
  return this.replaceRange(index, index + oldValue.length, newValue)
}

fun List<String>.getSetCookieValue(cookieName: String): String {
  // headers["Set-Cookie"] 可能有多个，找到以 cookieName 开头的那个，提取 cookieName 的值
  return this.first({ it.startsWith(cookieName) })
    .substringBefore(";")
    .substringAfter("=")
}
