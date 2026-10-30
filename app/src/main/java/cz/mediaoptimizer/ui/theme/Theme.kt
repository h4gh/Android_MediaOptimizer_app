package cz.mediaoptimizer.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cz.mediaoptimizer.data.AppColor
import cz.mediaoptimizer.data.ThemeMode

private val TealLight = lightColorScheme(
    primary = TealLightPrimary,
    onPrimary = TealLightOnPrimary,
    primaryContainer = TealLightContainer,
    onPrimaryContainer = TealLightOnContainer,
    secondary = TealLightAccent,
    secondaryContainer = TealLightSelected,
    onSecondaryContainer = TealLightOnSelected,
    background = TealLightBackground,
    onBackground = TealLightText,
    surface = TealLightSurface,
    onSurface = TealLightText,
    surfaceVariant = TealLightSurfaceVariant,
    onSurfaceVariant = TealLightMuted,
    outline = TealLightOutline,
)

private val TealDark = darkColorScheme(
    primary = TealDarkPrimary,
    onPrimary = TealDarkOnPrimary,
    primaryContainer = TealDarkContainer,
    onPrimaryContainer = TealDarkOnContainer,
    secondary = TealLightPrimary,
    secondaryContainer = TealDarkSelected,
    onSecondaryContainer = TealDarkOnSelected,
    background = TealDarkBackground,
    onBackground = TealDarkText,
    surface = TealDarkSurface,
    onSurface = TealDarkText,
    surfaceVariant = TealDarkSurfaceVariant,
    onSurfaceVariant = TealDarkMuted,
    outline = TealDarkOutline,
)

private val TangerineLight = lightColorScheme(
    primary = TangerineLightPrimary,
    onPrimary = Color.White,
    primaryContainer = TangerineLightContainer,
    onPrimaryContainer = Color.White,
    secondary = TangerineLightAccent,
    secondaryContainer = TangerineLightSelected,
    onSecondaryContainer = TangerineLightOnSelected,
    background = TangerineLightBackground,
    onBackground = TangerineLightText,
    surface = Color.White,
    onSurface = TangerineLightText,
    surfaceVariant = TangerineLightSurfaceVariant,
    onSurfaceVariant = TangerineLightMuted,
    outline = TangerineLightOutline,
)

private val TangerineDark = darkColorScheme(
    primary = TangerineDarkPrimary,
    onPrimary = TangerineDarkOnPrimary,
    primaryContainer = TangerineDarkPrimary,
    onPrimaryContainer = TangerineDarkOnContainer,
    secondary = TangerineDarkAccent,
    secondaryContainer = TangerineDarkSelected,
    onSecondaryContainer = TangerineLightSelected,
    background = TangerineDarkBackground,
    onBackground = TangerineDarkText,
    surface = TangerineDarkSurface,
    onSurface = TangerineDarkText,
    surfaceVariant = TangerineDarkSurfaceVariant,
    onSurfaceVariant = TangerineDarkMuted,
    outline = TangerineDarkOutline,
)

private val CobaltLight = lightColorScheme(
    primary = CobaltLightPrimary,
    onPrimary = Color.White,
    primaryContainer = CobaltLightContainer,
    onPrimaryContainer = Color.White,
    secondary = CobaltLightAccent,
    secondaryContainer = CobaltLightSelected,
    onSecondaryContainer = CobaltLightOnSelected,
    background = CobaltLightBackground,
    onBackground = CobaltLightText,
    surface = Color.White,
    onSurface = CobaltLightText,
    surfaceVariant = CobaltLightSurfaceVariant,
    onSurfaceVariant = CobaltLightMuted,
    outline = CobaltLightOutline,
)

private val CobaltDark = darkColorScheme(
    primary = CobaltDarkPrimary,
    onPrimary = CobaltDarkOnPrimary,
    primaryContainer = CobaltDarkPrimary,
    onPrimaryContainer = CobaltDarkOnContainer,
    secondary = CobaltLightPrimary,
    secondaryContainer = CobaltDarkSelected,
    onSecondaryContainer = CobaltLightSelected,
    background = CobaltDarkBackground,
    onBackground = CobaltDarkText,
    surface = CobaltDarkSurface,
    onSurface = CobaltDarkText,
    surfaceVariant = CobaltDarkSurfaceVariant,
    onSurfaceVariant = CobaltDarkMuted,
    outline = CobaltDarkOutline,
)

/** Colours from the wallpaper need Android 12. */
val wallpaperColorsAvailable get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

fun colorSchemeFor(color: AppColor, dark: Boolean, context: Context): ColorScheme = when (color) {
    AppColor.TEAL -> if (dark) TealDark else TealLight
    AppColor.TANGERINE -> if (dark) TangerineDark else TangerineLight
    AppColor.COBALT -> if (dark) CobaltDark else CobaltLight
    AppColor.WALLPAPER -> when {
        !wallpaperColorsAvailable -> if (dark) TealDark else TealLight
        // Hero card: strong container with readable text, like the fixed palettes
        dark -> dynamicDarkColorScheme(context).let { it.copy(primaryContainer = it.primary, onPrimaryContainer = it.onPrimary) }
        else -> dynamicLightColorScheme(context).let {
            it.copy(primaryContainer = it.onPrimaryContainer, onPrimaryContainer = it.primaryContainer)
        }
    }
}

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(18.dp),
)

/** Colour for "you saved X" figures; not part of the Material scheme. */
val LocalGoodColor = staticCompositionLocalOf { TealLightGood }

@Composable
fun MediaOptimizerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    color: AppColor = AppColor.TEAL,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }
    val scheme = colorSchemeFor(color, darkTheme, LocalContext.current).let {
        if (themeMode == ThemeMode.AMOLED) {
            it.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceVariant = AmoledSurfaceVariant,
                surfaceContainer = AmoledSurfaceContainer,
                surfaceContainerLow = Color.Black,
                surfaceContainerHigh = AmoledSurfaceContainer,
            )
        } else {
            it
        }
    }
    val good: Color = if (darkTheme) TealDarkGood else TealLightGood
    CompositionLocalProvider(LocalGoodColor provides good) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
