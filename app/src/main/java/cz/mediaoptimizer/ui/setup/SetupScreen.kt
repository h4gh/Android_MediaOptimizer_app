package cz.mediaoptimizer.ui.setup

import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cz.mediaoptimizer.R
import cz.mediaoptimizer.domain.model.ConversionSettings
import cz.mediaoptimizer.domain.model.MediaItem
import cz.mediaoptimizer.domain.model.MediaType
import cz.mediaoptimizer.domain.model.Preset
import cz.mediaoptimizer.ui.components.BigPrimaryButton
import cz.mediaoptimizer.ui.components.HeroCard
import cz.mediaoptimizer.ui.media.MediaSelectionState
import cz.mediaoptimizer.ui.media.MediaThumbnail
import cz.mediaoptimizer.ui.theme.HeroNumberStyle

// Size estimates ("—") arrive in Part 6.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    state: MediaSelectionState,
    onRemove: (MediaItem) -> Unit,
    onAdd: (List<Uri>) -> Unit,
    onBack: () -> Unit,
    onOptimize: (ConversionSettings) -> Unit,
) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(),
    ) { uris -> if (uris.isNotEmpty()) onAdd(uris) }
    var preset by rememberSaveable { mutableIntStateOf(1) }
    var stripMetadata by rememberSaveable { mutableStateOf(true) }
    var removeAudio by rememberSaveable { mutableStateOf(false) }
    val presets = listOf(R.string.preset_small, R.string.preset_medium, R.string.preset_large)
    val photos = state.items.count { it.type == MediaType.IMAGE }
    val videos = state.items.size - photos
    val totalBytes = state.items.sumOf { it.sizeBytes }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.setup_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                HeroCard {
                    Text(
                        text = stringResource(R.string.setup_original_label),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = if (state.loading) "…" else Formatter.formatShortFileSize(context, totalBytes),
                            style = HeroNumberStyle,
                        )
                        Text("— %", style = HeroNumberStyle, color = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = when {
                            state.loading -> stringResource(R.string.setup_loading)
                            else -> listOfNotNull(
                                pluralStringResource(R.plurals.photos, photos, photos).takeIf { photos > 0 },
                                pluralStringResource(R.plurals.videos, videos, videos).takeIf { videos > 0 },
                            ).joinToString(" · ").ifEmpty { stringResource(R.string.setup_empty) }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                if (state.skipped > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = pluralStringResource(R.plurals.setup_skipped, state.skipped, state.skipped),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(20.dp))
                if (state.loading && state.items.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(110.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.items, key = { it.uri.toString() }) { item ->
                            MediaTile(item, onRemove = { onRemove(item) })
                        }
                        item(key = "add") {
                            AddTile(
                                loading = state.loading,
                                onClick = {
                                    picker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                                    )
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.setup_size_label), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    presets.forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = preset == index,
                            onClick = { preset = index },
                            shape = SegmentedButtonDefaults.itemShape(index, presets.size),
                        ) { Text(stringResource(label)) }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.setup_preset_hint_photo, Preset.entries[preset].imageMaxSide) +
                        if (videos > 0) {
                            "\n" + stringResource(
                                R.string.setup_preset_hint_video,
                                Preset.entries[preset].videoShortSide,
                                Preset.entries[preset].videoBitrate / 1_000_000f,
                            )
                        } else {
                            ""
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = stripMetadata,
                        onClick = { stripMetadata = !stripMetadata },
                        label = { Text(stringResource(R.string.setup_strip_metadata)) },
                        shape = RoundedCornerShape(50),
                    )
                    if (videos > 0) {
                        FilterChip(
                            selected = removeAudio,
                            onClick = { removeAudio = !removeAudio },
                            label = { Text(stringResource(R.string.setup_remove_audio)) },
                            shape = RoundedCornerShape(50),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            BigPrimaryButton(
                text = stringResource(R.string.setup_optimize),
                onClick = {
                    onOptimize(
                        ConversionSettings(
                            preset = Preset.entries[preset],
                            stripMetadata = stripMetadata,
                            removeAudio = removeAudio,
                        ),
                    )
                },
                enabled = !state.loading && state.items.isNotEmpty(),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AddTile(loading: Boolean, onClick: () -> Unit) {
    Column(Modifier.width(88.dp)) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .clickable(enabled = !loading, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(28.dp))
            } else {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.setup_add_more),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.setup_add_more),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun MediaTile(item: MediaItem, onRemove: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.width(88.dp)) {
        Box {
            MediaThumbnail(item, Modifier.size(88.dp))
            FilledIconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White,
                ),
            ) {
                Icon(Icons.Filled.Close, stringResource(R.string.remove), Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = Formatter.formatShortFileSize(context, item.sizeBytes),
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = "${item.width}×${item.height}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
