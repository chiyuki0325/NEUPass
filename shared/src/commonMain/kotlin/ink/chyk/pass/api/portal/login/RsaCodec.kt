package ink.chyk.pass.api.portal.login

internal expect object RsaCodec {
  fun encrypt(plaintext: String): String
  fun decrypt(cipherBase64: String): String
}
