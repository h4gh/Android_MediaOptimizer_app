package cz.mediaoptimizer.domain.estimate

import cz.mediaoptimizer.domain.model.ConversionSettings
import cz.mediaoptimizer.domain.model.MediaItem
import cz.mediaoptimizer.domain.model.MediaType
import cz.mediaoptimizer.domain.model.Preset
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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

    fun estimate(item: MediaItem, settings: ConversionSettings, bytesPerPixel: Map<Preset, Float>): Long =
        when (item.type) {
            MediaType.IMAGE -> estimateImage(
                item.width,
                item.height,
                settings.preset,
                bytesPerPixel[settings.preset] ?: DEFAULT_BYTES_PER_PIXEL.getValue(settings.preset),
            )
            MediaType.VIDEO -> estimateVideo(item.durationMs ?: 0, settings.preset, settings.removeAudio)
        }

    /** Percentage saved, 0..100 (negative would mean the copy is bigger). */
    fun savedPercent(before: Long, after: Long): Int =
        if (before <= 0) 0 else min(100L, 100 - after * 100 / before).toInt()
}
