package cz.mediaoptimizer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cz.mediaoptimizer.ui.home.HomeScreen
import cz.mediaoptimizer.ui.media.MediaViewModel
import cz.mediaoptimizer.ui.result.ResultScreen
import cz.mediaoptimizer.ui.setup.SetupScreen

object Routes {
    const val HOME = "home"
    const val SETUP = "setup"
    const val RESULT = "result"
}

@Composable
fun AppNavHost(media: MediaViewModel) {
    val nav = rememberNavController()
    val state by media.state.collectAsStateWithLifecycle()

    // Media arrived from the share sheet: jump straight to Setup.
    LaunchedEffect(state.shareCount) {
        if (state.shareCount > 0) {
            nav.navigate(Routes.SETUP) {
                popUpTo(Routes.HOME)
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onMediaPicked = { uris ->
                    media.select(uris)
                    nav.navigate(Routes.SETUP)
                },
            )
        }
        composable(Routes.SETUP) {
            SetupScreen(
                state = state,
                onRemove = media::remove,
                onBack = { nav.popBackStack() },
                onOptimize = { nav.navigate(Routes.RESULT) },
            )
        }
        composable(Routes.RESULT) {
            ResultScreen(
                onDone = { nav.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
    }
}
