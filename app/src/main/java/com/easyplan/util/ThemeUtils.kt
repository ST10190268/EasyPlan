package com.easyplan.util

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate

/**
 * ThemeUtils - Utility object for managing app theme preferences
 *
 * This singleton handles theme selection and persistence across app sessions.
 * Supports three theme modes:
 * - SYSTEM: Follows device system theme (Android 10+)
 * - LIGHT: Always uses light theme
 * - DARK: Always uses dark theme
 *
 * Theme preference is stored in SharedPreferences for persistence.
 *
 * @author EasyPlan Team
 * @version 1.0
 *
 * References:
 * - Dark Theme: https://developer.android.com/guide/topics/ui/look-and-feel/darktheme
 * - AppCompatDelegate: https://developer.android.com/reference/androidx/appcompat/app/AppCompatDelegate
 */
object ThemeUtils {

    private const val TAG = "ThemeUtils"
    private const val PREFS = "easyplan_prefs"
    private const val KEY_THEME = "theme_mode"

    /**
     * Enum representing available theme modes
     *
     * @property delegateMode The corresponding AppCompatDelegate mode constant
     */
    enum class ThemeMode(val delegateMode: Int) {
        SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
        LIGHT(AppCompatDelegate.MODE_NIGHT_NO),
        DARK(AppCompatDelegate.MODE_NIGHT_YES)
    }

    /**
     * Applies the user's saved theme preference
     * Should be called in onCreate() of each activity before setContentView()
     *
     * @param context Application or Activity context
     */
    fun applySavedTheme(context: Context) {
        val mode = getSavedTheme(context)
        Log.d(TAG, "applySavedTheme: Applying theme mode: $mode")
        AppCompatDelegate.setDefaultNightMode(mode.delegateMode)
    }

    /**
     * Saves the user's theme preference and applies it immediately
     *
     * @param context Application or Activity context
     * @param mode The theme mode to save and apply
     */
    fun saveTheme(context: Context, mode: ThemeMode) {
        Log.d(TAG, "saveTheme: Saving theme mode: $mode")

        // Persist theme preference to SharedPreferences
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, mode.name)
            .apply()

        // Apply theme immediately
        AppCompatDelegate.setDefaultNightMode(mode.delegateMode)
        Log.i(TAG, "saveTheme: Theme saved and applied successfully")
    }

    /**
     * Retrieves the user's saved theme preference
     *
     * @param context Application or Activity context
     * @return The saved ThemeMode, or SYSTEM as default
     */
    fun getSavedTheme(context: Context): ThemeMode {
        val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_THEME, ThemeMode.SYSTEM.name)

        val theme = runCatching {
            ThemeMode.valueOf(name!!)
        }.getOrDefault(ThemeMode.SYSTEM)

        Log.d(TAG, "getSavedTheme: Retrieved theme mode: $theme")
        return theme
    }
}
