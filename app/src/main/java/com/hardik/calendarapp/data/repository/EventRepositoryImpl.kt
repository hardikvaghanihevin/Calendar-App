package com.hardik.calendarapp.data.repository

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.data.database.dao.EventDao
import com.hardik.calendarapp.data.database.entity.AlertOffsetConverter
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.data.database.entity.SourceType
import com.hardik.calendarapp.domain.repository.EventRepository
import com.hardik.calendarapp.utillities.AlarmScheduler
import com.hardik.calendarapp.utillities.DateUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao,
    private val context: Context
) : EventRepository {
    private val TAG = BASE_TAG + EventRepositoryImpl::class.simpleName

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val _deletedEventFlow = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    override val deletedEventFlow: SharedFlow<Event> = _deletedEventFlow.asSharedFlow()

    private val insertEventsMutex = Mutex()
    override suspend fun insertEvents(events: List<Event>) {
        events.forEach {
            if (it.sourceType == SourceType.CURSOR)
                Log.e(BASE_TAG, "final list: ${it.repeatOption},- ${it.title},- ${it.startTime},- ${it.isAllDay} ", )
        }

        // Ensure only one coroutine executes this block at a time
        insertEventsMutex.withLock {
            try {
                // Update each event's nextTriggerTime
                val updatedEvents = coroutineScope {
                    events.map { event ->
                        async(Dispatchers.Default) {
                            var nextTriggerTime = event.triggerTime

                            // Calculate nextTriggerTime if needed
                            val minus: Long = AlertOffsetConverter.toMilliseconds(event.alertOffset) ?: 0L
                            val calculatedTriggerTime = DateUtil.calculateNextOccurrence(event.startTime, event.repeatOption)

                            nextTriggerTime = if (calculatedTriggerTime != null) {
                                calculatedTriggerTime - minus
                            }else{
                                event.startTime - minus
                            }

                            // Return the updated event
                            event.copy(triggerTime = nextTriggerTime)
                        }
                    }.awaitAll() // Collect all updated events
                }

                withContext(Dispatchers.IO) {
                    this@EventRepositoryImpl.upsertEvents(updatedEvents)
                }

            } catch (e: Exception) {
                // Handle any errors
            }finally {

            }
        }
    }
    override suspend fun upsertEvent(event: Event) {
        eventDao.upsertEvent(event)
        setAlarm(event)       // Set a new alarm for this event
    }

    override suspend fun upsertEvents(events: List<Event>) {
        eventDao.upsertEvents(events)
        scheduleAlarms(events)
    }

    override suspend fun deleteEvent(event: Event): Int {
        cancelAlarm(event) // before delete
        val rowsAffected = eventDao.deleteEvent(event)
        if (rowsAffected > 0) {
            _deletedEventFlow.emit(event) // Notify deletion
        }
        return rowsAffected
    }

    override suspend fun deleteEvents(events: List<Event>): Int {
        events.forEach { event -> cancelAlarm(event) }// before delete
        return eventDao.deleteEvents(events)
    }

    override suspend fun deleteEventsHoliday(){
        //Todo :here scheduleAlarm(event) is not cancel so keep cancel. cancelAllAlarms()
       deleteEventsBySourceType(sourceType = SourceType.REMOTE)
    }

    override suspend fun deleteEventsCursor(){
        //Todo :here scheduleAlarm(event) is not cancel so keep cancel. cancelAllAlarms()
        deleteEventsBySourceType(sourceType = SourceType.CURSOR)
    }

    override suspend fun deleteEventsBySourceType(sourceType: SourceType){
        CoroutineScope(Dispatchers.IO).launch {
            if (sourceType == SourceType.REMOTE){
                cancelAllRemoteAlarms(sourceType)
            }
            if (sourceType == SourceType.CURSOR){
                cancelAllCursorAlarms(sourceType)
            }
        }.join()
        val eventsToDelete = eventDao.getEventsBySourceType(sourceType).firstOrNull() ?: emptyList()
        eventDao.deleteEventsBySourceType(sourceType = sourceType)
        eventsToDelete.forEach {
            _deletedEventFlow.emit(it) // Notify each deleted event
        }
    }


    override fun getRemoteEvents(): Flow<List<Event>> {
        return eventDao.getRemoteEvents()
    }

    override fun getAllEvents(): Flow<List<Event>> {
        return eventDao.getAllEvents()
    }

    override fun getEventsBySourceType(sourceType: SourceType): Flow<List<Event>> {
        return eventDao.getEventsBySourceType(sourceType = sourceType)
    }

    override fun getEventsByMonthOfTheYear(year: String, month: String): Flow<List<Event>>{
        return eventDao.getEventsByMonthOfTheYear(year = year, month = month)
    }
     override fun getEventsByDateOfMonthOfTheYear(year: String, month: String, date: String): Flow<List<Event>>{
        return eventDao.getEventsByDateOfMonthOfTheYear(year = year, month = month, date = date)
     }

    override suspend fun scheduleAlarms(events: List<Event>) {
        // Get the current date and the date 365 days later

        val timeSlap: Pair<Long, Long> = DateUtil.getCurrentAndFutureRange(daysInFuture = 30)

        // Use supervisorScope to handle independent coroutines
        supervisorScope {

            events.forEach { event:Event ->
                // Launch a coroutine for each event
                launch(Dispatchers.Default) {

                    if (event.triggerTime in timeSlap.first..timeSlap.second) {
                        setAlarm(event)
                    }
                }

            }//todo : schedule alarm if current year
        }
    }

    private val alarmScheduleMutex = Mutex()
    private suspend fun setAlarm(event: Event) {
        if (!hasExactAlarmPermission()) {
            // Exact alarm permission missing.
            requestExactAlarmPermission()//todo: here error occur on API 34 when event create( it has not permission to alarm manager)
        } else {
            alarmScheduleMutex.withLock{
                AlarmScheduler.updateAlarm(context, event)
            }
            // Alarm scheduled for event: ${event}
        }
    }

    override suspend fun cancelAlarm(event: Event) {
        AlarmScheduler.cancelAlarm(context = context, event = event )
    }
    private suspend fun cancelAllRemoteAlarms(sourceType: SourceType) {
        cancelAllAlarmsBySourceType(sourceType)
    }
    private suspend fun cancelAllCursorAlarms(sourceType: SourceType) {
        cancelAllAlarmsBySourceType(sourceType)
    }

    private suspend fun cancelAllAlarmsBySourceType(sourceType: SourceType) {
        CoroutineScope(Dispatchers.IO).launch {
            eventDao.getEventsBySourceType(sourceType).collectLatest {
                it.forEach { event -> cancelAlarm(event) }
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
