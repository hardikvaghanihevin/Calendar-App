package com.hardik.calendarapp.data.repository

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.util.Log
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.data.database.dao.EventDao
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.data.database.entity.SourceType
import com.hardik.calendarapp.domain.repository.CalendarRepository
import com.hardik.calendarapp.domain.repository.EventRepository
import com.hardik.calendarapp.utillities.CalendarContentObserver
import com.hardik.calendarapp.utillities.deleteCursorEventUtil
import com.hardik.calendarapp.utillities.getUserCustomEvents
import com.hardik.calendarapp.utillities.toEventList
import com.hardik.calendarapp.utillities.updateCursorEventUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

//class CalendarRepositoryImpl @Inject constructor(context: Context) : CalendarRepository {
//    private val TAG = Constants.BASE_TAG + CalendarRepositoryImpl::class.simpleName
//
//    private val contentResolver: ContentResolver = context.contentResolver
//    private val handler = Handler(Looper.getMainLooper())
//
//    private var eventListener: CalendarEventListener? = null
//
//    private val calendarObserver = CalendarContentObserver(handler) {
//        eventListener?.onCalendarEventsChanged() // Listener ko notify karein
//    }
//
//    override fun registerContentObserver() {
//        contentResolver.registerContentObserver(
//            CalendarContract.Events.CONTENT_URI,
//            true,
//            calendarObserver
//        )
//    }
//
//    override fun unregisterContentObserver() {
//        contentResolver.unregisterContentObserver(calendarObserver)
//    }
//
//    override fun setListener(listener: CalendarEventListener) {
//        this.eventListener = listener
//    }
//}
class CalendarRepositoryImpl @Inject constructor(
    private val context: Context,
    private val eventDao: EventDao,
    private val eventRepository: EventRepository,
) : CalendarRepository {

    private val handler = Handler(Looper.getMainLooper())

    // Create a CoroutineScope for background work
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val calendarObserver = CalendarContentObserver(handler) {
        coroutineScope.launch {
            syncCursorEvents() // Sync when calendar changes
        }
    }

    init {
        context.contentResolver.registerContentObserver(
            CalendarContract.Events.CONTENT_URI, true, calendarObserver
        )
    }


    override suspend fun syncCursorEvents() {
        Log.e(BASE_TAG, "syncCursorEvents: ", )
        val cursorEvents = getUserCustomEvents(context).toEventList() // Fetch from system calendar
        val storedEvents = eventRepository.getEventsBySourceType(SourceType.CURSOR).first() // Fetch from Room DB

        val cursorMap = cursorEvents.associateBy { it.id }
        val storedMap = storedEvents.associateBy { it.id }

        val newAndUpdatedEvents = cursorEvents.filter { event ->
            storedMap[event.id]?.let { storedEvent ->
                //event.startTime != storedEvent.startTime || event.endTime != storedEvent.endTime
                event != storedEvent  // Compares all fields
            } ?: true // If event is new (not in storedMap), add it
        }

        val deletedEvents = storedEvents.filter { it.id !in cursorMap.keys }

        Log.e(BASE_TAG, "syncCursorEvents: ${deletedEvents.size} - ${newAndUpdatedEvents.size}", )
        // Prevent unnecessary DB operations
        if (deletedEvents.isNotEmpty()) {
            eventRepository.deleteEvents(deletedEvents)
        }

        if (newAndUpdatedEvents.isNotEmpty()) {
            eventRepository.insertEvents(newAndUpdatedEvents)
        }
    }

    override suspend fun deleteCursorEvent(eventId: Long): Boolean {
        return deleteCursorEventUtil(context, eventId)
    }

    override suspend fun updateCursorEvent(event: Event): Boolean {
        return updateCursorEventUtil(context, event)
    }
}


