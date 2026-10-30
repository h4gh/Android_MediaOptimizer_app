package cz.mediaoptimizer.processing

import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaMetadataRetriever
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MimeTypes
import androidx.media3.container.Mp4LocationData
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.FrameDropEffect
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import cz.mediaoptimizer.domain.model.ConversionSettings
import cz.mediaoptimizer.domain.model.MediaItem
import cz.mediaoptimizer.domain.model.VideoParams
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

/** Re-encodes one video to a smaller H.264/AAC MP4 with Media3 Transformer. */
@OptIn(UnstableApi::class)
class VideoProcessor(private val context: Context) {

    suspend fun convert(
        item: MediaItem,
        params: VideoParams,
        settings: ConversionSettings,
        outFile: File,
        onProgress: (Float) -> Unit,
    ) {
        val location = if (settings.stripMetadata) null else readLocation(item)
        try {
            export(item, params, settings, location, outFile, strict = true, onProgress)
        } catch (e: ExportException) {
            // Some phones cannot tone-map HDR to SDR or keep a constant bitrate: retry without both
            outFile.delete()
            export(item, params, settings, location, outFile, strict = false, onProgress)
        }
    }

    private suspend fun readLocation(item: MediaItem): Mp4LocationData? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, item.uri)
            VideoMetadataMuxer.parseIso6709(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION))
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private suspend fun export(
        item: MediaItem,
        params: VideoParams,
        settings: ConversionSettings,
        location: Mp4LocationData?,
        outFile: File,
        strict: Boolean,
        onProgress: (Float) -> Unit,
    ) = withContext(Dispatchers.Main) {
        val inputShortSide = min(item.width, item.height)
        val videoEffects = buildList<Effect> {
            // Never upscale. Width follows from the aspect ratio; for portrait the short side is the width.
            if (inputShortSide == 0 || inputShortSide > params.shortSide) {
                val target = params.shortSide
                val height = if (item.height > item.width && item.width > 0) {
                    (item.height.toLong() * target / item.width).toInt()
                } else {
                    target
                }
                add(Presentation.createForHeight(height / 2 * 2))
            }
            // Only drops frames when the original has a higher frame rate
            params.frameRate?.let { add(FrameDropEffect.createDefaultFrameDropEffect(it.toFloat())) }
        }
        val source = androidx.media3.common.MediaItem.Builder()
            .setUri(item.uri)
            .apply {
                item.trim?.let { trim ->
                    setClippingConfiguration(
                        androidx.media3.common.MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(trim.startMs)
                            .setEndPositionMs(trim.endMs)
                            .build(),
                    )
                }
            }
            .build()
        val edited = EditedMediaItem.Builder(source)
            .setRemoveAudio(settings.removeAudio)
            .setEffects(Effects(emptyList(), videoEffects))
            .build()
        val composition = Composition.Builder(EditedMediaItemSequence(edited))
            .apply { if (strict) setHdrMode(Composition.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_OPEN_GL) }
            .build()

        val encoderFactory = DefaultEncoderFactory.Builder(context)
            .setRequestedVideoEncoderSettings(
                VideoEncoderSettings.Builder()
                    .setBitrate(params.bitrate)
                    // Constant bitrate: variable mode overshoots a lot at low bitrates on some phones
                    .apply { if (strict) setBitrateMode(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR) }
                    .build(),
            )
            .build()

        val finished = CompletableDeferred<Unit>()
        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .setEncoderFactory(encoderFactory)
            .setMuxerFactory(VideoMetadataMuxer.Factory(settings.stripMetadata, location))
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    finished.complete(Unit)
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException,
                ) {
                    finished.completeExceptionally(exportException)
                }
            })
            .build()

        transformer.start(composition, outFile.absolutePath)

        // Transformer has no progress callback: poll it
        val holder = ProgressHolder()
        val poller = launch {
            while (true) {
                if (transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                    onProgress(holder.progress / 100f)
                }
                delay(200)
            }
        }
        try {
            finished.await()
        } catch (e: CancellationException) {
            transformer.cancel()
            throw e
        } finally {
            poller.cancel()
        }
    }
}
