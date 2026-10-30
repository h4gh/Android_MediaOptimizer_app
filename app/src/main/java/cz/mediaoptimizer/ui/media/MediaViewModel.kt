package cz.mediaoptimizer.ui.media

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cz.mediaoptimizer.data.MediaInfoReader
import cz.mediaoptimizer.domain.model.MediaItem
import kotlinx.coroutines.Dispatchers
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

/** Holds the selected media; shared by all screens of the activity. */
class MediaViewModel(app: Application) : AndroidViewModel(app) {
    private val reader = MediaInfoReader(app)
    private val _state = MutableStateFlow(MediaSelectionState())
    val state: StateFlow<MediaSelectionState> = _state.asStateFlow()

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
}
