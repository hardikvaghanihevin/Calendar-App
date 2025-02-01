package com.hardik.calendarapp.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.data.database.dao.EventDao
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.data.database.entity.EventType
import com.hardik.calendarapp.data.database.entity.SourceType
import com.hardik.calendarapp.domain.repository.EventRepository
import com.hardik.calendarapp.presentation.receiver.NotificationReceiver
import com.hardik.calendarapp.utillities.AlarmScheduler
import com.hardik.calendarapp.utillities.DateUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao,
    private val context: Context
) : EventRepository {
    private val TAG = BASE_TAG + EventRepositoryImpl::class.simpleName

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override suspend fun upsertEvent(event: Event) {
        eventDao.upsertEvent(event)
        scheduleAlarm(event)       // Set a new alarm for this event
    }

    override suspend fun upsertEvents(events: List<Event>) {
        eventDao.upsertEvents(events)

        // Get the current date and the date 365 days later

        val timeSlap: Pair<Long, Long> = DateUtil.getCurrentAndFutureRange()

        // Use supervisorScope to handle independent coroutines
        supervisorScope {

            events.forEach { event:Event ->
            // Launch a coroutine for each event
                launch(Dispatchers.Default) {

                if (event.triggerTime in timeSlap.first..timeSlap.second) { scheduleAlarm(event) }}

            }//todo : schedule alarm if current year
        }
    }

    override suspend fun updateEvent(event: Event) {
        eventDao.updateEvent(event)
    }

    override suspend fun deleteEvent(event: Event) {
        cancelAlarm(event.id) // before delete
        eventDao.deleteEvent(event)
    }

    override suspend fun deleteEventsHoliday(){
        //Todo :here scheduleAlarm(event) is not cancel so keep cancel. cancelAllAlarms()
        cancelAllRemoteAlarms()
        eventDao.deleteEventsBySourceType(sourceType = SourceType.REMOTE)
    }


    override fun getAllEvents(): Flow<List<Event>> {
        return eventDao.getAllEvents()
    }

    override fun getEventsByMonthOfTheYear(year: String, month: String): Flow<List<Event>>{
        return eventDao.getEventsByMonthOfTheYear(year = year, month = month)
    }
     override fun getEventsByDateOfMonthOfTheYear(year: String, month: String, date: String): Flow<List<Event>>{
        return eventDao.getEventsByDateOfMonthOfTheYear(year = year, month = month, date = date)
     }

    override fun getEventByTitleAndType(title: String, eventType: EventType): Flow<Event?>{
        return eventDao.getEventByTitleAndType(title = title, eventType = eventType)
    }


    private fun scheduleAlarm(event: Event) {
        if (!hasExactAlarmPermission()) {
            // Exact alarm permission missing.
            requestExactAlarmPermission()//todo: here error occur on API 34 when event create( it has not permission to alarm manager)
        } else {
            AlarmScheduler.updateAlarm(context, event)
            // Alarm scheduled for event: ${event}
        }
    }

    override suspend fun cancelAlarm(id: String) {
        val intent = Intent(context, NotificationReceiver::class.java)
        intent.action = "com.hardik.calendarapp.NOTIFY_EVENT"
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }
    private suspend fun cancelAllRemoteAlarms() {
        CoroutineScope(Dispatchers.IO).launch {
            eventDao.getHolidayEvents().collectLatest {
                it.forEach { event -> cancelAlarm(event.id) }// currently no use
            }
        }
    }

    // Check if the app has the SCHEDULE_EXACT_ALARM permission
    private fun hasExactAlarmPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms() }
        else { true }// Permission not required on older versions
    }

    // Request exact alarm permission
    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasExactAlarmPermission()) {
            // Direct the user to the settings page for the app's exact alarm permission
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply { data = Uri.parse("package:${context.packageName}") }
            context.startActivity(intent)
            //Toast.makeText(context, "Please allow exact alarm permission.", Toast.LENGTH_LONG).show()
        }
        else {
            Toast.makeText(context, "Exact alarm permission not needed for this version.", Toast.LENGTH_SHORT).show() }
    }
}
