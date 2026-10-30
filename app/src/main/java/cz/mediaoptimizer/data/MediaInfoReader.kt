package cz.mediaoptimizer.data

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import cz.mediaoptimizer.domain.model.MediaItem
import cz.mediaoptimizer.domain.model.MediaType

/** Reads name, size, dimensions and duration of a picked or shared file. */
class MediaInfoReader(private val context: Context) {

    /** Returns null for anything that is not a readable photo or video. */
    fun read(uri: Uri): MediaItem? = runCatching {
        val mime = context.contentResolver.getType(uri) ?: return null
        val type = when {
            mime.startsWith("image/") -> MediaType.IMAGE
            mime.startsWith("video/") -> MediaType.VIDEO
            else -> return null
        }
        var name = uri.lastPathSegment ?: "file"
        var size = 0L
        context.contentResolver.query(
            uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null,
        )?.use { c ->
            if (c.moveToFirst()) {
                c.getString(0)?.let { name = it }
                if (!c.isNull(1)) size = c.getLong(1)
            }
        }
        if (type == MediaType.IMAGE) readImage(uri, name, size) else readVideo(uri, name, size)
    }.getOrNull()

    private fun readImage(uri: Uri, name: String, size: Long): MediaItem {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        val rotated = context.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).rotationDegrees % 180 != 0
        } ?: false
        val w = if (rotated) opts.outHeight else opts.outWidth
        val h = if (rotated) opts.outWidth else opts.outHeight
        return MediaItem(uri, MediaType.IMAGE, name, size, w, h)
    }

    private fun readVideo(uri: Uri, name: String, size: Long): MediaItem {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(context, uri)
            fun int(key: Int) = r.extractMetadata(key)?.toIntOrNull() ?: 0
            val w = int(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val h = int(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotated = int(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION) % 180 != 0
            val duration = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            return MediaItem(
                uri, MediaType.VIDEO, name, size,
                width = if (rotated) h else w,
                height = if (rotated) w else h,
                durationMs = duration,
            )
        } finally {
            r.release()
        }
    }
}
