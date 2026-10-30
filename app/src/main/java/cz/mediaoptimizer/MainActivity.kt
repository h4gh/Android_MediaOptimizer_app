package cz.mediaoptimizer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.mediaoptimizer.data.SettingsRepository
import cz.mediaoptimizer.ui.media.MediaViewModel
import cz.mediaoptimizer.ui.navigation.AppNavHost
import cz.mediaoptimizer.ui.theme.MediaOptimizerTheme

class MainActivity : ComponentActivity() {
    private val media: MediaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleShare(intent)
        val settingsRepository = SettingsRepository.get(this)
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle()
            MediaOptimizerTheme(themeMode = settings.themeMode, color = settings.color) {
                AppNavHost(media, settings, onSettingsChange = settingsRepository::update)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    /** Media shared from the gallery ("Share" -> Media Optimizer). */
    private fun handleShare(intent: Intent?) {
        val uris: List<Uri> = when (intent?.action) {
            Intent.ACTION_SEND ->
                listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
            Intent.ACTION_SEND_MULTIPLE ->
                IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
            else -> emptyList()
        }
        media.select(uris, fromShare = true)
    }
}
