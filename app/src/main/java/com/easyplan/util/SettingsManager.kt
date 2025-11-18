package com.easyplan.util

import android.content.Context
import android.util.Log

/**
 * Stores lightweight boolean preferences for the Settings screen.
 */
object SettingsManager {

    private const val TAG = "SettingsManager"
    private const val PREFS = "easyplan_settings"
    private const val KEY_NOTIFICATIONS = "notifications_enabled"

    fun isNotificationsEnabled(context: Context): Boolean {
        val enabled = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTIFICATIONS, false)
        Log.v(TAG, "isNotificationsEnabled: $enabled")
        return enabled
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTIFICATIONS, enabled)
            .apply()
        Log.i(TAG, "setNotificationsEnabled: $enabled")
    }
}
