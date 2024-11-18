package com.hardik.calendarapp

import android.app.Application
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

        Prefs.Builder()
            .setContext(this)
            .build()
    }
}