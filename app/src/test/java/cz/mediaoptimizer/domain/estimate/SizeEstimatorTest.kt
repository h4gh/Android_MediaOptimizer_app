package cz.mediaoptimizer.domain.estimate

import cz.mediaoptimizer.domain.model.Preset
import org.junit.Assert.assertEquals
import org.junit.Test

class SizeEstimatorTest {

    @Test
    fun `photo is scaled so the longer side fits`() {
        assertEquals(1920 to 1440, SizeEstimator.imageOutputSize(4000, 3000, 1920))
        assertEquals(960 to 1280, SizeEstimator.imageOutputSize(3000, 4000, 1280))
    }

    @Test
    fun `photo is never upscaled`() {
        assertEquals(800 to 600, SizeEstimator.imageOutputSize(800, 600, 2560))
    }

    @Test
    fun `photo estimate uses output pixels`() {
        // 1920 x 1440 px at 0.1 B/px
        assertEquals(276_480L, SizeEstimator.estimateImage(4000, 3000, Preset.MEDIUM, 0.1f))
    }

    @Test
    fun `thirty second 720p video is about ten megabytes`() {
        val bytes = SizeEstimator.estimateVideo(30_000, Preset.MEDIUM, removeAudio = false)
        // (2.5 Mb/s + 128 kb/s) x 30 s / 8 x 1.02
        assertEquals(10_052_100.0, bytes.toDouble(), 2.0)
    }

    @Test
    fun `removing audio makes the video estimate smaller`() {
        val withAudio = SizeEstimator.estimateVideo(10_000, Preset.SMALL, removeAudio = false)
        val noAudio = SizeEstimator.estimateVideo(10_000, Preset.SMALL, removeAudio = true)
        assertEquals(SizeEstimator.AUDIO_BITRATE / 8 * 10 * 1.02, (withAudio - noAudio).toDouble(), 1.0)
    }

    @Test
    fun `saved percent`() {
        assertEquals(86, SizeEstimator.savedPercent(7_800_000, 1_092_000))
        assertEquals(0, SizeEstimator.savedPercent(0, 100))
    }
}
