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

    //region Todo :here scheduleAlarm(event) is not cancel so keep cancel. cancelAllAlarms() by using 'getHolidayEvents' and then delete all event where sourceType is 'REMOTE'
    @Query("DELETE FROM events WHERE sourceType = :sourceType")
    suspend fun deleteEventsBySourceType(sourceType: SourceType = SourceType.REMOTE) // For deleting all events with a specific source type
    //select count(*) from events where sourceType = "LOCAL"// CURSOR/REMOTE

    @Query("SELECT * FROM events WHERE sourceType = :sourceType")
    fun getHolidayEvents(sourceType: SourceType = SourceType.REMOTE): Flow<List<Event>> // Todo: 'cancelAllRemoteAlarms' before delete all remote events
    //endregion


    //region Todo: GetEvents for year, month, date
    //@Query("SELECT * FROM events ORDER BY startTime ASC, endTime ASC, title ASC")
    //@Query("SELECT * FROM events WHERE (sourceType = 'LOCAL' OR sourceType = 'CURSOR' OR (sourceType = 'REMOTE' AND id IN (SELECT MIN(id) FROM events GROUP BY title))) ORDER BY year ASC, month ASC, date ASC, startTime ASC, endTime ASC, title ASC")
    @Query("SELECT * FROM events WHERE (sourceType = 'LOCAL' OR sourceType = 'CURSOR' OR (sourceType = 'REMOTE' AND id IN (SELECT MIN(id) FROM events GROUP BY title))) ORDER BY startTime ASC, endTime ASC, title ASC")
    fun getAllEvents(): Flow<List<Event>> // Todo: For schedule event list

    //@Query("SELECT * FROM events WHERE year = :year AND month = :month ORDER BY startTime ASC, endTime ASC, title ASC")
    @Query("SELECT * FROM events WHERE year = :year AND month = :month AND (sourceType = 'LOCAL' OR sourceType = 'CURSOR' OR (sourceType = 'REMOTE' AND id IN (SELECT MIN(id) FROM events WHERE year = :year AND month = :month GROUP BY title))) ORDER BY startTime ASC, endTime ASC, title ASC")
    fun getEventsByMonthOfTheYear(
        year: String,
        month: String
    ): Flow<List<Event>> // Todo: use in CalendarMonth1Fragment

    //@Query("SELECT * FROM events WHERE year = :year AND month = :month AND date =:date ORDER BY startTime ASC, endTime ASC, title ASC")
    @Query("SELECT * FROM events WHERE year = :year AND month = :month AND date = :date AND (sourceType = 'LOCAL' OR sourceType = 'CURSOR' OR (sourceType = 'REMOTE' AND id IN (SELECT MIN(id) FROM events WHERE year = :year AND month = :month AND date = :date GROUP BY title))) ORDER BY startTime ASC, endTime ASC, title ASC")
    fun getEventsByDateOfMonthOfTheYear(
        year: String,
        month: String,
        date: String
    ): Flow<List<Event>> // Todo: use in CalendarMonth1Fragment
    //endregion


    @Query("SELECT * FROM events WHERE title = :title AND eventType = :eventType LIMIT 1")
    fun getEventByTitleAndType(title: String, eventType: EventType): Flow<Event?>

}
