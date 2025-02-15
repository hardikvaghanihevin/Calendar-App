package com.hardik.calendarapp.domain.repository

import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.data.database.entity.SourceType
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    suspend fun upsertEvent(event: Event)
    suspend fun upsertEvents(events: List<Event>)
    suspend fun deleteEvent(event: Event)
    suspend fun deleteEventsHoliday()
    suspend fun deleteEventsCursor()

    fun getAllEvents(): Flow<List<Event>>
    fun getEventsBySourceType(sourceType: SourceType): Flow<List<Event>>
    fun getEventsByMonthOfTheYear(year: String, month: String): Flow<List<Event>>
    fun getEventsByDateOfMonthOfTheYear(year: String, month: String, date: String): Flow<List<Event>>

    suspend fun cancelAlarm(event: Event)
}
