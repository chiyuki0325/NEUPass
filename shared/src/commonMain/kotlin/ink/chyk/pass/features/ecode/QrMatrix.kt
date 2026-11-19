package ink.chyk.pass.features.ecode

data class QrMatrix(val width: Int, val height: Int, val pixels: BooleanArray) {
  operator fun get(x: Int, y: Int): Boolean = pixels[y * width + x]

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is QrMatrix) return false

    if (width != other.width) return false
    if (height != other.height) return false
    if (!pixels.contentEquals(other.pixels)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = width
    result = 31 * result + height
    result = 31 * result + pixels.contentHashCode()
    return result
  }
}

expect fun createQrMatrix(content: String): QrMatrix?
