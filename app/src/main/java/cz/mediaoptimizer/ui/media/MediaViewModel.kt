package cz.mediaoptimizer.ui.media

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cz.mediaoptimizer.data.CacheManager
import cz.mediaoptimizer.data.MediaInfoReader
import cz.mediaoptimizer.domain.model.ConversionResult
import cz.mediaoptimizer.domain.model.ConversionSettings
import cz.mediaoptimizer.domain.model.MediaItem
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
)

data class ConversionState(
    val running: Boolean = false,
    val finished: Boolean = false,
    val done: Int = 0,
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
    private var conversionJob: Job? = null

    private val _state = MutableStateFlow(MediaSelectionState())
    val state: StateFlow<MediaSelectionState> = _state.asStateFlow()

    private val _conversion = MutableStateFlow(ConversionState())
    val conversion: StateFlow<ConversionState> = _conversion.asStateFlow()

    fun select(uris: List<Uri>, fromShare: Boolean = false) {
        if (uris.isEmpty()) return
        _state.update {
            it.copy(
                items = emptyList(),
                loading = true,
                skipped = 0,
                shareCount = if (fromShare) it.shareCount + 1 else it.shareCount,
            )
        }
        viewModelScope.launch {
            val items = withContext(Dispatchers.IO) { uris.distinct().mapNotNull(reader::read) }
            _state.update {
                it.copy(items = items, loading = false, skipped = uris.distinct().size - items.size)
            }
        }
    }

    fun remove(item: MediaItem) {
        _state.update { s -> s.copy(items = s.items - item) }
    }

    fun convert(settings: ConversionSettings) {
        val items = _state.value.items
        conversionJob?.cancel()
        _conversion.value = ConversionState(running = true, total = items.size)
        conversionJob = viewModelScope.launch {
            val results = queue.run(items, settings) { done, result ->
                _conversion.update { it.copy(done = done, results = it.results + result) }
            }
            _conversion.update { it.copy(running = false, finished = true, results = results) }
        }
    }

    fun cancelConversion() {
        conversionJob?.cancel()
        _conversion.value = ConversionState()
        viewModelScope.launch(Dispatchers.IO) { cache.clearAll() }
    }

    /** Leaving the result: converted copies are no longer needed. */
    fun finish() {
        _conversion.value = ConversionState()
        _state.value = MediaSelectionState(shareCount = _state.value.shareCount)
        viewModelScope.launch(Dispatchers.IO) { cache.clearAll() }
    }
}
