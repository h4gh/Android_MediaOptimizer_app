package cz.mediaoptimizer.domain.model

import java.io.File

/** How one photo is encoded: longer side cap and JPEG quality. */
data class ImageParams(val maxSide: Int, val quality: Int)

/** How one video is encoded: short side cap and video bitrate (b/s). */
data class VideoParams(val shortSide: Int, val bitrate: Int)

/** Encoding of one item; only the part matching its type is used. */
data class ItemPlan(val image: ImageParams, val video: VideoParams)

enum class Preset(
    val imageMaxSide: Int,
    val imageQuality: Int,
    val videoShortSide: Int,
    val videoBitrate: Int,
) {
    SMALL(1280, 75, 480, 1_000_000),
    MEDIUM(1920, 80, 720, 2_500_000),
    LARGE(2560, 85, 1080, 5_000_000);

    val plan get() = ItemPlan(ImageParams(imageMaxSide, imageQuality), VideoParams(videoShortSide, videoBitrate))
}

data class ConversionSettings(
    val preset: Preset = Preset.MEDIUM,
    val stripMetadata: Boolean = true,
    val removeAudio: Boolean = false,
    /** "Fit into X MB" mode: the whole batch should fit into this many bytes; the preset is then ignored. */
    val targetBytes: Long? = null,
)

data class ConversionResult(
    val source: MediaItem,
    val outputFile: File?,
    val outputBytes: Long,
    val error: String? = null,
    /** True when the item was not processed (e.g. videos before Part 5). */
    val skipped: Boolean = false,
)
