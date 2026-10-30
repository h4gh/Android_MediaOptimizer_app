package cz.mediaoptimizer.processing

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
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
        settings: ConversionSettings,
        outFile: File,
        onProgress: (Float) -> Unit,
    ) {
        try {
            export(item, settings, outFile, toneMapHdr = true, onProgress)
        } catch (e: ExportException) {
            // Some phones cannot tone-map HDR to SDR: try once more without it
            outFile.delete()
            export(item, settings, outFile, toneMapHdr = false, onProgress)
        }
    }

    private suspend fun export(
        item: MediaItem,
        settings: ConversionSettings,
        outFile: File,
        toneMapHdr: Boolean,
        onProgress: (Float) -> Unit,
    ) = withContext(Dispatchers.Main) {
        val preset = settings.preset
        val inputShortSide = min(item.width, item.height)
        val videoEffects = buildList<Effect> {
            // Never upscale. Width follows from the aspect ratio; for portrait the short side is the width.
            if (inputShortSide == 0 || inputShortSide > preset.videoShortSide) {
                val target = preset.videoShortSide
                val height = if (item.height > item.width && item.width > 0) {
                    (item.height.toLong() * target / item.width).toInt()
                } else {
                    target
                }
                add(Presentation.createForHeight(height / 2 * 2))
            }
        }
        val edited = EditedMediaItem.Builder(androidx.media3.common.MediaItem.fromUri(item.uri))
            .setRemoveAudio(settings.removeAudio)
            .setEffects(Effects(emptyList(), videoEffects))
            .build()
        val composition = Composition.Builder(EditedMediaItemSequence(edited))
            .apply { if (toneMapHdr) setHdrMode(Composition.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_OPEN_GL) }
            .build()

        val encoderFactory = DefaultEncoderFactory.Builder(context)
            .setRequestedVideoEncoderSettings(
                VideoEncoderSettings.Builder().setBitrate(preset.videoBitrate).build(),
            )
            .build()

        val finished = CompletableDeferred<Unit>()
        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .setEncoderFactory(encoderFactory)
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
