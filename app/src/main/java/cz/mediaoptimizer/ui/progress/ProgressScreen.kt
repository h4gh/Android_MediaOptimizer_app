package cz.mediaoptimizer.ui.progress

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cz.mediaoptimizer.R
import cz.mediaoptimizer.ui.components.HeroCard
import cz.mediaoptimizer.ui.media.ConversionState
import cz.mediaoptimizer.ui.theme.HeroNumberStyle

@Composable
fun ProgressScreen(state: ConversionState, onCancel: () -> Unit) {
    BackHandler(onBack = onCancel)
    val fraction = if (state.total == 0) 0f else state.done.toFloat() / state.total
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                HeroCard {
                    Text(stringResource(R.string.progress_title), style = MaterialTheme.typography.labelMedium)
                    Text("${(fraction * 100).toInt()} %", style = HeroNumberStyle)
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.progress_count, state.done, state.total),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}
