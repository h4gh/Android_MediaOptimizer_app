package cz.mediaoptimizer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cz.mediaoptimizer.R

@OptIn(ExperimentalTextApi::class)
private fun variableFont(res: Int, weight: FontWeight) = Font(
    resId = res,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

// Display font for headlines and big numbers
val Sora = FontFamily(
    variableFont(R.font.sora, FontWeight.Normal),
    variableFont(R.font.sora, FontWeight.SemiBold),
    variableFont(R.font.sora, FontWeight.Bold),
    variableFont(R.font.sora, FontWeight.ExtraBold),
)

// Body font for everything else
val DmSans = FontFamily(
    variableFont(R.font.dm_sans, FontWeight.Normal),
    variableFont(R.font.dm_sans, FontWeight.Medium),
    variableFont(R.font.dm_sans, FontWeight.SemiBold),
    variableFont(R.font.dm_sans, FontWeight.Bold),
)

private val base = Typography()

val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Sora, fontWeight = FontWeight.ExtraBold),
    displayMedium = base.displayMedium.copy(fontFamily = Sora, fontWeight = FontWeight.ExtraBold),
    displaySmall = base.displaySmall.copy(fontFamily = Sora, fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontFamily = Sora, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = Sora, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = Sora, fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontFamily = Sora, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontFamily = DmSans, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontFamily = DmSans, fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(fontFamily = DmSans),
    bodyMedium = base.bodyMedium.copy(fontFamily = DmSans),
    bodySmall = base.bodySmall.copy(fontFamily = DmSans),
    labelLarge = base.labelLarge.copy(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    labelMedium = base.labelMedium.copy(fontFamily = DmSans, fontWeight = FontWeight.Medium),
    labelSmall = base.labelSmall.copy(fontFamily = DmSans, fontWeight = FontWeight.Medium),
)

// Big hero numbers ("4.2 MB", "−82 %")
val HeroNumberStyle = TextStyle(
    fontFamily = Sora,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 44.sp,
    lineHeight = 48.sp,
)
