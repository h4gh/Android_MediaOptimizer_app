package cz.mediaoptimizer.data

import android.content.Context
import java.io.File
import java.util.concurrent.TimeUnit

/** Converted copies live only in cache/out/<batch>/ and are removed when no longer needed. */
class CacheManager(context: Context) {
    val outRoot = File(context.cacheDir, "out")

    fun newBatchDir(): File {
        clearAll()
        return File(outRoot, System.currentTimeMillis().toString()).apply { mkdirs() }
    }

    fun clearAll() {
        outRoot.deleteRecursively()
    }

    /** Called on app start: drop batches older than 24 h (e.g. app was killed after sharing). */
    fun clearOld() {
        val limit = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
        outRoot.listFiles()?.filter { it.lastModified() < limit }?.forEach { it.deleteRecursively() }
    }
}
