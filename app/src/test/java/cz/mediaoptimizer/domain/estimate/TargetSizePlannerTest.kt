package cz.mediaoptimizer.domain.estimate

import cz.mediaoptimizer.domain.model.ImageParams
import cz.mediaoptimizer.domain.model.Preset
import cz.mediaoptimizer.domain.model.VideoParams
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TargetSizePlannerTest {
    // 12 MP photo; with default calibration Medium is 1920 x 1440 x 0.12 B/px = 331 776 B
    private val photo = TargetSizePlanner.Source(isVideo = false, width = 4000, height = 3000, sourceBytes = 3_000_000)
    private fun video(seconds: Int) =
        TargetSizePlanner.Source(isVideo = true, width = 1920, height = 1080, sourceBytes = 0, durationMs = seconds * 1000L)

    @Test
    fun `photos keep Medium when the limit is big enough`() {
        val plan = TargetSizePlanner.plan(List(10) { photo }, 25_000_000, removeAudio = false, calibration = null)
        assertEquals(ImageParams(1920, 80), plan.items.first().image)
        assertTrue(plan.fits)
    }

    @Test
    fun `photos step down until they fit`() {
        // 10 photos into 2 MB: needs <= 194 kB each -> 1600 px at quality 60 (~150 kB)
        val plan = TargetSizePlanner.plan(List(10) { photo }, 2_000_000, removeAudio = false, calibration = null)
        assertEquals(ImageParams(1600, 60), plan.items.first().image)
        assertTrue(plan.estimatedBytes <= 2_000_000 * TargetSizePlanner.RESERVE)
        assertTrue(plan.fits)
    }

    @Test
    fun `video gets the rest of the limit`() {
        // 9.7 MB x 8 / 1.02 / 60 s - 128 kb/s = ~1.14 Mb/s -> 480p
        val plan = TargetSizePlanner.plan(listOf(video(60)), 10_000_000, removeAudio = false, calibration = null)
        val v = plan.items.first().video
        assertEquals(480, v.shortSide)
        assertEquals(1_140_000.0, v.bitrate.toDouble(), 2_000.0)
        assertTrue(plan.estimatedBytes <= 10_000_000)
        assertTrue(plan.fits)
    }

    @Test
    fun `video bitrate is capped at Large`() {
        val plan = TargetSizePlanner.plan(listOf(video(5)), 100_000_000, removeAudio = false, calibration = null)
        assertEquals(VideoParams(1080, Preset.LARGE.videoBitrate), plan.items.first().video)
    }

    @Test
    fun `too small limit is reported`() {
        val plan = TargetSizePlanner.plan(listOf(video(120)), 1_000_000, removeAudio = false, calibration = null)
        assertFalse(plan.fits)
        assertEquals(TargetSizePlanner.MIN_VIDEO_BITRATE, plan.items.first().video.bitrate)
    }

    @Test
    fun `shrinking a video lowers its bitrate`() {
        val current = Preset.MEDIUM.plan
        val next = TargetSizePlanner.shrink(video(30), current, 0.5)
        assertEquals(1_250_000, next.video.bitrate)
        assertEquals(480, next.video.shortSide)
    }

    @Test
    fun `shrinking a photo moves to a smaller step`() {
        val next = TargetSizePlanner.shrink(photo, Preset.MEDIUM.plan, 0.7)
        // 1920 px q60 is 0.65 of 1920 px q80
        assertEquals(ImageParams(1920, 60), next.image)
    }

    @Test
    fun `quality factor is interpolated`() {
        assertEquals(1.0f, SizeEstimator.qualityFactor(80), 0.0001f)
        assertEquals(0.928f, SizeEstimator.qualityFactor(77), 0.0001f)
        assertEquals(0.55f, SizeEstimator.qualityFactor(30), 0.0001f)
    }
}
