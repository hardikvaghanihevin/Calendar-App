package com.hardik.calendarapp.utillities

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.data.database.entity.RepeatOption
import com.hardik.calendarapp.presentation.receiver.NotificationReceiver
import java.util.Calendar

object AlarmScheduler {
    private val TAG = BASE_TAG + AlarmScheduler::class.simpleName

    // Update alarm logic: reschedule and cancel any existing alarm first.
    fun updateAlarm(context: Context, event: Event) {
        cancelAlarm(context, event)
        scheduleNotification(context, event)
    }

    // Schedule the notification based on repeat option.
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleNotification(context: Context, event: Event) {
        Log.e(TAG, "scheduleNotification: ", )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("event_title", event.title)           // Pass event title
            putExtra("event_description", event.description) // Pass event description
            putExtra("event_id", event.eventId)            // Pass event ID
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.eventId.toInt(), // Unique request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculate the trigger time using event's start time and alert offset.
        val triggerTime = event.startTime + event.alertOffset
        //val triggerTime = SystemClock.elapsedRealtime() + 5000 // 5 seconds from now
        Log.e(TAG, "scheduleNotification: $triggerTime", )

        // Depending on the repeat option, schedule the alarm accordingly.
        when (event.repeatOption) {
            RepeatOption.ONCE -> {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            RepeatOption.DAILY -> {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
            RepeatOption.WEEKLY -> {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    AlarmManager.INTERVAL_DAY * 7,
                    pendingIntent
                )
            }
            RepeatOption.MONTHLY -> {
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = triggerTime
                    add(Calendar.MONTH, 1)
                }
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
           /* RepeatOption.MONTHLY -> {
                val interval = 30L * 24 * 60 * 60 * 1000 // Approx. 30 days in milliseconds
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    interval,
                    pendingIntent
                )
            }*/
            RepeatOption.YEARLY -> {
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = triggerTime
                    add(Calendar.YEAR, 1)
                }
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
           /* RepeatOption.YEARLY -> {
                val interval = 365L * 24 * 60 * 60 * 1000 // Approx. 365 days in milliseconds
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    interval,
                    pendingIntent
                )
            }*/
        }

        // Show a Toast confirming the alarm is set.
        Log.d(TAG, "Alarm scheduled for event: ${event.title}, Repeat: ${event.repeatOption}")
        //Toast.makeText(context, "Alarm scheduled for: ${event.title}", Toast.LENGTH_SHORT).show()
    }

    // Cancel existing alarms if any.
    fun cancelAlarm(context: Context, event: Event) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.eventId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Alarm canceled for event: ${event.title}")
    }
}
