package cz.mediaoptimizer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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
    secondary = TealLightPrimary,
    background = TealDarkBackground,
    onBackground = TealDarkText,
    surface = TealDarkSurface,
    onSurface = TealDarkText,
    surfaceVariant = TealDarkSurfaceVariant,
    onSurfaceVariant = TealDarkMuted,
    outline = TealDarkOutline,
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(18.dp),
)

/** Colour for "you saved X" figures; not part of the Material scheme. */
val LocalGoodColor = staticCompositionLocalOf { TealLightGood }

@Composable
fun MediaOptimizerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val good: Color = if (darkTheme) TealDarkGood else TealLightGood
    CompositionLocalProvider(LocalGoodColor provides good) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
