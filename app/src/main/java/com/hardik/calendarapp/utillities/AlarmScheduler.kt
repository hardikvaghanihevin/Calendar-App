package com.hardik.calendarapp.utillities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.data.database.entity.AlertOffset
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.presentation.receiver.NotificationReceiver
import com.hardik.calendarapp.presentation.ui.MainActivity.Companion.REQUEST_CODE_CALENDAR_PERMISSIONS

object AlarmScheduler {
    private val TAG = BASE_TAG + AlarmScheduler::class.simpleName

    // Method to check and request POST_NOTIFICATIONS permission
    private fun ensureNotificationPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            )

            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                // Request the permission from the user & Ensure context is an instance of Activity
                if (context is Activity) {
                    ActivityCompat.requestPermissions(
                        context,
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                        REQUEST_CODE_CALENDAR_PERMISSIONS
                    )
                } else {
                    Log.e("PermissionError", "Context is not an Activity. Cannot request permissions.")
                    // Handle this error gracefully, e.g., show a message or fallback logic.
                }
            }
        }
    }

    // Handle the result of the permission request
    fun handlePermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
        if (requestCode == REQUEST_CODE_CALENDAR_PERMISSIONS) {
            return if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "POST_NOTIFICATIONS permission granted.")
                true
            } else {
                Log.e(TAG, "POST_NOTIFICATIONS permission denied.")
                false
            }
        }
        return false
    }

    // Rest of the AlarmScheduler code
    fun updateAlarm( context: Context, event: Event, isComingFromNotificationReceiver: Boolean = false ) {
        Log.i(TAG, "updateAlarm: ${event.date} ${event.month} ${event.year} | ${event.id}")
        ensureNotificationPermission(context) // Ensure permission before setting an alarm
        cancelAlarm(context, event)

        if (event.alertOffset != AlertOffset.NONE){
            Log.i(TAG, "updateAlarm: alertOffset is valid")
            if (event.triggerTime < System.currentTimeMillis() - 5000L){
                Log.i(TAG, "updateAlarm: TriggerTime is past time from current!")
            }else{
                scheduleExactTime(context, event.triggerTime, event)
            }

        }else{
            // do not set  any alarm
            Log.i(TAG, "updateAlarm: alertOffset is NONE")
        }
    }


    // Schedule the notification for a specific time.
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleExactTime(context: Context, triggerTime: Long, event: Event) {
        Log.i(TAG, "scheduleExactTime: ")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null, cannot schedule notification.")
            return
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "com.hardik.calendarapp.NOTIFY_EVENT"
            putExtra("event", event)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.hashCode(), // Unique request code for each event
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )

        Log.d(TAG, "Exact notification scheduled for event ID:- ${event.id} | at:- $triggerTime")
    }

    // Cancel the alarm for a specific event.
    private fun cancelAlarm(context: Context, event: Event) {
        val intent = Intent(context, NotificationReceiver::class.java)
        intent.action = "com.hardik.calendarapp.NOTIFY_EVENT"
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Alarm canceled for event ID: ${event.id}")
        } else {
            Log.e(TAG, "AlarmManager is null, cannot cancel alarm.")
        }
    }
}
