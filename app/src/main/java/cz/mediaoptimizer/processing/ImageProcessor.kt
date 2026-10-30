package cz.mediaoptimizer.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import androidx.exifinterface.media.ExifInterface
import cz.mediaoptimizer.domain.model.ConversionSettings
import cz.mediaoptimizer.domain.model.MediaItem
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/** Shrinks one photo to a JPEG copy; the original is never modified. */
class ImageProcessor(private val context: Context) {

    fun convert(item: MediaItem, settings: ConversionSettings, outFile: File) {
        val maxSide = settings.preset.imageMaxSide
        val source = ImageDecoder.createSource(context.contentResolver, item.uri)
        val decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val w = info.size.width
            val h = info.size.height
            val scale = maxSide.toFloat() / max(w, h)
            // Never upscale
            if (scale < 1f) {
                decoder.setTargetSize((w * scale).roundToInt().coerceAtLeast(1), (h * scale).roundToInt().coerceAtLeast(1))
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        // JPEG has no transparency: put transparent PNG/WebP on white instead of black
        val bitmap = if (decoded.hasAlpha()) {
            Bitmap.createBitmap(decoded.width, decoded.height, Bitmap.Config.ARGB_8888).also {
                Canvas(it).apply {
                    drawColor(Color.WHITE)
                    drawBitmap(decoded, 0f, 0f, null)
                }
                decoded.recycle()
            }
        } else {
            decoded
        }
        try {
            outFile.outputStream().use { out ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, settings.preset.imageQuality, out)) {
                    "JPEG compression failed"
                }
            }
        } finally {
            bitmap.recycle()
        }
        if (!settings.stripMetadata) copyExif(item, outFile)
    }

    /** Copies the useful EXIF tags; orientation stays "normal" because pixels are already rotated. */
    private fun copyExif(item: MediaItem, outFile: File) {
        runCatching {
            val src = context.contentResolver.openInputStream(item.uri)?.use { ExifInterface(it) } ?: return
            val dst = ExifInterface(outFile)
            for (tag in KEPT_TAGS) src.getAttribute(tag)?.let { dst.setAttribute(tag, it) }
            dst.saveAttributes()
        }
    }

    private companion object {
        val KEPT_TAGS = listOf(
            ExifInterface.TAG_DATETIME,
            ExifInterface.TAG_DATETIME_ORIGINAL,
            ExifInterface.TAG_DATETIME_DIGITIZED,
            ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
            ExifInterface.TAG_MAKE,
            ExifInterface.TAG_MODEL,
            ExifInterface.TAG_GPS_LATITUDE,
            ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE,
            ExifInterface.TAG_GPS_LONGITUDE_REF,
            ExifInterface.TAG_GPS_ALTITUDE,
            ExifInterface.TAG_GPS_ALTITUDE_REF,
        )
    }
}
