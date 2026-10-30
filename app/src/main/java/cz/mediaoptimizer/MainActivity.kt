package cz.mediaoptimizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cz.mediaoptimizer.ui.navigation.AppNavHost
import cz.mediaoptimizer.ui.theme.MediaOptimizerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MediaOptimizerTheme {
                AppNavHost()
            }
        }
    }
}
