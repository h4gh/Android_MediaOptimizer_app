package cz.mediaoptimizer.data

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Packs converted files into one ZIP next to them; media are already compressed, so entries are STORED. */
object ZipHelper {

    fun zip(files: List<File>): File {
        val dir = files.first().parentFile!!
        val name = "MediaOptimizer_" + SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date()) + ".zip"
        val out = File(dir, name)
        if (out.exists()) return out // already packed (shared twice)
        val tmp = File(dir, "$name.part")
        ZipOutputStream(tmp.outputStream().buffered()).use { zip ->
            files.forEach { file ->
                zip.putNextEntry(storedEntry(file))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        tmp.renameTo(out)
        return out
    }

    // STORED entries need size and CRC up front.
    private fun storedEntry(file: File) = ZipEntry(file.name).apply {
        method = ZipEntry.STORED
        size = file.length()
        compressedSize = file.length()
        crc = CRC32().also { crc ->
            file.inputStream().use { input ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    crc.update(buf, 0, n)
                }
            }
        }.value
        time = file.lastModified()
    }
}
