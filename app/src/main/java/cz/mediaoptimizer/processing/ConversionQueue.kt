package cz.mediaoptimizer.processing

import android.content.Context
import cz.mediaoptimizer.data.CacheManager
import cz.mediaoptimizer.domain.model.ConversionResult
import cz.mediaoptimizer.domain.model.ConversionSettings
import cz.mediaoptimizer.domain.model.ItemPlan
import cz.mediaoptimizer.domain.model.MediaItem
import cz.mediaoptimizer.domain.model.MediaType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

/** Converts a batch one file at a time into cache/out/<batch>/. */
class ConversionQueue(context: Context, private val cache: CacheManager) {
    private val images = ImageProcessor(context)
    private val videos = VideoProcessor(context)

    suspend fun run(
        items: List<MediaItem>,
        plans: List<ItemPlan>,
        settings: ConversionSettings,
        onFileProgress: (Float) -> Unit,
        onProgress: (done: Int, result: ConversionResult) -> Unit,
    ): List<ConversionResult> = withContext(Dispatchers.Default) {
        val dir = cache.newBatchDir()
        val usedNames = mutableSetOf<String>()

        items.mapIndexed { index, item ->
            ensureActive()
            val ext = if (item.type == MediaType.VIDEO) "mp4" else "jpg"
            val out = File(dir, uniqueName(item.name, ext, usedNames))
            val result = convertOne(item, plans[index], settings, out, onFileProgress)
            onProgress(index + 1, result)
            result
        }
    }

    /** Converts one item into [out] (overwritten if it exists, e.g. when converting again with a lower target). */
    suspend fun convertOne(
        item: MediaItem,
        plan: ItemPlan,
        settings: ConversionSettings,
        out: File,
        onFileProgress: (Float) -> Unit,
    ): ConversionResult {
        onFileProgress(0f)
        return try {
            when (item.type) {
                MediaType.IMAGE -> withContext(Dispatchers.IO) { images.convert(item, plan.image, settings, out) }
                MediaType.VIDEO -> videos.convert(item, plan.video, settings, out, onFileProgress)
            }
            ConversionResult(item, out, out.length())
        } catch (e: CancellationException) {
            out.delete()
            throw e
        } catch (e: Exception) {
            out.delete()
            ConversionResult(item, null, 0, error = e.message ?: e.javaClass.simpleName)
        }
    }

    private fun uniqueName(original: String, ext: String, used: MutableSet<String>): String {
        val base = original.substringBeforeLast('.').ifBlank { "file" }
        var name = "${base}_small.$ext"
        var n = 2
        while (!used.add(name)) name = "${base}_small_${n++}.$ext"
        return name
    }
}
