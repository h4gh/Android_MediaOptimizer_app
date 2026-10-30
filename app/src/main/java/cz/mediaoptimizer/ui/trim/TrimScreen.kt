package cz.mediaoptimizer.ui.trim

import androidx.annotation.OptIn
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.PlayerView
import cz.mediaoptimizer.R
import cz.mediaoptimizer.domain.model.MediaItem
import cz.mediaoptimizer.domain.model.TrimRange
import cz.mediaoptimizer.ui.components.BigPrimaryButton
import cz.mediaoptimizer.ui.media.formatDuration
import kotlinx.coroutines.delay
import kotlin.math.min

/** Shortest part of a video that can be kept. */
private const val MIN_LENGTH_MS = 1_000L

/** Two bars in the button's content colour (the ⏸ emoji is drawn orange on some phones). */
@Composable
private fun PauseIcon() {
    val color = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val bar = size.width / 5
        drawRect(color, topLeft = Offset(bar, size.height * 0.2f), size = Size(bar, size.height * 0.6f))
        drawRect(color, topLeft = Offset(bar * 3, size.height * 0.2f), size = Size(bar, size.height * 0.6f))
    }
}

/** Video preview with a start/end slider; the original file is never changed. */
@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrimScreen(item: MediaItem, onDone: (TrimRange?) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val duration = (item.durationMs ?: 0).coerceAtLeast(1)
    val fullRange = 0f..duration.toFloat()
    var range by remember {
        mutableStateOf(item.trim?.let { it.startMs.toFloat()..it.endMs.toFloat() } ?: fullRange)
    }
    var playing by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(range.start.toLong()) }
    // Which mark was dragged last, so the preview shows that frame
    var draggedStart by remember { mutableStateOf(true) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(item.uri))
            prepare()
            seekTo(range.start.toLong())
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    // While previewing, stop at the end mark and jump back to the start
    LaunchedEffect(playing) {
        while (playing) {
            position = player.currentPosition
            if (position >= range.endInclusive) {
                player.pause()
                player.seekTo(range.start.toLong())
                position = range.start.toLong()
            }
            delay(30)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trim_title)) },
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
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        useController = false
                        this.player = player
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(MaterialTheme.shapes.large)
                    .background(Color.Black),
            )

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = formatDuration(range.start.toLong()) + " – " + formatDuration(range.endInclusive.toLong()),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(
                            R.string.trim_length,
                            formatDuration((range.endInclusive - range.start).toLong()),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalButton(
                    onClick = {
                        if (playing) {
                            player.pause()
                        } else {
                            if (player.currentPosition !in range.start.toLong()..<range.endInclusive.toLong() - 100) {
                                player.seekTo(range.start.toLong())
                            }
                            player.play()
                        }
                    },
                ) {
                    if (playing) PauseIcon() else Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(if (playing) R.string.trim_pause else R.string.trim_play))
                }
            }

            Spacer(Modifier.height(8.dp))
            RangeSlider(
                value = range,
                onValueChange = { new ->
                    if (new.endInclusive - new.start < min(MIN_LENGTH_MS, duration)) return@RangeSlider
                    draggedStart = new.start != range.start
                    range = new
                    player.pause()
                    // Fast, approximate frames while dragging
                    player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                    position = (if (draggedStart) new.start else new.endInclusive).toLong()
                    player.seekTo(position)
                },
                onValueChangeFinished = {
                    player.setSeekParameters(SeekParameters.EXACT)
                    player.seekTo((if (draggedStart) range.start else range.endInclusive).toLong())
                },
                valueRange = fullRange,
                track = { sliderState ->
                    // Playhead drawn over the track so it lines up with the marks exactly
                    val playheadColor = MaterialTheme.colorScheme.primary // same as the selected part of the track
                    val outlineColor = MaterialTheme.colorScheme.background
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        SliderDefaults.Track(rangeSliderState = sliderState)
                        // Same height as the track; a thin outline keeps it visible on both track colours
                        Canvas(Modifier.matchParentSize()) {
                            val x = size.width * (position.toFloat() / duration).coerceIn(0f, 1f)
                            drawLine(outlineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 7.dp.toPx())
                            drawLine(playheadColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 4.dp.toPx())
                        }
                    }
                },
            )

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = {
                        range = fullRange
                        position = 0
                        player.seekTo(0)
                    },
                    modifier = Modifier.height(60.dp),
                ) {
                    Text(stringResource(R.string.trim_reset))
                }
                BigPrimaryButton(
                    text = stringResource(R.string.trim_done),
                    onClick = {
                        val trim = TrimRange(range.start.toLong(), range.endInclusive.toLong())
                        onDone(trim.takeIf { range != fullRange })
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
