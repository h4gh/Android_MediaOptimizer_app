package cz.mediaoptimizer.domain.model

import android.net.Uri

enum class MediaType { IMAGE, VIDEO }

data class MediaItem(
    val uri: Uri,
    val type: MediaType,
    val name: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long? = null, // video only
)
