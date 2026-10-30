package cz.mediaoptimizer.domain.model

import java.io.File

enum class Preset(
    val imageMaxSide: Int,
    val imageQuality: Int,
    val videoShortSide: Int,
    val videoBitrate: Int,
) {
    SMALL(1280, 75, 480, 1_000_000),
    MEDIUM(1920, 80, 720, 2_500_000),
    LARGE(2560, 85, 1080, 5_000_000),
}

data class ConversionSettings(
    val preset: Preset = Preset.MEDIUM,
    val stripMetadata: Boolean = true,
    val removeAudio: Boolean = false,
)

data class ConversionResult(
    val source: MediaItem,
    val outputFile: File?,
    val outputBytes: Long,
    val error: String? = null,
    /** True when the item was not processed (e.g. videos before Part 5). */
    val skipped: Boolean = false,
)
