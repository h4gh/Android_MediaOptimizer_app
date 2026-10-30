package cz.mediaoptimizer.ui.setup

import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.material3.Slider
import androidx.compose.material3.TextButton
import cz.mediaoptimizer.domain.model.ImageFormat
import cz.mediaoptimizer.domain.model.ImageParams
import cz.mediaoptimizer.domain.model.ItemPlan
import cz.mediaoptimizer.domain.model.VideoParams
import kotlin.math.roundToInt
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.KeyboardType
import cz.mediaoptimizer.domain.estimate.TargetSizePlanner
import cz.mediaoptimizer.ui.media.plannerSource
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
import cz.mediaoptimizer.data.AppSettings
import cz.mediaoptimizer.domain.estimate.SizeEstimator
import cz.mediaoptimizer.domain.model.ConversionSettings
import cz.mediaoptimizer.domain.model.MediaItem
import cz.mediaoptimizer.domain.model.MediaType
import cz.mediaoptimizer.domain.model.Preset
import cz.mediaoptimizer.ui.components.BigPrimaryButton
import cz.mediaoptimizer.ui.components.HeroCard
import cz.mediaoptimizer.ui.media.MediaSelectionState
import cz.mediaoptimizer.ui.media.MediaThumbnail
import cz.mediaoptimizer.ui.theme.HeroNumberStyle
import cz.mediaoptimizer.ui.theme.LocalGoodColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    state: MediaSelectionState,
    defaults: AppSettings,
    onRemove: (MediaItem) -> Unit,
    onAdd: (List<Uri>) -> Unit,
    onTrim: (MediaItem) -> Unit,
    onBack: () -> Unit,
    onOptimize: (ConversionSettings) -> Unit,
) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(),
    ) { uris -> if (uris.isNotEmpty()) onAdd(uris) }
    // 0-2 = S/M/L, 3 = fit into a limit
    var preset by rememberSaveable { mutableIntStateOf(defaults.defaultPreset.ordinal) }
    var limitMb by rememberSaveable { mutableStateOf("10") }
    var moreOptions by rememberSaveable { mutableStateOf(false) }
    // "More options" values; start from the preset and are reset when another preset is picked
    var maxSide by rememberSaveable { mutableIntStateOf(defaults.defaultPreset.imageMaxSide) }
    var quality by rememberSaveable { mutableIntStateOf(defaults.defaultPreset.imageQuality) }
    var format by rememberSaveable { mutableStateOf(ImageFormat.JPEG) }
    var shortSide by rememberSaveable { mutableIntStateOf(defaults.defaultPreset.videoShortSide) }
    var bitrate by rememberSaveable { mutableIntStateOf(defaults.defaultPreset.videoBitrate) }
    var frameRate by rememberSaveable { mutableStateOf<Int?>(null) }
    var stripMetadata by rememberSaveable { mutableStateOf(defaults.stripMetadata) }
    var removeAudio by rememberSaveable { mutableStateOf(false) }
    val presets = listOf(R.string.preset_small, R.string.preset_medium, R.string.preset_large, R.string.preset_limit)
    val limitMode = preset == LIMIT_INDEX
    val targetBytes = if (limitMode) limitMb.toIntOrNull()?.takeIf { it > 0 }?.let { it * MB } else null
    val photos = state.items.count { it.type == MediaType.IMAGE }
    val videos = state.items.size - photos
    val totalBytes = state.items.sumOf { it.sizeBytes }
    val settings = ConversionSettings(
        preset = Preset.entries[if (limitMode) 1 else preset],
        stripMetadata = stripMetadata,
        removeAudio = removeAudio,
        targetBytes = targetBytes,
        custom = if (moreOptions && !limitMode) {
            ItemPlan(ImageParams(maxSide, quality, format), VideoParams(shortSide, bitrate, frameRate))
        } else {
            null
        },
    )
    val plan = remember(state.items, state.calibration, targetBytes, removeAudio) {
        targetBytes?.let { target ->
            TargetSizePlanner.plan(state.items.map { it.plannerSource() }, target, removeAudio, state.calibration)
        }
    }
    val estimates = if (plan != null) {
        state.items.zip(plan.estimates).associate { (item, bytes) -> item.uri to bytes }
    } else {
        state.items.associate { it.uri to SizeEstimator.estimate(it, settings, state.calibration) }
    }
    val estimatedTotal = estimates.values.sum()
    val shownPlan = plan?.items?.firstOrNull() ?: settings.custom ?: settings.preset.plan

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
                        text = stringResource(R.string.setup_estimate_label),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val ready = !state.loading && state.items.isNotEmpty()
                        Text(
                            text = if (ready) "~" + Formatter.formatShortFileSize(context, estimatedTotal) else "…",
                            style = HeroNumberStyle,
                        )
                        Text(
                            text = if (ready) "−${SizeEstimator.savedPercent(totalBytes, estimatedTotal)} %" else "— %",
                            style = HeroNumberStyle,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = when {
                            state.loading -> stringResource(R.string.setup_loading)
                            else -> listOfNotNull(
                                stringResource(R.string.setup_from_size, Formatter.formatShortFileSize(context, totalBytes))
                                    .takeIf { state.items.isNotEmpty() },
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
                            MediaTile(
                                item,
                                estimates[item.uri] ?: 0,
                                onRemove = { onRemove(item) },
                                onClick = { onTrim(item) }.takeIf { item.type == MediaType.VIDEO },
                            )
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

                if (videos > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.setup_trim_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.setup_size_label), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    presets.forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = preset == index,
                            onClick = {
                                preset = index
                                if (index != LIMIT_INDEX) {
                                    val p = Preset.entries[index]
                                    maxSide = p.imageMaxSide
                                    quality = p.imageQuality
                                    format = ImageFormat.JPEG
                                    shortSide = p.videoShortSide
                                    bitrate = p.videoBitrate
                                    frameRate = null
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, presets.size),
                        ) { Text(stringResource(label)) }
                    }
                }

                if (limitMode) {
                    Spacer(Modifier.height(12.dp))
                    LimitPicker(limitMb, onChange = { limitMb = it })
                } else {
                    TextButton(onClick = { moreOptions = !moreOptions }) {
                        Text(stringResource(if (moreOptions) R.string.setup_fewer_options else R.string.setup_more_options))
                    }
                    if (moreOptions) {
                        MoreOptions(
                            showPhotos = photos > 0,
                            showVideos = videos > 0,
                            maxSide = maxSide,
                            onMaxSide = { maxSide = it },
                            quality = quality,
                            onQuality = { quality = it },
                            format = format,
                            onFormat = { format = it },
                            shortSide = shortSide,
                            onShortSide = { shortSide = it },
                            bitrate = bitrate,
                            onBitrate = { bitrate = it },
                            frameRate = frameRate,
                            onFrameRate = { frameRate = it },
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = listOfNotNull(
                        when {
                            photos == 0 && limitMode -> null
                            limitMode -> stringResource(
                                R.string.setup_limit_hint_photo,
                                shownPlan.image.maxSide,
                                shownPlan.image.quality,
                            )
                            else -> stringResource(R.string.setup_preset_hint_photo, shownPlan.image.maxSide)
                        },
                        if (videos > 0) {
                            stringResource(
                                R.string.setup_preset_hint_video,
                                shownPlan.video.shortSide,
                                shownPlan.video.bitrate / 1_000_000f,
                            )
                        } else {
                            null
                        },
                    ).joinToString("\n"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (plan != null && !plan.fits && state.items.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            if (videos > 0) R.string.setup_limit_too_small_video else R.string.setup_limit_too_small,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

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
                    onOptimize(settings)
                },
                enabled = !state.loading && state.items.isNotEmpty() && (!limitMode || targetBytes != null),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

private val PHOTO_SIZES = listOf(640, 800, 1024, 1280, 1600, 1920, 2560, 3200, 4096)
private val VIDEO_SIZES = listOf(360, 480, 720, 1080)
private val FRAME_RATES = listOf<Int?>(null, 30, 24)
private const val MIN_BITRATE = 500_000
private const val MAX_BITRATE = 8_000_000
private const val BITRATE_STEP = 250_000

/** Detailed values under the presets (Part 11); each row shows its current value. */
@Composable
private fun MoreOptions(
    showPhotos: Boolean,
    showVideos: Boolean,
    maxSide: Int,
    onMaxSide: (Int) -> Unit,
    quality: Int,
    onQuality: (Int) -> Unit,
    format: ImageFormat,
    onFormat: (ImageFormat) -> Unit,
    shortSide: Int,
    onShortSide: (Int) -> Unit,
    bitrate: Int,
    onBitrate: (Int) -> Unit,
    frameRate: Int?,
    onFrameRate: (Int?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp),
    ) {
        if (showPhotos) {
            Text(stringResource(R.string.options_photos), style = MaterialTheme.typography.titleSmall)
            OptionLabel(stringResource(R.string.options_max_side), "$maxSide px")
            val sizeIndex = PHOTO_SIZES.indexOfFirst { it >= maxSide }.let { if (it < 0) PHOTO_SIZES.lastIndex else it }
            Slider(
                value = sizeIndex.toFloat(),
                onValueChange = { onMaxSide(PHOTO_SIZES[it.roundToInt()]) },
                valueRange = 0f..PHOTO_SIZES.lastIndex.toFloat(),
                steps = PHOTO_SIZES.size - 2,
            )
            if (format != ImageFormat.PNG) {
                OptionLabel(stringResource(R.string.options_quality), "$quality %")
                Slider(
                    value = quality.toFloat(),
                    onValueChange = { onQuality((it / 5).roundToInt() * 5) },
                    valueRange = 40f..95f,
                    steps = 10,
                )
            }
            OptionLabel(stringResource(R.string.options_format), null)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ImageFormat.entries.forEachIndexed { index, f ->
                    SegmentedButton(
                        selected = format == f,
                        onClick = { onFormat(f) },
                        shape = SegmentedButtonDefaults.itemShape(index, ImageFormat.entries.size),
                    ) { Text(f.label) }
                }
            }
            Text(
                text = stringResource(
                    when (format) {
                        ImageFormat.JPEG -> R.string.options_format_jpg
                        ImageFormat.WEBP -> R.string.options_format_webp
                        ImageFormat.PNG -> R.string.options_format_png
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (showPhotos && showVideos) Spacer(Modifier.height(20.dp))
        if (showVideos) {
            Text(stringResource(R.string.options_videos), style = MaterialTheme.typography.titleSmall)
            OptionLabel(stringResource(R.string.options_resolution), null)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                VIDEO_SIZES.forEachIndexed { index, size ->
                    SegmentedButton(
                        selected = shortSide == size,
                        onClick = { onShortSide(size) },
                        shape = SegmentedButtonDefaults.itemShape(index, VIDEO_SIZES.size),
                    ) { Text("${size}p") }
                }
            }
            OptionLabel(
                stringResource(R.string.options_bitrate),
                stringResource(R.string.options_bitrate_value, bitrate / 1_000_000f),
            )
            Slider(
                value = bitrate.toFloat(),
                onValueChange = { onBitrate((it / BITRATE_STEP).roundToInt() * BITRATE_STEP) },
                valueRange = MIN_BITRATE.toFloat()..MAX_BITRATE.toFloat(),
            )
            OptionLabel(stringResource(R.string.options_frame_rate), null)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                FRAME_RATES.forEachIndexed { index, fps ->
                    SegmentedButton(
                        selected = frameRate == fps,
                        onClick = { onFrameRate(fps) },
                        shape = SegmentedButtonDefaults.itemShape(index, FRAME_RATES.size),
                    ) { Text(if (fps == null) stringResource(R.string.options_frame_rate_original) else "$fps fps") }
                }
            }
        }
    }
}

@Composable
private fun OptionLabel(label: String, value: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        if (value != null) Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

private const val LIMIT_INDEX = 3
private const val MB = 1_000_000L

/** Quick limits for common uses (Gmail allows 25 MB per e-mail, but attachments grow by a third when sent). */
private val LIMIT_CHOICES = listOf(5 to null, 10 to null, 18 to R.string.setup_limit_gmail, 25 to null)

@Composable
private fun LimitPicker(limitMb: String, onChange: (String) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LIMIT_CHOICES.forEach { (mb, label) ->
            FilterChip(
                selected = limitMb == mb.toString(),
                onClick = { onChange(mb.toString()) },
                label = { Text(if (label != null) stringResource(label, mb) else "$mb MB") },
                shape = RoundedCornerShape(50),
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = limitMb,
        onValueChange = { value -> onChange(value.filter { it.isDigit() }.take(4)) },
        label = { Text(stringResource(R.string.setup_limit_custom)) },
        suffix = { Text("MB") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
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
private fun MediaTile(item: MediaItem, estimate: Long, onRemove: () -> Unit, onClick: (() -> Unit)?) {
    val context = LocalContext.current
    Column(Modifier.width(88.dp)) {
        Box {
            MediaThumbnail(
                item,
                Modifier
                    .size(88.dp)
                    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            )
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
            text = "→ ~" + Formatter.formatShortFileSize(context, estimate),
            style = MaterialTheme.typography.labelMedium,
            color = LocalGoodColor.current,
            maxLines = 1,
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
