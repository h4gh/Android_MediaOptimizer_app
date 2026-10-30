package cz.mediaoptimizer.processing

import android.content.Context
import cz.mediaoptimizer.domain.model.ConversionResult
import cz.mediaoptimizer.domain.model.ConversionSettings
import cz.mediaoptimizer.domain.model.MediaItem
import cz.mediaoptimizer.domain.model.MediaType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

/** Converts a batch one file at a time into cache/out/<batch>/. */
class ConversionQueue(private val context: Context) {
    private val images = ImageProcessor(context)

    suspend fun run(
        items: List<MediaItem>,
        settings: ConversionSettings,
        onProgress: (done: Int, result: ConversionResult) -> Unit,
    ): List<ConversionResult> = withContext(Dispatchers.Default) {
        val outRoot = File(context.cacheDir, "out")
        outRoot.deleteRecursively()
        val dir = File(outRoot, System.currentTimeMillis().toString()).apply { mkdirs() }
        val usedNames = mutableSetOf<String>()

        items.mapIndexed { index, item ->
            ensureActive()
            val result = when (item.type) {
                MediaType.VIDEO -> ConversionResult(item, null, 0, skipped = true)
                MediaType.IMAGE -> {
                    val out = File(dir, uniqueName(item.name, "jpg", usedNames))
                    try {
                        withContext(Dispatchers.IO) { images.convert(item, settings, out) }
                        ConversionResult(item, out, out.length())
                    } catch (e: CancellationException) {
                        out.delete()
                        throw e
                    } catch (e: Exception) {
                        out.delete()
                        ConversionResult(item, null, 0, error = e.message ?: e.javaClass.simpleName)
                    }
                }
            }
            onProgress(index + 1, result)
            result
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
