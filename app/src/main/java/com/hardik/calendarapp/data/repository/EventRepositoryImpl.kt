package com.hardik.calendarapp.data.repository

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.data.database.dao.EventDao
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.data.database.entity.EventType
import com.hardik.calendarapp.data.database.entity.SourceType
import com.hardik.calendarapp.domain.repository.EventRepository
import com.hardik.calendarapp.utillities.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao,
    private val context: Context
) : EventRepository {
    private val TAG = BASE_TAG + EventRepositoryImpl::class.simpleName

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override suspend fun upsertEvent(event: Event) {
        Log.d(TAG, "upsertEvent() called with: event = $event")
        //cancelAlarm(event.eventId) // Cancel any existing alarms for this event
        eventDao.upsertEvent(event)
        scheduleAlarm(event)       // Set a new alarm for this event
    }

    override suspend fun upsertEvents(events: List<Event>) {
        // Cancel all existing alarms and reschedule
        cancelAllAlarms()

        // Step 1: Check if the eventId exists
        events.forEach { event ->
            // Collect the Flow to check if the eventId exists
            val exists = eventDao.getEventById(event.eventId)?.firstOrNull() // Collect only the first result (suspendable function)

            if (exists != null) {
                Log.d(TAG,"UpsertEvents -> Event with ID: ${event.eventId} already exists, updating...")
            } else {
                Log.v(TAG,"UpsertEvents ->New event inserted with ID: ${event.eventId}.")
            }
        }
        eventDao.upsertEvents(events)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR).toString()
        events.forEach { event:Event ->
            if (event.year == currentYear) {
                scheduleAlarm(event)
            }
        }
    }

    override suspend fun updateEvent(event: Event) {
        eventDao.updateEvent(event)
    }

    override suspend fun deleteEvent(event: Event) {
        cancelAlarm(event.eventId)
        eventDao.deleteEvent(event)
    }

    override suspend fun deleteEventsHoliday(){
        //todo :here scheduleAlarm(event) is not cancel so keep cancel. cancelAllAlarms()
        eventDao.deleteEventsBySourceType(sourceType = SourceType.REMOTE)
    }

    override fun getEventById(eventId: Long): Flow<Event>?{
        return eventDao.getEventById(eventId)
    }
    override fun getAllEventIds(): Flow<List<Long>>{
        return eventDao.getAllEventIds()
    }

    override fun getAllEvents(): Flow<List<Event>> {
        return eventDao.getAllEventsFlow()
    }

    override fun getHolidayEvents(): Flow<List<Event>> {
        return eventDao.getHolidayEventsFlow()
    }

    override fun getEventsForMonth(startOfMonth: Long, endOfMonth: Long): Flow<List<Event>> {
        return eventDao.getEventsForMonth(startOfMonth, endOfMonth)
    }

    override fun getEventsByMonthOfYear(year: String, month: String): Flow<List<Event>>{
        return eventDao.getEventsByMonthOfYear(year = year, month = month)
    }
     override fun getEventsByDateOfMonthOfYear(year: String, month: String, date: String): Flow<List<Event>>{
        return eventDao.getEventsByDateOfMonthOfYear(year = year, month = month, date = date)
     }

    override fun getEventsByYearAndMonth(year: String, month: String): Flow<List<Event>>{
        return eventDao.getEventsByYearAndMonth(year, month)
    }

    override fun getEventByTitleAndType(title: String, eventType: EventType): Flow<Event?>{
        return eventDao.getEventByTitleAndType(title = title, eventType = eventType)
    }


    private fun scheduleAlarm(event: Event) {
        if (!hasExactAlarmPermission()) {
            Log.w(TAG, "Exact alarm permission missing.")
            requestExactAlarmPermission()
        } else {
            AlarmScheduler.updateAlarm(context, event)
            Log.i(TAG, "Alarm scheduled for event: ${event}")
            //Toast.makeText(context, "Alarm set for event: ${event.title}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelAlarm(eventId: Long) {
//        val intent = Intent(context, NotificationReceiver::class.java)
//        val pendingIntent = PendingIntent.getBroadcast(
//            context,
//            eventId.toInt(),
//            intent,
//            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
//        )
//        pendingIntent?.let {
//            alarmManager.cancel(it)
//            Log.d(TAG, "Alarm canceled for eventId: $eventId")
//        }
        AlarmScheduler.cancelAlarm(context , eventId.toInt())
    }

    private fun cancelAllAlarms() {
        CoroutineScope(Dispatchers.IO).launch {
            //eventDao.getAllEvents().forEach { event -> cancelAlarm(event.eventId) }
            eventDao.getAllEventIds().collectLatest {
                val ids: List<Int> = it.map { it.toInt() }
                AlarmScheduler.cancelAllScheduledAlarms(context, ids)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply { data = android.net.Uri.parse("package:${context.packageName}") }
            Toast.makeText(context, "Please allow exact alarm permission.", Toast.LENGTH_LONG).show()
            context.startActivity(intent) }
        else {
            Toast.makeText(context, "Exact alarm permission not needed for this version.", Toast.LENGTH_SHORT).show() }
    }
}
