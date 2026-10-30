package cz.mediaoptimizer.processing

import androidx.annotation.OptIn
import androidx.media3.common.Metadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.container.MdtaMetadataEntry
import androidx.media3.container.Mp4LocationData
import androidx.media3.transformer.DefaultMuxer
import androidx.media3.transformer.Muxer

/**
 * Transformer copies metadata of the input video (including GPS location) into the output.
 * This muxer drops location entries so a shared video does not reveal where it was taken.
 */
@OptIn(UnstableApi::class)
class LocationStrippingMuxer(private val delegate: Muxer) : Muxer by delegate {

    override fun addMetadataEntry(metadataEntry: Metadata.Entry) {
        if (!isLocation(metadataEntry)) delegate.addMetadataEntry(metadataEntry)
    }

    class Factory(private val delegate: Muxer.Factory = DefaultMuxer.Factory()) : Muxer.Factory by delegate {
        override fun create(path: String): Muxer = LocationStrippingMuxer(delegate.create(path))
    }

    companion object {
        fun isLocation(entry: Metadata.Entry): Boolean = when (entry) {
            is Mp4LocationData -> true
            // e.g. "com.apple.quicktime.location.ISO6709"
            is MdtaMetadataEntry -> entry.key.contains("location", ignoreCase = true)
            else -> false
        }
    }
}
