package com.hardik.calendarapp.data.repository

import com.hardik.calendarapp.data.database.dao.EventDao
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.data.database.entity.EventType
import com.hardik.calendarapp.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao
) : EventRepository {

    override suspend fun upsertEvent(event: Event) {
        eventDao.upsertEvent(event)
    }

    override suspend fun upsertEvents(events: List<Event>) {
        eventDao.upsertEvents(events)
    }

    override suspend fun updateEvent(event: Event) {
        eventDao.updateEvent(event)
    }

    override suspend fun deleteEvent(event: Event) {
        eventDao.deleteEvent(event)
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
}
