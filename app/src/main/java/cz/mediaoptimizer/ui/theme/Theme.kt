package cz.mediaoptimizer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = TealLightPrimary,
    onPrimary = TealLightOnPrimary,
    primaryContainer = TealLightContainer,
    onPrimaryContainer = TealLightOnContainer,
    secondary = TealLightAccent,
    background = TealLightBackground,
    onBackground = TealLightText,
    surface = TealLightSurface,
    onSurface = TealLightText,
    surfaceVariant = TealLightSurfaceVariant,
    onSurfaceVariant = TealLightMuted,
    outline = TealLightOutline,
)

private val DarkColors = darkColorScheme(
    primary = TealDarkPrimary,
    onPrimary = TealDarkOnPrimary,
    primaryContainer = TealDarkContainer,
    onPrimaryContainer = TealDarkOnContainer,
    background = TealDarkBackground,
    onBackground = TealDarkText,
    surface = TealDarkSurface,
    onSurface = TealDarkText,
    surfaceVariant = TealDarkSurfaceVariant,
    onSurfaceVariant = TealDarkMuted,
    outline = TealDarkOutline,
)

@Composable
fun MediaOptimizerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
