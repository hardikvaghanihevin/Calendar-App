package com.hardik.calendarapp.utillities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
                    // PermissionError: Context is not an Activity. Cannot request permissions.
                }
            }
        }
    }

    // Handle the result of the permission request
    fun handlePermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
        if (requestCode == REQUEST_CODE_CALENDAR_PERMISSIONS) {
            return grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        }
        return false
    }

    // Rest of the AlarmScheduler code
    fun updateAlarm( context: Context, event: Event, isComingFromNotificationReceiver: Boolean = false ) {
        ensureNotificationPermission(context) // Ensure permission before setting an alarm
        cancelAlarm(context, event)

        if (event.alertOffset != AlertOffset.NONE){
            // updateAlarm: alertOffset is valid
            if (event.triggerTime < System.currentTimeMillis() - 5000L){
                // updateAlarm: TriggerTime is past time from current!
            }else{
                scheduleExactTime(context, event.triggerTime, event)
            }

        }else{
            // do not set any alarm, because updateAlarm: alertOffset is NONE
        }
    }


    // Schedule the notification for a specific time.
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleExactTime(context: Context, triggerTime: Long, event: Event) {
        // AlarmManager is null, cannot schedule notification.
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

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
            // Alarm canceled for event ID: ${event.id}
        } else {
            // AlarmManager is null, cannot cancel alarm.
        }
    }
}
