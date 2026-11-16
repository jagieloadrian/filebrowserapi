package com.anjo.filebrowser.ui.files

import androidx.compose.ui.graphics.toPixelMap
import com.anjo.filebrowser.ui.PNG_SIZE
import com.anjo.filebrowser.ui.model.PreviewContent
import com.anjo.filebrowser.ui.png
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.jetbrains.skia.Color
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Surface

private const val OVER_LIMIT = 4100

private data class ScaleCase(
    val width: Int,
    val height: Int,
    val scale: Int?,
)

private fun encoded(
    width: Int,
    height: Int,
    format: EncodedImageFormat,
): ByteArray {
    val surface = Surface.makeRasterN32Premul(width, height)
    try {
        surface.canvas.clear(Color.RED)
        return checkNotNull(surface.makeImageSnapshot().encodeToData(format)) { "Could not encode $format" }.bytes
    } finally {
        surface.close()
    }
}

private fun rotatedRight(jpeg: ByteArray): ByteArray {
    val tiff = byteArrayOf(0x4D, 0x4D, 0, 0x2A, 0, 0, 0, 8, 0, 1, 0x01, 0x12, 0, 3, 0, 0, 0, 1, 0, 6, 0, 0, 0, 0, 0, 0)
    val exif = "Exif".encodeToByteArray() + byteArrayOf(0, 0) + tiff
    val length = exif.size + 2
    val segment = byteArrayOf(0xFF.toByte(), 0xE1.toByte(), (length shr 8).toByte(), length.toByte()) + exif
    return jpeg.copyOfRange(0, 2) + segment + jpeg.copyOfRange(2, jpeg.size)
}

private val notDecoded: (ByteArray) -> Nothing = { error("The full decoder must not run") }

class PreviewImageTest :
    FunSpec({
        context("should pick the smallest downscale that fits 4096 × 4096 pixels") {
            withTests(
                nameFn = { "${it.width} × ${it.height}" },
                ScaleCase(4096, 4096, 1),
                ScaleCase(100_000, 100, 1),
                ScaleCase(4097, 4096, 2),
                ScaleCase(8192, 8192, 2),
                ScaleCase(8193, 8192, 4),
                ScaleCase(32_768, 32_768, 8),
                ScaleCase(32_769, 32_768, null),
            ) { case ->
                // when
                val scale = previewScale(case.width, case.height)

                // then
                scale shouldBe case.scale
            }
        }

        test("should decode a small PNG whole") {
            // when
            val content = previewImage(png())

            // then
            content.shouldBeInstanceOf<PreviewContent.Image>().bitmap.width shouldBe PNG_SIZE
        }

        test("should decode a small JPEG whole") {
            // when
            val content = previewImage(encoded(3, 2, EncodedImageFormat.JPEG))

            // then
            val bitmap = content.shouldBeInstanceOf<PreviewContent.Image>().bitmap
            bitmap.width to bitmap.height shouldBe (3 to 2)
        }

        test("should decode a JPEG over the pixel limit at half size without the full decoder") {
            // when
            val content = previewImage(encoded(OVER_LIMIT, OVER_LIMIT, EncodedImageFormat.JPEG), notDecoded)

            // then
            val bitmap = content.shouldBeInstanceOf<PreviewContent.Image>().bitmap
            bitmap.width to bitmap.height shouldBe (OVER_LIMIT / 2 to OVER_LIMIT / 2)
        }

        test("should turn a JPEG over the pixel limit upright as its EXIF orientation says") {
            // given
            val jpeg = rotatedRight(encoded(OVER_LIMIT, OVER_LIMIT + 100, EncodedImageFormat.JPEG))

            // when
            val content = previewImage(jpeg, notDecoded)

            // then
            val bitmap = content.shouldBeInstanceOf<PreviewContent.Image>().bitmap
            bitmap.width to bitmap.height shouldBe ((OVER_LIMIT + 100) / 2 to OVER_LIMIT / 2)
            val pixels = bitmap.toPixelMap()
            listOf(pixels[0, 0], pixels[bitmap.width - 1, bitmap.height - 1]).map { it.alpha } shouldBe listOf(1f, 1f)
        }

        test("should call a PNG over the pixel limit too large without decoding it") {
            // when
            val content = previewImage(encoded(OVER_LIMIT, OVER_LIMIT, EncodedImageFormat.PNG), notDecoded)

            // then
            content shouldBe PreviewContent.TooManyPixels(OVER_LIMIT, OVER_LIMIT)
        }

        test("should say it cannot preview an image whose header is cut off") {
            // when
            val content = previewImage(png().copyOf(20), notDecoded)

            // then
            content shouldBe PreviewContent.Unreadable
        }
    })
