package cz.mediaoptimizer.ui.result

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cz.mediaoptimizer.R
import cz.mediaoptimizer.data.ShareHelper
import cz.mediaoptimizer.domain.model.ConversionResult
import cz.mediaoptimizer.ui.components.BigPrimaryButton
import cz.mediaoptimizer.ui.components.HeroCard
import cz.mediaoptimizer.ui.media.ConversionState
import cz.mediaoptimizer.ui.media.MediaThumbnail
import cz.mediaoptimizer.ui.theme.HeroNumberStyle
import cz.mediaoptimizer.ui.theme.LocalGoodColor

@Composable
fun ResultScreen(state: ConversionState, onDone: () -> Unit) {
    BackHandler(onBack = onDone)
    val context = LocalContext.current
    val saved = if (state.beforeBytes > 0) 100 - (state.afterBytes * 100 / state.beforeBytes) else 0
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.result_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(16.dp))
            HeroCard {
                Text(stringResource(R.string.result_saved_label), style = MaterialTheme.typography.labelMedium)
                Text("−$saved %", style = HeroNumberStyle, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(stringResource(R.string.result_before), style = MaterialTheme.typography.labelMedium)
                        Text(
                            Formatter.formatShortFileSize(context, state.beforeBytes),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.result_after), style = MaterialTheme.typography.labelMedium)
                        Text(
                            Formatter.formatShortFileSize(context, state.afterBytes),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.results, key = { it.source.uri.toString() }) { ResultRow(it) }
            }
            Spacer(Modifier.height(12.dp))
            val chooserTitle = stringResource(R.string.result_share)
            BigPrimaryButton(
                text = chooserTitle,
                onClick = {
                    ShareHelper.share(context, state.converted.mapNotNull { it.outputFile }, chooserTitle)
                },
                enabled = state.converted.isNotEmpty(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.result_done))
            }
        }
    }
}

@Composable
private fun ResultRow(result: ConversionResult) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        MediaThumbnail(result.source, Modifier.size(52.dp))
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                result.source.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            when {
                result.skipped -> Text(
                    stringResource(R.string.result_video_later),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                result.error != null -> Text(
                    stringResource(R.string.result_error, result.error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                else -> Text(
                    Formatter.formatShortFileSize(context, result.source.sizeBytes) + "  →  " +
                        Formatter.formatShortFileSize(context, result.outputBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalGoodColor.current,
                )
            }
        }
    }
}
