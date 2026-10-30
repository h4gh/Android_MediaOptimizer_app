package cz.mediaoptimizer.domain.model

import android.net.Uri

enum class MediaType { IMAGE, VIDEO }

/** Part of a video to keep, in milliseconds from its start. */
data class TrimRange(val startMs: Long, val endMs: Long) {
    val durationMs get() = endMs - startMs
}

data class MediaItem(
    val uri: Uri,
    val type: MediaType,
    val name: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long? = null, // video only
    val trim: TrimRange? = null, // video only, null = whole video
) {
    /** Length of the output video (after trimming). */
    val effectiveDurationMs get() = trim?.durationMs ?: durationMs ?: 0
}
