package com.anjo.filebrowser.ui.files

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import com.anjo.filebrowser.ui.model.PreviewContent
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.EncodedOrigin
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

internal const val MAX_PREVIEW_PIXELS = 4096L * 4096
private val SCALES = listOf(1, 2, 4, 8)
private val SCALABLE_FORMATS = setOf(EncodedImageFormat.JPEG, EncodedImageFormat.WEBP)

internal fun previewScale(
    width: Int,
    height: Int,
): Int? = SCALES.firstOrNull { width.divideUp(it).toLong() * height.divideUp(it) <= MAX_PREVIEW_PIXELS }

internal fun previewImage(
    bytes: ByteArray,
    decode: (ByteArray) -> ImageBitmap = ByteArray::decodeToImageBitmap,
): PreviewContent {
    val data = Data.makeFromBytes(bytes)
    val codec =
        try {
            Codec.makeFromData(data)
        } catch (e: IllegalArgumentException) {
            data.close()
            return PreviewContent.Unreadable
        }
    return try {
        val scale = previewScale(codec.width, codec.height)
        when {
            scale == 1 -> PreviewContent.Image(decode(bytes))
            scale == null || codec.encodedImageFormat !in SCALABLE_FORMATS -> PreviewContent.TooManyPixels(codec.width, codec.height)
            else -> PreviewContent.Image(codec.decodeScaled(scale))
        }
    } catch (e: IllegalArgumentException) {
        PreviewContent.Unreadable
    } finally {
        codec.close()
        data.close()
    }
}

private fun Codec.decodeScaled(scale: Int): ImageBitmap {
    val decoded = Bitmap()
    try {
        decoded.allocPixels(ImageInfo.makeN32Premul(width.divideUp(scale), height.divideUp(scale)))
        readPixels(decoded)
    } catch (e: IllegalArgumentException) {
        decoded.close()
        throw e
    }
    return decoded.setImmutable().oriented(encodedOrigin).asComposeImageBitmap()
}

private fun Bitmap.oriented(origin: EncodedOrigin): Bitmap {
    if (origin == EncodedOrigin.TOP_LEFT) return this
    val (orientedWidth, orientedHeight) = if (origin.swapsWidthHeight()) height to width else width to height
    val result = Bitmap()
    result.allocPixels(ImageInfo.makeN32Premul(orientedWidth, orientedHeight))
    val image = Image.makeFromBitmap(this)
    Canvas(result).concat(origin.toMatrix(orientedWidth, orientedHeight)).drawImage(image, 0f, 0f).close()
    image.close()
    close()
    return result.setImmutable()
}

private fun Int.divideUp(divisor: Int): Int = ((toLong() + divisor - 1) / divisor).toInt()
