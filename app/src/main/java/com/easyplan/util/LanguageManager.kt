package com.easyplan.util

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.easyplan.R

/**
 * LanguageManager - Stores and applies the user's locale choice.
 *
 * References:
 * https://developer.android.com/guide/topics/resources/app-languages
 */
object LanguageManager {

    private const val TAG = "LanguageManager"
    private const val PREFS_NAME = "easyplan_language"
    private const val KEY_LANGUAGE = "lang"

    enum class SupportedLanguage(val tag: String, val labelRes: Int) {
        ENGLISH("en", R.string.language_english),
        ZULU("zu", R.string.language_zulu)
    }

    fun getSavedLanguage(context: Context): SupportedLanguage {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedTag = prefs.getString(KEY_LANGUAGE, SupportedLanguage.ENGLISH.tag)
        return SupportedLanguage.values().firstOrNull { it.tag == savedTag }
            ?: SupportedLanguage.ENGLISH
    }

    fun applyStoredLanguage(context: Context) {
        val language = getSavedLanguage(context)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
        Log.d(TAG, "applyStoredLanguage: Applied ${language.tag}")
    }

    fun applyLanguage(context: Context, language: SupportedLanguage) {
        Log.i(TAG, "applyLanguage: Switching to ${language.tag}")
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language.tag).apply()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }

    fun getDisplayName(context: Context, language: SupportedLanguage): String {
        return context.getString(language.labelRes)
    }
}
