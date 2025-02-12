package com.hardik.calendarapp.data.database.dao

import androidx.room.*
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.data.database.entity.SourceType
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvent(event: Event)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvents(event: List<Event>)

    @Delete
    suspend fun deleteEvent(event: Event)

    //region Todo :here scheduleAlarm(event) is not cancel so keep cancel. cancelAllAlarms() by using 'getHolidayEvents' and then delete all event where sourceType is 'REMOTE'
    @Query("DELETE FROM events WHERE sourceType = :sourceType")
    suspend fun deleteEventsBySourceType(sourceType: SourceType = SourceType.REMOTE) // For deleting all events with a specific source type

    @Query("SELECT * FROM events WHERE sourceType = :sourceType")
    fun getHolidayEvents(sourceType: SourceType = SourceType.REMOTE): Flow<List<Event>> // Todo: 'cancelAllRemoteAlarms' before delete all remote events
    //endregion


    //region Todo: GetEvents for year, month, date
    @Query("SELECT * FROM events e1 WHERE ( e1.sourceType = 'LOCAL' OR e1.sourceType = 'CURSOR' OR ( e1.sourceType = 'REMOTE' AND NOT EXISTS ( SELECT 1 FROM events e2 WHERE e2.year = e1.year AND e2.month = e1.month AND e2.title = e1.title AND e2.sourceType = 'CURSOR' ) ) ) ORDER BY e1.startTime ASC, e1.endTime ASC, e1.title ASC")
    fun getAllEvents(): Flow<List<Event>> // Todo: For schedule event list

    @Query("SELECT * FROM events e1 WHERE e1.year =:year AND e1.month =:month AND ( e1.sourceType = 'LOCAL' OR e1.sourceType = 'CURSOR' OR ( e1.sourceType = 'REMOTE' AND NOT EXISTS ( SELECT 1 FROM events e2 WHERE e2.year = e1.year AND e2.month = e1.month AND e2.title = e1.title AND e2.sourceType = 'CURSOR' ) ) ) ORDER BY e1.startTime ASC, e1.endTime ASC, e1.title ASC")
    fun getEventsByMonthOfTheYear(year: String, month: String): Flow<List<Event>> // Todo: use in CalendarMonth1Fragment

   @Query("SELECT * FROM events e1 WHERE e1.year =:year AND e1.month =:month AND date = :date AND ( e1.sourceType = 'LOCAL' OR e1.sourceType = 'CURSOR' OR ( e1.sourceType = 'REMOTE' AND NOT EXISTS ( SELECT 1 FROM events e2 WHERE e2.year = e1.year AND e2.month = e1.month AND date = :date AND e2.title = e1.title AND e2.sourceType = 'CURSOR' ) ) ) ORDER BY e1.startTime ASC, e1.endTime ASC, e1.title ASC")
    fun getEventsByDateOfMonthOfTheYear(year: String, month: String, date: String): Flow<List<Event>> // Todo: use in CalendarMonth1Fragment
    //endregion

}
