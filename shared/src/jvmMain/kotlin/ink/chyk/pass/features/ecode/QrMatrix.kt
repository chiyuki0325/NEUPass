package ink.chyk.pass.features.ecode

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

// jvm 侧采用 zxing 生成二维码
// android / desktop
actual fun createQrMatrix(content: String): QrMatrix? {
    val hints = mapOf(
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
        EncodeHintType.MARGIN to 1,
    )
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
    return QrMatrix(matrix.width, matrix.height, BooleanArray(matrix.width * matrix.height) { index ->
        matrix[index % matrix.width, index / matrix.width]
    })
}
