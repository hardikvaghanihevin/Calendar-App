package com.hardik.calendarapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.database.CursorWindow
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.utillities.LocaleHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class MyCalendarApplication: Application() {
    private val TAG = BASE_TAG + MyCalendarApplication::class.java.simpleName
    override fun onCreate() {
        super.onCreate()

        // Increase cursor window size to prevent crashes during large queries
        increaseCursorWindowSize()

        // Launch background operations
        CoroutineScope(Dispatchers.Default).launch {
            loadPreferencesAndInitializeApp()
        }
    }

    /** Load preferences, apply the theme, and configure locale */
    private suspend fun loadPreferencesAndInitializeApp() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val languageCode = sharedPreferences.getString("language", "en") ?: "en"
        val appTheme = sharedPreferences.getString("app_theme", "system") ?: "system"

        applyTheme(appTheme)
        // Update locale
        LocaleHelper.setLocale(this, languageCode)
        // Create notification channel if necessary
        createNotificationChannel()
    }

    /** Apply the selected theme for the app */
    private fun applyTheme(appTheme: String) {
        val nightMode = when (appTheme) {
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    /** Create notification channel for event notifications */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "event_channel_id",
                "Event Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Channel for Event Notifications"
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    /** Increase cursor window size to 20MB to handle large query results */
    private fun increaseCursorWindowSize() {
        try {
            val field = CursorWindow::class.java.getDeclaredField("sCursorWindowSize")
            field.isAccessible = true
            field.set(null, 1024 * 1024 * 20) // Increase size to 20MB
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}