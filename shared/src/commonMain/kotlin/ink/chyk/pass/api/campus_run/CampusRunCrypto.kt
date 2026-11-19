package ink.chyk.pass.api.campus_run

internal expect object CampusRunCrypto {
  fun signMD5(data: String): String
  fun aesDecrypt(data: String): String
  fun aesEncrypt(data: String): String
}
