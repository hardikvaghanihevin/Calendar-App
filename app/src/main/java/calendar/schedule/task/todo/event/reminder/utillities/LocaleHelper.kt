package calendar.schedule.task.todo.event.reminder.utillities
import android.content.Context
import android.content.res.Configuration
import androidx.preference.PreferenceManager
import calendar.schedule.task.todo.event.reminder.common.Constants.PREF_KEY_LANGUAGE
import java.util.Locale

object LocaleHelper {
    fun setLocale(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    fun getLocale(context: Context): Locale {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val languageCode = sharedPreferences.getString(PREF_KEY_LANGUAGE, "en") ?: "en"
        return Locale(languageCode)
    }
/*    fun setLocale(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.createConfigurationContext(config) // Important for API 25+

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.edit().putString(PREF_KEY_LANGUAGE, languageCode).apply()
    }

    fun getLocale(context: Context): Locale {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val languageCode = sharedPreferences.getString(PREF_KEY_LANGUAGE, "en") ?: "en"
        return Locale(languageCode)
    }*/
}
