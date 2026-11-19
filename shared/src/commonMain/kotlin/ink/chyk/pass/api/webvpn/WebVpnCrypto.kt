package ink.chyk.pass.api.webvpn

internal expect object WebVpnCrypto {
  val HOST: String
  fun encode(url: String): String
  fun hostSegment(url: String): String
}
