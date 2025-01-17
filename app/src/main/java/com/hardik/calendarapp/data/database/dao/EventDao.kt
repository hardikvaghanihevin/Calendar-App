package com.hardik.calendarapp.data.database.dao

import androidx.room.*
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.data.database.entity.EventType
import com.hardik.calendarapp.data.database.entity.SourceType
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

    @Query("DELETE FROM events WHERE sourceType = :sourceType")
    suspend fun deleteEventsBySourceType(sourceType: SourceType = SourceType.REMOTE) // For deleting all events with a specific source type
    //select count(*) from events where sourceType = "LOCAL"// CURSOR/REMOTE

    @Query("SELECT * FROM events WHERE eventId = :eventId LIMIT 1")
    fun getEventById(eventId: Long): Flow<Event>?

    @Query("SELECT eventId FROM events")
    fun getAllEventIds(): Flow<List<Long>>

    @Query("SELECT * FROM events")
    suspend fun getAllEvents(): List<Event>

    //@Query("SELECT * FROM events ORDER BY startTime ASC, endTime ASC, title ASC")
    @Query("SELECT * FROM events WHERE id IN (SELECT id FROM events WHERE sourceType = 'CURSOR' GROUP BY title, startTime, endTime UNION ALL SELECT id FROM events WHERE sourceType = 'REMOTE' AND NOT EXISTS (SELECT 1 FROM events e2 WHERE e2.title = events.title AND e2.startTime = events.startTime AND e2.endTime = events.endTime AND e2.sourceType = 'CURSOR') )ORDER BY startTime ASC, endTime ASC, title ASC")
    fun getAllEventsFlow(): Flow<List<Event>> // Flow-based query

    @Query("SELECT * FROM events WHERE isHoliday = 1")
    suspend fun getHolidayEvents(): List<Event>

    @Query("SELECT * FROM events WHERE sourceType = :sourceType")
    fun getHolidayEventsFlow(sourceType: SourceType = SourceType.REMOTE): Flow<List<Event>> // Flow-based query

    //@Query("SELECT * FROM events WHERE startTime >= :startOfMonth AND startTime <= :endOfMonth ORDER BY startTime ASC, endTime DESC, title ASC")//todo: use in CalendarMonthFragment
    @Query("SELECT * FROM events WHERE startTime >= :startOfMonth AND startTime <= :endOfMonth ORDER BY startTime ASC, endTime ASC, title ASC")//todo: use in CalendarMonthFragment
    fun getEventsForMonth(startOfMonth: Long, endOfMonth: Long): Flow<List<Event>>

    //val sortedEvents = events.sortedWith(compareBy<Event> { it.startTime }.thenBy { it.title })
    //@Query("SELECT * FROM events WHERE year = :year AND month = :month ORDER BY startTime ASC, endTime ASC, title ASC")//todo: use in CalendarMonth1Fragment
    @Query("SELECT * FROM events WHERE year = :year AND month = :month AND id IN (SELECT id FROM events WHERE year = :year AND month = :month AND sourceType = 'CURSOR' GROUP BY title, startTime, endTime UNION ALL SELECT id FROM events WHERE year = :year AND month = :month AND sourceType = 'REMOTE' AND NOT EXISTS (SELECT 1 FROM events e2  WHERE e2.title = events.title AND e2.startTime = events.startTime AND e2.endTime = events.endTime  AND e2.sourceType = 'CURSOR' ) )ORDER BY startTime ASC, endTime ASC, title ASC")
    fun getEventsByMonthOfYear(year: String, month: String): Flow<List<Event>>
    //@Query("SELECT * FROM events WHERE year = :year AND month = :month AND date =:date ORDER BY startTime ASC, endTime ASC, title ASC")//todo: use in CalendarMonth1Fragment
    @Query("SELECT * FROM events WHERE year = :year AND month = :month AND date = :date AND id IN ( SELECT id FROM events WHERE year = :year AND month = :month AND date = :date AND sourceType = 'CURSOR' GROUP BY title, startTime, endTime UNION ALL SELECT id FROM events WHERE year = :year AND month = :month AND date = :date AND sourceType = 'REMOTE' AND NOT EXISTS ( SELECT 1 FROM events e2 WHERE e2.title = events.title AND e2.startTime = events.startTime AND e2.endTime = events.endTime AND e2.date = events.date AND e2.sourceType = 'CURSOR')) ORDER BY startTime ASC, endTime ASC, title ASC")
    fun getEventsByDateOfMonthOfYear(year: String, month: String, date: String): Flow<List<Event>>

    //@Query("SELECT * FROM events WHERE year = :year AND month = :month ORDER BY startTime ASC, endTime ASC, title ASC")
    @Query("SELECT * FROM events WHERE year = :year AND month = :month AND id IN (SELECT id FROM events WHERE year = :year AND month = :month AND sourceType = 'CURSOR' GROUP BY title, startTime, endTime UNION ALL SELECT id FROM events WHERE year = :year AND month = :month AND sourceType = 'REMOTE' AND NOT EXISTS (SELECT 1 FROM events e2  WHERE e2.title = events.title AND e2.startTime = events.startTime AND e2.endTime = events.endTime  AND e2.sourceType = 'CURSOR' ) )ORDER BY startTime ASC, endTime ASC, title ASC")
    fun getEventsByYearAndMonth(year: String, month: String): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE title = :title AND eventType = :eventType LIMIT 1")
    fun getEventByTitleAndType(title: String, eventType: EventType): Flow<Event?>

}
