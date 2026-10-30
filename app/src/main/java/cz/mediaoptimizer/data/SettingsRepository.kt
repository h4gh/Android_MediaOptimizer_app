package cz.mediaoptimizer.data

import android.content.Context
import cz.mediaoptimizer.domain.model.Preset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AppColor { TEAL, TANGERINE, COBALT, WALLPAPER }

/** User preferences; the app language is kept by Android itself (per-app language). */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val color: AppColor = AppColor.TEAL,
    val defaultPreset: Preset = Preset.MEDIUM,
    val stripMetadata: Boolean = true,
)

/** Small key-value store; one instance for the whole app. */
class SettingsRepository private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        val new = transform(_settings.value)
        prefs.edit()
            .putString(KEY_THEME, new.themeMode.name)
            .putString(KEY_COLOR, new.color.name)
            .putString(KEY_PRESET, new.defaultPreset.name)
            .putBoolean(KEY_STRIP, new.stripMetadata)
            .apply()
        _settings.value = new
    }

    private fun read() = AppSettings(
        themeMode = enumOr(prefs.getString(KEY_THEME, null), ThemeMode.SYSTEM),
        color = enumOr(prefs.getString(KEY_COLOR, null), AppColor.TEAL),
        defaultPreset = enumOr(prefs.getString(KEY_PRESET, null), Preset.MEDIUM),
        stripMetadata = prefs.getBoolean(KEY_STRIP, true),
    )

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    companion object {
        private const val KEY_THEME = "theme"
        private const val KEY_COLOR = "color"
        private const val KEY_PRESET = "default_preset"
        private const val KEY_STRIP = "strip_metadata"

        @Volatile private var instance: SettingsRepository? = null

        fun get(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
    }
}
