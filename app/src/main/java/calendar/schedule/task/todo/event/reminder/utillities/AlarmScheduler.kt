package calendar.schedule.task.todo.event.reminder.utillities

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import calendar.schedule.task.todo.event.reminder.common.Constants.BASE_TAG
import calendar.schedule.task.todo.event.reminder.common.Constants.KEY_EVENT_JSON
import calendar.schedule.task.todo.event.reminder.data.database.entity.AlertOffset
import calendar.schedule.task.todo.event.reminder.data.database.entity.Event
import calendar.schedule.task.todo.event.reminder.presentation.receiver.NotificationReceiver
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object AlarmScheduler {
    private val TAG = BASE_TAG + AlarmScheduler::class.simpleName

    private val updateAlarmMutex = Mutex()
    // Rest of the AlarmScheduler code
    suspend fun updateAlarm(context: Context, event: Event, isComingFromNotificationReceiver: Boolean = false ) {
        updateAlarmMutex.withLock {
            cancelAlarm(context, event)

            if (event.alertOffset != AlertOffset.NONE){
                // updateAlarm: alertOffset is valid
                scheduleExactTime(context, event.triggerTime, event)
            }else{
                // do not set any alarm, because updateAlarm: alertOffset is NONE
            }
        }
    }


    // Schedule the notification for a specific time.
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleExactTime(context: Context, triggerTime: Long, event: Event) {
        // AlarmManager is null, cannot schedule notification.
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val currentTime = System.currentTimeMillis()

        // Skip scheduling if the time is already in the past
        if (triggerTime <= currentTime) {
            return
        }

        val eventJson = GsonUtil.toJson(event)

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "calendar.schedule.task.todo.event.reminder.NOTIFY_EVENT"

            putExtra(KEY_EVENT_JSON, eventJson)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.hashCode(), // Unique request code for each event
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        //alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)//AlarmClockInfo()
        //alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerTime, pendingIntent), pendingIntent)

        Log.e(TAG, "scheduleExactTime: $triggerTime", )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
                if (alarmManager.canScheduleExactAlarms()) {
                    Log.w(TAG,"Alarm: Exact alarms allowed. Using setExactAndAllowWhileIdle alarm.")
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    Log.w(TAG,"Alarm: Exact alarms not allowed. Using inexact alarm.")
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)

                }
            } else { // Android 7+ (API 24-30)
                Log.w(TAG,"Alarm: Exact alarms allowed. Using setExactAndAllowWhileIdle alarm.(API 24-30)")
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.e(TAG,"Alarm: SecurityException: ${e.message}")
        }
    }

    // Cancel the alarm for a specific event.
    fun cancelAlarm(context: Context, event: Event) {
        val intent = Intent(context, NotificationReceiver::class.java)
        intent.action = "calendar.schedule.task.todo.event.reminder.NOTIFY_EVENT"
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
