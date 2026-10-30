package cz.mediaoptimizer.processing

import androidx.annotation.OptIn
import androidx.media3.common.Metadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.container.MdtaMetadataEntry
import androidx.media3.container.Mp4LocationData
import androidx.media3.transformer.DefaultMuxer
import androidx.media3.transformer.Muxer

/**
 * Controls the GPS location written into a converted video.
 * - [stripLocation]: drop every location entry Transformer would copy from the input.
 * - otherwise [location] (read from the original) is written explicitly, because the
 *   location does not reliably survive re-encoding on its own.
 */
@OptIn(UnstableApi::class)
class VideoMetadataMuxer(
    private val delegate: Muxer,
    private val stripLocation: Boolean,
    private val location: Mp4LocationData?,
) : Muxer by delegate {

    init {
        if (!stripLocation && location != null) delegate.addMetadataEntry(location)
    }

    override fun addMetadataEntry(metadataEntry: Metadata.Entry) {
        // Skip input locations when stripping, and avoid a duplicate when we wrote our own
        val skip = isLocation(metadataEntry) && (stripLocation || location != null)
        if (!skip) delegate.addMetadataEntry(metadataEntry)
    }

    class Factory(
        private val stripLocation: Boolean,
        private val location: Mp4LocationData?,
        private val delegate: Muxer.Factory = DefaultMuxer.Factory(),
    ) : Muxer.Factory by delegate {
        override fun create(path: String): Muxer =
            VideoMetadataMuxer(delegate.create(path), stripLocation, location)
    }

    companion object {
        fun isLocation(entry: Metadata.Entry): Boolean = when (entry) {
            is Mp4LocationData -> true
            // e.g. "com.apple.quicktime.location.ISO6709"
            is MdtaMetadataEntry -> entry.key.contains("location", ignoreCase = true)
            else -> false
        }

        private val ISO6709 = Regex("""^([+-]\d+(?:\.\d+)?)([+-]\d+(?:\.\d+)?)""")

        /** Parses MediaMetadataRetriever's location, e.g. "+50.0755+014.4378/". */
        fun parseIso6709(value: String?): Mp4LocationData? {
            val match = value?.let { ISO6709.find(it.trim()) } ?: return null
            val lat = match.groupValues[1].toFloatOrNull() ?: return null
            val lon = match.groupValues[2].toFloatOrNull() ?: return null
            return Mp4LocationData(lat, lon)
        }
    }
}
