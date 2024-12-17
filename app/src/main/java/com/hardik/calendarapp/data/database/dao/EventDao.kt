package com.hardik.calendarapp.data.database.dao

import androidx.room.*
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.data.database.entity.EventType
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvent(event: Event)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvents(event: List<Event>)

    @Update
    suspend fun updateEvent(event: Event)

    @Delete
    suspend fun deleteEvent(event: Event)

    @Query("SELECT * FROM events WHERE eventId = :eventId LIMIT 1")
    fun getEventById(eventId: Long): Flow<Event>?

    @Query("SELECT eventId FROM events")
    fun getAllEventIds(): Flow<List<Long>>

    @Query("SELECT * FROM events")
    suspend fun getAllEvents(): List<Event>

    @Query("SELECT * FROM events")
    fun getAllEventsFlow(): Flow<List<Event>> // Flow-based query

    @Query("SELECT * FROM events WHERE isHoliday = 1")
    suspend fun getHolidayEvents(): List<Event>

    @Query("SELECT * FROM events WHERE isHoliday = 1")
    fun getHolidayEventsFlow(): Flow<List<Event>> // Flow-based query

    @Query("SELECT * FROM events WHERE startTime >= :startOfMonth AND startTime <= :endOfMonth ORDER BY startTime ASC, endTime DESC, title ASC")//todo: use in CalendarMonthFragment
    fun getEventsForMonth(startOfMonth: Long, endOfMonth: Long): Flow<List<Event>>

    //val sortedEvents = events.sortedWith(compareBy<Event> { it.startTime }.thenBy { it.title })
    @Query("SELECT * FROM events WHERE year = :year AND month = :month ORDER BY startTime ASC, endTime ASC")//todo: use in CalendarMonth1Fragment
    fun getEventsByMonthOfYear(year: String, month: String): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE year = :year AND month = :month AND date =:date ORDER BY startTime ASC, endTime ASC")//todo: use in CalendarMonth1Fragment
    fun getEventsByDateOfMonthOfYear(year: String, month: String, date: String): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE year = :year AND month = :month")
    fun getEventsByYearAndMonth(year: String, month: String): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE title = :title AND eventType = :eventType LIMIT 1")
    fun getEventByTitleAndType(title: String, eventType: EventType): Flow<Event?>

}
