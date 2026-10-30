package cz.mediaoptimizer.data

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList

/** Per-app language (Android 13+); older Android versions follow the phone language. */
object AppLanguage {
    /** Language tags offered in Settings; "" = phone language. */
    val OPTIONS = listOf("", "en", "cs")

    val supported get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun current(context: Context): String {
        if (!supported) return ""
        val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
        return if (locales.isEmpty) "" else locales[0].language
    }

    /** Android restarts the screen in the new language. */
    fun set(context: Context, tag: String) {
        if (!supported) return
        context.getSystemService(LocaleManager::class.java).applicationLocales =
            if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
    }
}
