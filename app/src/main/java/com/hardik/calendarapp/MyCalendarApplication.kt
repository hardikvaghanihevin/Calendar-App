package com.hardik.calendarapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.utillities.Prefs
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyCalendarApplication: Application() {
    private val TAG = BASE_TAG + MyCalendarApplication::class.java.simpleName
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: ")

        Prefs.Builder().setContext(this).build()
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