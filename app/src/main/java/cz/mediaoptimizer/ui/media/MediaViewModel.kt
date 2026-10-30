package cz.mediaoptimizer.ui.media

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cz.mediaoptimizer.data.CacheManager
import cz.mediaoptimizer.data.MediaInfoReader
import cz.mediaoptimizer.domain.model.ConversionResult
import cz.mediaoptimizer.domain.model.ConversionSettings
import cz.mediaoptimizer.domain.estimate.PhotoCalibration
import cz.mediaoptimizer.domain.estimate.SizeEstimator
import cz.mediaoptimizer.domain.model.MediaItem
import cz.mediaoptimizer.domain.model.MediaType
import cz.mediaoptimizer.domain.model.Preset
import cz.mediaoptimizer.processing.ImageProcessor
import cz.mediaoptimizer.processing.ConversionQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MediaSelectionState(
    val items: List<MediaItem> = emptyList(),
    val loading: Boolean = false,
    /** Files that could not be read (not a photo/video, or unavailable). */
    val skipped: Int = 0,
    /** Increases whenever media arrive from the Android share sheet. */
    val shareCount: Int = 0,
    /** Measured on sample photos; null until ready (defaults are used meanwhile). */
    val calibration: PhotoCalibration? = null,
)

data class ConversionState(
    val running: Boolean = false,
    val finished: Boolean = false,
    val done: Int = 0,
    /** Progress of the file being converted now, 0..1 (videos). */
    val current: Float = 0f,
    val total: Int = 0,
    val results: List<ConversionResult> = emptyList(),
) {
    val converted get() = results.filter { it.outputFile != null }
    val beforeBytes get() = converted.sumOf { it.source.sizeBytes }
    val afterBytes get() = converted.sumOf { it.outputBytes }
}

/** Holds the selected media and the running conversion; shared by all screens. */
class MediaViewModel(app: Application) : AndroidViewModel(app) {
    private val reader = MediaInfoReader(app)
    private val cache = CacheManager(app).also { c ->
        viewModelScope.launch(Dispatchers.IO) { c.clearOld() }
    }
    private val queue = ConversionQueue(app, cache)
    private val images = ImageProcessor(app)
    private var calibrationJob: Job? = null
    private var conversionJob: Job? = null

    private val _state = MutableStateFlow(MediaSelectionState())
    val state: StateFlow<MediaSelectionState> = _state.asStateFlow()

    private val _conversion = MutableStateFlow(ConversionState())
    val conversion: StateFlow<ConversionState> = _conversion.asStateFlow()

    // Each selection / conversion gets an id so late updates from an older one are ignored.
    private var selectionId = 0
    @Volatile private var batchId = 0

    fun select(uris: List<Uri>, fromShare: Boolean = false) {
        if (uris.isEmpty()) return
        val id = ++selectionId
        calibrationJob?.cancel()
        resetConversion()
        _state.update {
            it.copy(
                items = emptyList(),
                loading = true,
                skipped = 0,
                shareCount = if (fromShare) it.shareCount + 1 else it.shareCount,
            )
        }
        viewModelScope.launch {
            val distinct = uris.distinct()
            val items = withContext(Dispatchers.IO) { distinct.mapNotNull(reader::read) }
            if (id != selectionId) return@launch
            _state.update {
                it.copy(items = items, loading = false, skipped = distinct.size - items.size, calibration = null)
            }
            calibrate()
        }
    }

    /** Adds more media to the current selection; already selected files are skipped. */
    fun add(uris: List<Uri>) {
        val known = _state.value.items.map { it.uri }.toSet()
        val fresh = uris.distinct().filter { it !in known }
        if (fresh.isEmpty()) return
        val id = selectionId
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            val items = withContext(Dispatchers.IO) { fresh.mapNotNull(reader::read) }
            if (id != selectionId) return@launch
            _state.update {
                it.copy(
                    items = it.items + items.filter { n -> it.items.none { o -> o.uri == n.uri } },
                    loading = false,
                    skipped = it.skipped + fresh.size - items.size,
                )
            }
            if (_state.value.calibration == null) calibrate()
        }
    }

    /** Test-compresses up to two photos in memory so photo estimates match this batch. */
    private fun calibrate() {
        calibrationJob?.cancel()
        val samples = _state.value.items.filter { it.type == MediaType.IMAGE }.take(2)
        if (samples.isEmpty()) return
        val id = selectionId
        calibrationJob = viewModelScope.launch(Dispatchers.Default) {
            val measured = Preset.entries.associateWith { preset ->
                samples.mapNotNull { runCatching { images.sampleBytesPerPixel(it, preset) }.getOrNull() }
                    .takeIf { it.isNotEmpty() }?.average()?.toFloat()
                    ?: SizeEstimator.DEFAULT_BYTES_PER_PIXEL.getValue(preset)
            }
            val sourceDensity = samples.map { it.sizeBytes.toDouble() / (it.width.toLong() * it.height).coerceAtLeast(1) }
                .average().toFloat()
            val calibration = PhotoCalibration(measured, sourceDensity)
            if (id == selectionId) _state.update { it.copy(calibration = calibration) }
        }
    }

    fun remove(item: MediaItem) {
        _state.update { s -> s.copy(items = s.items - item) }
    }

    fun convert(settings: ConversionSettings) {
        if (_conversion.value.running) return // double tap
        val items = _state.value.items
        resetConversion()
        val id = batchId
        _conversion.value = ConversionState(running = true, total = items.size)
        conversionJob = viewModelScope.launch {
            val results = queue.run(
                items,
                settings,
                onFileProgress = { f ->
                    _conversion.update { if (id != batchId) it else it.copy(current = f) }
                },
            ) { done, result ->
                _conversion.update {
                    if (id != batchId) it else it.copy(done = done, current = 0f, results = it.results + result)
                }
            }
            _conversion.update {
                if (id != batchId) it else it.copy(running = false, finished = true, results = results)
            }
        }
    }

    private fun resetConversion() {
        batchId++
        conversionJob?.cancel()
        conversionJob = null
        _conversion.value = ConversionState()
    }

    fun cancelConversion() {
        resetConversion()
        viewModelScope.launch(Dispatchers.IO) { cache.clearAll() }
    }

    /** Leaving the result: converted copies are no longer needed. */
    fun finish() {
        resetConversion()
        selectionId++
        _state.value = MediaSelectionState(shareCount = _state.value.shareCount)
        viewModelScope.launch(Dispatchers.IO) { cache.clearAll() }
    }
}
