@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package ink.chyk.pass.features.ecode

import kotlinx.cinterop.get
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFRelease
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGDataProviderCopyData
import platform.CoreGraphics.CGImageGetBitsPerPixel
import platform.CoreGraphics.CGImageGetBytesPerRow
import platform.CoreGraphics.CGImageGetDataProvider
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGImageRelease
import platform.CoreImage.CIContext
import platform.CoreImage.CIFilter
import platform.CoreImage.createCGImage
import platform.CoreImage.filterWithName
import platform.CoreImage.kCIFormatRGBA8
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.dataUsingEncoding
import platform.Foundation.setValue

actual fun createQrMatrix(content: String): QrMatrix? {
  if (content.isEmpty()) return null

  val message = (content as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return null
  val filter = CIFilter.filterWithName("CIQRCodeGenerator") ?: return null
  filter.setValue(message, forKey = "inputMessage")
  filter.setValue("H", forKey = "inputCorrectionLevel")
  val image = filter.outputImage ?: return null

  val colorSpace = CGColorSpaceCreateDeviceRGB()
  val cgImage = try {
    CIContext.contextWithOptions(null).createCGImage(
      image = image,
      fromRect = image.extent,
      format = kCIFormatRGBA8,
      colorSpace = colorSpace,
    )
  } finally {
    CGColorSpaceRelease(colorSpace)
  } ?: return null

  try {
    val width = CGImageGetWidth(cgImage).toInt()
    val height = CGImageGetHeight(cgImage).toInt()
    val bytesPerPixel = CGImageGetBitsPerPixel(cgImage).toInt() / 8
    val bytesPerRow = CGImageGetBytesPerRow(cgImage).toInt()
    if (width <= 0 || height <= 0 || bytesPerPixel < 3 || bytesPerRow < width * bytesPerPixel) return null

    val dataProvider = CGImageGetDataProvider(cgImage) ?: return null
    val bitmapData = CGDataProviderCopyData(dataProvider) ?: return null
    try {
      val bytes = CFDataGetBytePtr(bitmapData) ?: return null
      if (CFDataGetLength(bitmapData) < bytesPerRow.toLong() * height) return null

      val pixels = BooleanArray(width * height) { index ->
        val x = index % width
        val y = index / width
        bytes[y * bytesPerRow + x * bytesPerPixel].toInt() < 128
      }
      return QrMatrix(width, height, pixels)
    } finally {
      CFRelease(bitmapData)
    }
  } finally {
    CGImageRelease(cgImage)
  }
}
