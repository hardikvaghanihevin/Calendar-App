package com.hardik.calendarapp.domain.repository

import com.hardik.calendarapp.data.database.entity.Event

//interface CalendarRepository {
//    fun registerContentObserver()
//    fun unregisterContentObserver()
//    fun setListener(listener: CalendarEventListener)
//}
//
//interface CalendarEventListener {
//    fun onCalendarEventsChanged()
//}
interface CalendarRepository{
    suspend fun syncCursorEvents()
    suspend fun deleteCursorEvent(eventId: Long): Boolean
    suspend fun updateCursorEvent(event: Event): Boolean
}