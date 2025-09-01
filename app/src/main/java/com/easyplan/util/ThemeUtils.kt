package com.easyplan.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeUtils {
    private const val PREFS = "easyplan_prefs"
    private const val KEY_THEME = "theme_mode"

    enum class ThemeMode(val delegateMode: Int) {
        SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
        LIGHT(AppCompatDelegate.MODE_NIGHT_NO),
        DARK(AppCompatDelegate.MODE_NIGHT_YES)
    }

    fun applySavedTheme(context: Context) {
        val mode = getSavedTheme(context)
        AppCompatDelegate.setDefaultNightMode(mode.delegateMode)
    }

    fun saveTheme(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, mode.name)
            .apply()
        AppCompatDelegate.setDefaultNightMode(mode.delegateMode)
    }

    fun getSavedTheme(context: Context): ThemeMode {
        val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_THEME, ThemeMode.SYSTEM.name)
        return runCatching { ThemeMode.valueOf(name!!) }.getOrDefault(ThemeMode.SYSTEM)
    }
}
