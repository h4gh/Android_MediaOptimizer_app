package cz.mediaoptimizer.ui.result

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cz.mediaoptimizer.R
import cz.mediaoptimizer.ui.components.BigPrimaryButton
import cz.mediaoptimizer.ui.components.HeroCard
import cz.mediaoptimizer.ui.theme.HeroNumberStyle
import cz.mediaoptimizer.ui.theme.MediaOptimizerTheme

// Part 1: layout only. Real before/after sizes and sharing arrive in Part 4.
@Composable
fun ResultScreen(onDone: () -> Unit) {
    BackHandler(onBack = onDone)
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
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                HeroCard {
                    Text(
                        text = stringResource(R.string.result_saved_label),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text("— %", style = HeroNumberStyle, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(stringResource(R.string.result_before), style = MaterialTheme.typography.labelMedium)
                            Text("— MB", style = MaterialTheme.typography.headlineSmall)
                        }
                        Column {
                            Text(stringResource(R.string.result_after), style = MaterialTheme.typography.labelMedium)
                            Text("— MB", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }
            BigPrimaryButton(text = stringResource(R.string.result_share), onClick = {}, enabled = false)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.result_done))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultPreview() {
    MediaOptimizerTheme { ResultScreen(onDone = {}) }
}
