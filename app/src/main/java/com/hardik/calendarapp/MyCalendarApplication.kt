package com.hardik.calendarapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.utillities.LocaleHelper
import com.hardik.calendarapp.utillities.Prefs
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyCalendarApplication: Application() {
    private val TAG = BASE_TAG + MyCalendarApplication::class.java.simpleName
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: ")
        // Step 1: Retrieve saved language and theme preferences
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val languageCode = sharedPreferences.getString("language", "en") ?: "en"
        val countryCode = sharedPreferences.getStringSet("countries", setOf("indian")) ?: setOf("indian")
        val appTheme = sharedPreferences.getString("app_theme", "system") ?: "system"

        // Step 2: Set the theme
        when (appTheme) {
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        // Step 3: Update locale
        LocaleHelper.setLocale(this, languageCode)

        // Step 4: Initialize shared preferences
        Prefs.Builder().setContext(this).build()

        // Step 5: Create notification channel
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