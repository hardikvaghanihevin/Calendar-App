package com.hardik.calendarapp.utillities
import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    fun setLocale(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

/*    fun setLocale(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.createConfigurationContext(config) // Important for API 25+

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.edit().putString("language", languageCode).apply()
    }

    fun getLocale(context: Context): Locale {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val languageCode = sharedPreferences.getString("language", "en") ?: "en"
        return Locale(languageCode)
    }*/
}
