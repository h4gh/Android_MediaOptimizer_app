package cz.mediaoptimizer.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import cz.mediaoptimizer.R

/** Launcher icon variants; each is an activity-alias in the manifest. */
enum class AppIcon(val alias: String, val background: Int, val foreground: Int) {
    LIGHT(".LauncherLight", R.drawable.ic_launcher_bg_light, R.drawable.ic_launcher_fg_light),
    NIGHT(".LauncherNight", R.drawable.ic_launcher_bg_night, R.drawable.ic_launcher_fg_night);

    private fun component(context: Context) = ComponentName(context.packageName, "cz.mediaoptimizer$alias")

    companion object {
        /** The launcher state is the source of truth, so nothing is stored separately. */
        fun current(context: Context): AppIcon = entries.firstOrNull { icon ->
            when (context.packageManager.getComponentEnabledSetting(icon.component(context))) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> icon == LIGHT // as in the manifest
                else -> false
            }
        } ?: LIGHT

        /** The launcher may take a few seconds to show the new icon. */
        fun set(context: Context, selected: AppIcon) {
            val pm = context.packageManager
            // Enable the new one first so there is always a launcher entry
            entries.sortedBy { it != selected }.forEach { icon ->
                pm.setComponentEnabledSetting(
                    icon.component(context),
                    if (icon == selected) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }
    }
}
