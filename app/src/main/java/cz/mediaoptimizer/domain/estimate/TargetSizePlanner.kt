package cz.mediaoptimizer.domain.estimate

import cz.mediaoptimizer.domain.model.ImageParams
import cz.mediaoptimizer.domain.model.ItemPlan
import cz.mediaoptimizer.domain.model.Preset
import cz.mediaoptimizer.domain.model.VideoParams
import kotlin.math.max
import kotlin.math.min

/** "Fit into X MB": picks photo quality/size and video bitrate so the batch fits (TECH_DESIGN §9). */
object TargetSizePlanner {
    /** Part of the limit kept free, because estimates are never exact. */
    const val RESERVE = 0.97
    const val MIN_VIDEO_BITRATE = 300_000
    private val MAX_VIDEO_BITRATE = Preset.LARGE.videoBitrate

    /** Photo settings from best to smallest; all photos of the batch use the same step. */
    val PHOTO_STEPS = listOf(
        ImageParams(1920, 80),
        ImageParams(1920, 70),
        ImageParams(1920, 60),
        ImageParams(1600, 60),
        ImageParams(1280, 60),
        ImageParams(1280, 50),
        ImageParams(1024, 50),
        ImageParams(800, 50),
        ImageParams(640, 50),
    )

    /** What the planner needs to know about one item (no Android types, so it is unit-testable). */
    data class Source(
        val isVideo: Boolean,
        val width: Int,
        val height: Int,
        val sourceBytes: Long,
        val durationMs: Long = 0,
    )

    data class Plan(
        val items: List<ItemPlan>,
        val estimates: List<Long>,
        /** False when even the smallest settings exceed the limit. */
        val fits: Boolean,
    ) {
        val estimatedBytes get() = estimates.sum()
    }

    fun plan(
        sources: List<Source>,
        targetBytes: Long,
        removeAudio: Boolean,
        calibration: PhotoCalibration?,
    ): Plan {
        val budget = (targetBytes * RESERVE).toLong()
        val photos = sources.filter { !it.isVideo }
        val videoMs = sources.filter { it.isVideo }.sumOf { it.durationMs }

        fun photoBytes(step: ImageParams) = photos.sumOf {
            SizeEstimator.estimateImage(it.width, it.height, it.sourceBytes, step, calibration)
        }

        // Videos must keep at least the minimum bitrate; photos get what is left
        val videoMin = if (videoMs > 0) SizeEstimator.estimateVideo(videoMs, MIN_VIDEO_BITRATE, removeAudio) else 0
        val step = PHOTO_STEPS.firstOrNull { photoBytes(it) <= budget - videoMin } ?: PHOTO_STEPS.last()
        val photoTotal = photoBytes(step)

        // The rest is shared by all videos in proportion to their length, i.e. one common bitrate
        val bitrate = SizeEstimator.videoBitrateFor(budget - photoTotal, videoMs, removeAudio)
            .coerceAtMost(MAX_VIDEO_BITRATE)
        val video = VideoParams(shortSideFor(max(bitrate, MIN_VIDEO_BITRATE)), max(bitrate, MIN_VIDEO_BITRATE))
        val fits = photoTotal <= budget - videoMin && (videoMs == 0L || bitrate >= MIN_VIDEO_BITRATE)

        val plan = ItemPlan(step, video)
        return Plan(
            items = sources.map { plan },
            estimates = sources.map {
                if (it.isVideo) {
                    SizeEstimator.estimateVideo(it.durationMs, video.bitrate, removeAudio)
                } else {
                    SizeEstimator.estimateImage(it.width, it.height, it.sourceBytes, step, calibration)
                }
            },
            fits = fits,
        )
    }

    /** Lower bitrates look better at a lower resolution. */
    fun shortSideFor(bitrate: Int) = when {
        bitrate < 1_500_000 -> 480
        bitrate < 3_500_000 -> 720
        else -> 1080
    }

    /**
     * Used after converting when the batch is still over the limit: settings for converting
     * one item again so it ends up about [factor] times its current size.
     */
    fun shrink(source: Source, current: ItemPlan, factor: Double): ItemPlan {
        if (source.isVideo) {
            val bitrate = max(MIN_VIDEO_BITRATE, (current.video.bitrate * factor).toInt())
            return current.copy(video = VideoParams(min(current.video.shortSide, shortSideFor(bitrate)), bitrate))
        }
        val now = relativePhotoSize(source, current.image)
        val next = PHOTO_STEPS.firstOrNull { relativePhotoSize(source, it) <= now * factor }
            ?: PHOTO_STEPS.last()
        return current.copy(image = next)
    }

    private fun relativePhotoSize(source: Source, params: ImageParams): Double {
        val (w, h) = SizeEstimator.imageOutputSize(source.width, source.height, params.maxSide)
        return w.toDouble() * h * SizeEstimator.qualityFactor(params.quality)
    }
}
