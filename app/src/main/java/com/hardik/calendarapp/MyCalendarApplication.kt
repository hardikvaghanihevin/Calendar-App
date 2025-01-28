package com.hardik.calendarapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
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

        // Launch background operations
        CoroutineScope(Dispatchers.Default).launch {
            loadPreferencesAndInitializeApp()
        }
    }

    private suspend fun loadPreferencesAndInitializeApp() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val languageCode = sharedPreferences.getString("language", "en") ?: "en"
        val appTheme = sharedPreferences.getString("app_theme", "system") ?: "system"

        // Set the theme based on preference
        when (appTheme) {
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        // Update locale
        LocaleHelper.setLocale(this, languageCode)

        // Create notification channel if necessary
        createNotificationChannel()
    }

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
}