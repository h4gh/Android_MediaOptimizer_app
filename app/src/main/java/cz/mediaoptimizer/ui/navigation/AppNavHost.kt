package cz.mediaoptimizer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cz.mediaoptimizer.ui.home.HomeScreen
import cz.mediaoptimizer.ui.result.ResultScreen
import cz.mediaoptimizer.ui.setup.SetupScreen

object Routes {
    const val HOME = "home"
    const val SETUP = "setup"
    const val RESULT = "result"
}

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(onPickMedia = { nav.navigate(Routes.SETUP) })
        }
        composable(Routes.SETUP) {
            SetupScreen(
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
