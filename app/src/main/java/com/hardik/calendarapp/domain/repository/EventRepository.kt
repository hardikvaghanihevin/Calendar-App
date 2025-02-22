package com.hardik.calendarapp.domain.repository

import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.data.database.entity.SourceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface EventRepository {

    val deletedEventFlow: SharedFlow<Event>
    suspend fun upsertEvent(event: Event)
    suspend fun upsertEvents(events: List<Event>)
    suspend fun deleteEvent(event: Event): Int
    suspend fun deleteEventsHoliday()
    suspend fun deleteEventsCursor()
    suspend fun deleteEventsBySourceType(sourceType: SourceType)

    fun getRemoteEvents(): Flow<List<Event>>
    fun getAllEvents(): Flow<List<Event>>
    fun getEventsBySourceType(sourceType: SourceType): Flow<List<Event>>
    fun getEventsByMonthOfTheYear(year: String, month: String): Flow<List<Event>>
    fun getEventsByDateOfMonthOfTheYear(year: String, month: String, date: String): Flow<List<Event>>

    suspend fun scheduleAlarms(events: List<Event>)
    suspend fun cancelAlarm(event: Event)
}
