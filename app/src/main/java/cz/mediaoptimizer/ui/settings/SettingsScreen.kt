package cz.mediaoptimizer.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cz.mediaoptimizer.BuildConfig
import cz.mediaoptimizer.R
import cz.mediaoptimizer.data.AppColor
import cz.mediaoptimizer.data.AppIcon
import cz.mediaoptimizer.data.AppLanguage
import cz.mediaoptimizer.data.AppSettings
import cz.mediaoptimizer.data.ThemeMode
import cz.mediaoptimizer.domain.model.Preset
import cz.mediaoptimizer.ui.theme.colorSchemeFor
import cz.mediaoptimizer.ui.theme.wallpaperColorsAvailable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Section(stringResource(R.string.settings_appearance))
            Label(stringResource(R.string.settings_theme))
            Segmented(
                labels = listOf(
                    R.string.settings_theme_system,
                    R.string.settings_theme_light,
                    R.string.settings_theme_dark,
                    R.string.settings_theme_amoled,
                )
                    .map { stringResource(it) },
                selected = settings.themeMode.ordinal,
                onSelect = { i -> onChange { it.copy(themeMode = ThemeMode.entries[i]) } },
            )
            Label(stringResource(R.string.settings_color))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AppColor.entries
                    .filter { it != AppColor.WALLPAPER || wallpaperColorsAvailable }
                    .forEach { color ->
                        ColorSwatch(
                            color = color,
                            selected = settings.color == color,
                            onClick = { onChange { it.copy(color = color) } },
                        )
                    }
            }

            Label(stringResource(R.string.settings_icon))
            var icon by remember { mutableStateOf(AppIcon.current(context)) }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                AppIcon.entries.forEach { option ->
                    IconChoice(
                        icon = option,
                        selected = icon == option,
                        onClick = {
                            icon = option
                            AppIcon.set(context, option)
                        },
                    )
                }
            }
            Text(
                stringResource(R.string.settings_icon_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            if (AppLanguage.supported) {
                Section(stringResource(R.string.settings_language))
                var language by remember { mutableStateOf(AppLanguage.current(context)) }
                Segmented(
                    labels = listOf(stringResource(R.string.settings_language_system), "English", "Čeština"),
                    selected = AppLanguage.OPTIONS.indexOf(language).coerceAtLeast(0),
                    onSelect = { i ->
                        language = AppLanguage.OPTIONS[i]
                        AppLanguage.set(context, language)
                    },
                )
            }

            Section(stringResource(R.string.settings_defaults))
            Label(stringResource(R.string.settings_default_size))
            Segmented(
                labels = listOf(R.string.preset_small, R.string.preset_medium, R.string.preset_large).map { stringResource(it) },
                selected = settings.defaultPreset.ordinal,
                onSelect = { i -> onChange { it.copy(defaultPreset = Preset.entries[i]) } },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clickable { onChange { it.copy(stripMetadata = !it.stripMetadata) } },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.setup_strip_metadata), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.settings_strip_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = settings.stripMetadata,
                    onCheckedChange = { checked -> onChange { it.copy(stripMetadata = checked) } },
                )
            }

            Spacer(Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
    )
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
}

@Composable
private fun Segmented(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            SegmentedButton(
                selected = selected == index,
                onClick = { onSelect(index) },
                shape = SegmentedButtonDefaults.itemShape(index, labels.size),
            ) { Text(label, maxLines = 1) }
        }
    }
}

@Composable
private fun IconChoice(icon: AppIcon, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .border(
                    3.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                    CircleShape,
                )
                .padding(5.dp)
                .clip(CircleShape),
        ) {
            // Launcher layers are 108 dp with the visible part in the middle 72 dp
            Image(painterResource(icon.background), null, Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
            Image(
                painterResource(icon.foreground),
                null,
                Modifier.matchParentSize().scale(1.5f),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(if (icon == AppIcon.LIGHT) R.string.icon_light else R.string.icon_night),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun ColorSwatch(color: AppColor, selected: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val scheme = colorSchemeFor(color, dark, context)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(scheme.primary)
                .border(3.dp, if (selected) MaterialTheme.colorScheme.onSurface else scheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = scheme.onPrimary)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(
                when (color) {
                    AppColor.TEAL -> R.string.color_teal
                    AppColor.TANGERINE -> R.string.color_tangerine
                    AppColor.COBALT -> R.string.color_cobalt
                    AppColor.WALLPAPER -> R.string.color_wallpaper
                },
            ),
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
        )
    }
}
