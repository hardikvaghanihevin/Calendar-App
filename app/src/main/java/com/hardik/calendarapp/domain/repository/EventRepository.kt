package com.hardik.calendarapp.domain.repository

import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.data.database.entity.EventType
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    suspend fun upsertEvent(event: Event)
    suspend fun upsertEvents(events: List<Event>)
    suspend fun updateEvent(event: Event)
    suspend fun deleteEvent(event: Event)

    fun getAllEvents(): Flow<List<Event>>
    fun getHolidayEvents(): Flow<List<Event>>
    fun getEventsForMonth(startOfMonth: Long, endOfMonth: Long): Flow<List<Event>>
    fun getEventsByMonthOfYear(year: String, month: String): Flow<List<Event>>
    fun getEventsByDateOfMonthOfYear(year: String, month: String, date: String): Flow<List<Event>>
    fun getEventsByYearAndMonth(year: String, month: String): Flow<List<Event>>

    fun getEventByTitleAndType(title: String, eventType: EventType): Flow<Event?>
}
