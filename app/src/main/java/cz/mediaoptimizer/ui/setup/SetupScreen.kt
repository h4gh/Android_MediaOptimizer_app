package cz.mediaoptimizer.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cz.mediaoptimizer.R
import cz.mediaoptimizer.ui.components.BigPrimaryButton
import cz.mediaoptimizer.ui.components.HeroCard
import cz.mediaoptimizer.ui.theme.HeroNumberStyle
import cz.mediaoptimizer.ui.theme.MediaOptimizerTheme

// Part 1: layout only. Numbers and thumbnails are placeholders until Parts 2–3.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(onBack: () -> Unit, onOptimize: () -> Unit) {
    var preset by rememberSaveable { mutableIntStateOf(1) }
    var stripMetadata by rememberSaveable { mutableStateOf(true) }
    var removeAudio by rememberSaveable { mutableStateOf(false) }
    val presets = listOf(R.string.preset_small, R.string.preset_medium, R.string.preset_large)

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
                        Text("— MB", style = HeroNumberStyle)
                        Text("— %", style = HeroNumberStyle, color = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.setup_files_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(6) {
                        Spacer(
                            Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
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

                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = stripMetadata,
                        onClick = { stripMetadata = !stripMetadata },
                        label = { Text(stringResource(R.string.setup_strip_metadata)) },
                        shape = RoundedCornerShape(50),
                    )
                    FilterChip(
                        selected = removeAudio,
                        onClick = { removeAudio = !removeAudio },
                        label = { Text(stringResource(R.string.setup_remove_audio)) },
                        shape = RoundedCornerShape(50),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            BigPrimaryButton(text = stringResource(R.string.setup_optimize), onClick = onOptimize)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SetupPreview() {
    MediaOptimizerTheme { SetupScreen(onBack = {}, onOptimize = {}) }
}
