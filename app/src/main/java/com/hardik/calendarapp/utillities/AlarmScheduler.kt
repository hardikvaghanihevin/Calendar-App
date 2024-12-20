package com.hardik.calendarapp.utillities

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.data.database.entity.AlertOffset
import com.hardik.calendarapp.data.database.entity.AlertOffsetConverter
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

        // Early return for NONE: No notification should be scheduled.
        //if (event.repeatOption == RepeatOption.NONE) { Log.d(TAG, "No notification scheduled for event: ${event.title} (RepeatOption.NONE)"); return }
        if (event.alertOffset == AlertOffset.NONE) { Log.d(TAG, "No notification scheduled for event: ${event.title} (AlertOffset.NONE)"); return }

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

        val beforeTime: Long = if (event.alertOffset == AlertOffset.BEFORE_CUSTOM_TIME){
            event.customAlertOffset ?: 0L
        }else{
            AlertOffsetConverter.toMilliseconds(event.alertOffset) ?: 0L
        }
        // Calculate the trigger time using event's start time and alert offset.
        var triggerTime = event.startTime - beforeTime
        //val triggerTime = SystemClock.elapsedRealtime() + 5000 // 5 seconds from now
        Log.e(TAG, "scheduleNotification: TriggerTime:$triggerTime", )
        // If the trigger time is in the past, adjust it based on the repeat option
        while (triggerTime < System.currentTimeMillis()) {
            triggerTime = when (event.repeatOption) {
                //RepeatOption.NONE -> { return }
                //RepeatOption.ONCE -> { Log.e(TAG, "scheduleNotification: Cannot set alarm for a past time!", );return }// Exit if the event is a one-time event
                RepeatOption.NEVER -> { Log.e(TAG, "scheduleNotification: Cannot set alarm for a past time!", );return }// Exit if the event is a one-time event
                RepeatOption.DAILY -> triggerTime + AlarmManager.INTERVAL_DAY
                RepeatOption.WEEKLY -> triggerTime + AlarmManager.INTERVAL_DAY * 7
                RepeatOption.MONTHLY -> {
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = triggerTime
                        add(Calendar.MONTH, 1)
                    }
                    calendar.timeInMillis
                }
                RepeatOption.YEARLY -> {
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = triggerTime
                        add(Calendar.YEAR, 1)
                    }
                    calendar.timeInMillis
                }
            }
        }

        // Depending on the repeat option, schedule the alarm accordingly.
        // Schedule the alarm with the updated trigger time
        when (event.repeatOption) {
            //RepeatOption.NONE -> { return }
            //RepeatOption.ONCE -> { alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent) }
            RepeatOption.NEVER -> {
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
            RepeatOption.MONTHLY, RepeatOption.YEARLY -> {
                // For monthly or yearly events, reschedule using the exact calculated time
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
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
        Log.d(TAG, "Alarm scheduled for event: ${event.title}, Repeat: ${event.repeatOption}, Next Trigger: $triggerTime")
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
