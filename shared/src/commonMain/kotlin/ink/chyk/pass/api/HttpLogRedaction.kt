package ink.chyk.pass.api

// 网络调试日志的脱敏规则

private val secretNames = setOf(
  "authorization", "cookie", "set-cookie", "password",
  "sms_code", "ticket", "token", "xsrf-token", "x-xsrf-token", "device_id", "uuid",
  "username", "student_id", "x-mobile-device-uuid", "data", "authorization-str",
)

internal fun Map<String, String>.redactedHeaders(): Map<String, String> =
  mapValues { (key, value) ->
    when {
      key.lowercase() in secretNames -> "<redacted>"
      key.equals("Location", true) -> value.redactedUrl()
      else -> value
    }
  }

internal fun Map<String, List<String>>.redactedHeaderValues(): Map<String, List<String>> =
  mapValues { (key, values) ->
    when {
      key.lowercase() in secretNames -> listOf("<redacted>")
      key.equals("Location", true) -> values.map(String::redactedUrl)
      else -> values
    }
  }

internal fun List<Pair<String, String>>.redactedForm(): List<Pair<String, String>> =
  map { (key, value) -> key to if (key.lowercase() in secretNames) "<redacted>" else value }

internal fun String.redactedUrl(): String {
  val separator = indexOf('?')
  if (separator < 0) return this
  val base = substring(0, separator)
  val query = substring(separator + 1).split('&').joinToString("&") { part ->
    val key = part.substringBefore('=')
    if (key.lowercase() in secretNames || key.lowercase().contains("ticket") || key.lowercase().contains("token")) {
      "$key=<redacted>"
    } else part
  }
  return "$base?$query"
}

internal fun String.redactedBody(): String =
  replace(Regex("(?i)\"(password|sms_?code|ticket|token|cookie|qrCode)\"\\s*:\\s*\"[^\"]*\"")) {
    "\"${it.groupValues[1]}\":\"<redacted>\""
  }
