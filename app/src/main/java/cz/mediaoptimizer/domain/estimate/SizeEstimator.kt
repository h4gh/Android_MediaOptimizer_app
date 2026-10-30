package cz.mediaoptimizer.domain.estimate

import cz.mediaoptimizer.domain.model.ConversionSettings
import cz.mediaoptimizer.domain.model.MediaItem
import cz.mediaoptimizer.domain.model.MediaType
import cz.mediaoptimizer.domain.model.Preset
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Measured on sample photos of the batch: output JPEG bytes per pixel for each preset,
 * and how densely the samples' originals were compressed (bytes per source pixel).
 */
data class PhotoCalibration(
    val bytesPerPixel: Map<Preset, Float>,
    val sourceBytesPerPixel: Float,
)

/** Predicts output sizes before converting (TECH_DESIGN §8). */
object SizeEstimator {
    const val AUDIO_BITRATE = 128_000
    private const val CONTAINER_OVERHEAD = 1.02

    /** JPEG bytes per output pixel when no sample photo has been measured yet. */
    val DEFAULT_BYTES_PER_PIXEL = mapOf(
        Preset.SMALL to 0.11f,
        Preset.MEDIUM to 0.12f,
        Preset.LARGE to 0.13f,
    )

    /** Output photo size: longer side capped at [maxSide], never upscaled. */
    fun imageOutputSize(width: Int, height: Int, maxSide: Int): Pair<Int, Int> {
        val longer = max(width, height)
        if (longer <= maxSide || longer == 0) return width to height
        val scale = maxSide.toFloat() / longer
        return (width * scale).roundToInt().coerceAtLeast(1) to (height * scale).roundToInt().coerceAtLeast(1)
    }

    fun estimateImage(width: Int, height: Int, preset: Preset, bytesPerPixel: Float): Long {
        val (w, h) = imageOutputSize(width, height, preset.imageMaxSide)
        return (w.toLong() * h * bytesPerPixel).toLong()
    }

    fun estimateVideo(durationMs: Long, preset: Preset, removeAudio: Boolean): Long {
        val bitsPerSecond = preset.videoBitrate + if (removeAudio) 0 else AUDIO_BITRATE
        return (bitsPerSecond.toDouble() * durationMs / 1000 / 8 * CONTAINER_OVERHEAD).toLong()
    }

    /**
     * A photo whose original is more detailed (more bytes per pixel) than the samples
     * will also give a bigger copy, so the calibrated value is scaled by that ratio.
     */
    fun photoBytesPerPixel(
        preset: Preset,
        sourceBytes: Long,
        width: Int,
        height: Int,
        calibration: PhotoCalibration?,
    ): Float {
        val base = calibration?.bytesPerPixel?.get(preset) ?: return DEFAULT_BYTES_PER_PIXEL.getValue(preset)
        val pixels = width.toLong() * height
        if (pixels <= 0 || sourceBytes <= 0 || calibration.sourceBytesPerPixel <= 0f) return base
        val ratio = (sourceBytes.toFloat() / pixels) / calibration.sourceBytesPerPixel
        return base * ratio.coerceIn(0.4f, 2.5f)
    }

    fun estimate(item: MediaItem, settings: ConversionSettings, calibration: PhotoCalibration?): Long =
        when (item.type) {
            MediaType.IMAGE -> estimateImage(
                item.width,
                item.height,
                settings.preset,
                photoBytesPerPixel(settings.preset, item.sizeBytes, item.width, item.height, calibration),
            )
            MediaType.VIDEO -> estimateVideo(item.durationMs ?: 0, settings.preset, settings.removeAudio)
        }

    /** Percentage saved, 0..100 (negative would mean the copy is bigger). */
    fun savedPercent(before: Long, after: Long): Int =
        if (before <= 0) 0 else min(100L, 100 - after * 100 / before).toInt()
}
