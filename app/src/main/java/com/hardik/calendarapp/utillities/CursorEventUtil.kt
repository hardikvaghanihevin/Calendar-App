package com.hardik.calendarapp.utillities

import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.os.Handler
import android.provider.CalendarContract
import android.util.Log
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.data.database.entity.AlertOffset
import com.hardik.calendarapp.data.database.entity.AlertOffsetConverter
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.data.database.entity.RepeatOption
import com.hardik.calendarapp.data.database.entity.RepeatOptionConverter
import com.hardik.calendarapp.data.database.entity.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.TimeZone

data class CursorEvent(
    val id: String = "",
    val calendarId: Long,
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val startTime: Long,
    val endTime: Long,
    val isAllDay: Boolean = false,
    val timeZone: String = TimeZone.getDefault().id,
    val repeatOption: RepeatOption = RepeatOption.NEVER,
    val alertOffset: AlertOffset = AlertOffset.AT_TIME_OF_EVENT,
    val customAlertOffset: Long? = null,
)

suspend fun getAllCursorEvents(context: Context): List<CursorEvent> =
    withContext(Dispatchers.IO) {
        val events = mutableListOf<CursorEvent>()
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.RRULE,
        )
        val uri = CalendarContract.Events.CONTENT_URI
        val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

        val cursor: Cursor? = try {
            context.contentResolver.query(uri, projection, null, null, sortOrder)
        } catch (e: SecurityException) {
            e.printStackTrace()
            return@withContext emptyList() // Handle permission issues
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList() // Handle other exceptions
        }

        cursor?.use {
            while (it.moveToNext()) {
                try {
                    val id = it.getLongOrNull(CalendarContract.Events._ID) ?: continue
                    val title = it.getStringOrNull(CalendarContract.Events.TITLE) ?: "Untitled"
                    val description = it.getStringOrNull(CalendarContract.Events.DESCRIPTION) ?: ""
                    val startTime = it.getLongOrNull(CalendarContract.Events.DTSTART) ?: 0L
                    val endTime = it.getLongOrNull(CalendarContract.Events.DTEND) ?: 0L
                    val calendarId = it.getLongOrNull(CalendarContract.Events.CALENDAR_ID) ?: -1L
                    val location = it.getStringOrNull(CalendarContract.Events.EVENT_LOCATION) ?: ""

                    // Map RRule to RepeatOption Enum
                    val rrule = it.getStringOrNull(CalendarContract.Events.RRULE) ?: ""
                    val repeatOption = RepeatOptionConverter.parseRepeatRule(rrule)
                    // Fetch reminder time separately
                    val reminderMinutesBefore = getReminderMinutes(context, id)
                    val alertOffset: AlertOffset = AlertOffsetConverter.parseAlertOffset(reminderMinutesBefore)

                    //val customAlertOffset = (reminderMinutesBefore * 60 * 1000L).takeIf { alertOffset == AlertOffset.BEFORE_CUSTOM_TIME}
                    val customAlertOffset = if( alertOffset == AlertOffset.BEFORE_CUSTOM_TIME ) AlertOffsetConverter.getCustomTime() else null

                    if(startTime > endTime){
                        // Todo: if startTime is greater than endTime so skip that event - Log.i(BASE_TAG, "getAllCursorEvents: $endTime", )
                        continue
                    }

                    events.add(
                        CursorEvent(
                            id = id.toString(),
                            title = title,
                            description = description,
                            startTime = startTime,
                            endTime = endTime,
                            calendarId = calendarId,
                            location = location,
                            repeatOption = repeatOption,
                            alertOffset = alertOffset,
                            customAlertOffset = customAlertOffset,
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return@withContext events
    }

/**
 * Fetches reminder minutes for a given event ID.
 */
private fun getReminderMinutes(context: Context, eventId: Long): Int {
    var reminderMinutesBefore = -1 // Default no reminder
    try {
        context.contentResolver.query(
            CalendarContract.Reminders.CONTENT_URI,
            arrayOf(CalendarContract.Reminders.MINUTES),
            "${CalendarContract.Reminders.EVENT_ID} = ?",
            arrayOf(eventId.toString()),
            null
        )?.use { rCursor ->
            if (rCursor.moveToFirst()) {
                reminderMinutesBefore = rCursor.getInt(rCursor.getColumnIndexOrThrow(CalendarContract.Reminders.MINUTES))
            }
        }
    } catch (e: Exception) {
        Log.e(BASE_TAG, "Error fetching reminder for event $eventId: ${e.message}", e)
    }
    return reminderMinutesBefore
}
/**
 * Safely retrieves a string value from a Cursor.
 */
private fun Cursor.getStringOrNull(columnName: String): String? =
    getColumnIndex(columnName).takeIf { it >= 0 }?.let { getString(it) }

/**
 * Safely retrieves a long value from a Cursor.
 */
private fun Cursor.getLongOrNull(columnName: String): Long? =
    getColumnIndex(columnName).takeIf { it >= 0 }?.let { getLong(it) }

class CalendarContentObserver(
    private val handler: Handler,
    private val onChangeCallback: () -> Unit
) : ContentObserver(handler) {

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        onChangeCallback()
    }
}


suspend fun getUserCustomEvents(context: Context): List<CursorEvent> =
    withContext(Dispatchers.IO) {
        val events = mutableListOf<CursorEvent>()

        // Step 1: Get all calendar IDs
        val calendarIds = getCalendarIds(context)

        if (calendarIds.isEmpty()) {
            return@withContext events
        }

        // Step 2: Dynamically construct IN clause and selectionArgs
        val inClause = calendarIds.joinToString(",") { "?" } // Generates "?, ?, ?"
        //val selection = "${CalendarContract.Events.CALENDAR_ID} IN ($inClause)"
        //val selectionArgs = calendarIds.map { it.toString() }.toTypedArray()
        val selection = """
            ${CalendarContract.Events.CALENDAR_ID} IN ($inClause)
            AND ${CalendarContract.Events.DESCRIPTION} NOT LIKE ?  -- Exclude holidays or predefined events
        """
        val selectionArgs = calendarIds.map { it.toString() }.toMutableList().apply {
            add("%Holiday%")  // Exclude events with 'Holiday' in description
        }.toTypedArray()

        // Step 3: Define projection and query events
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.RRULE,   // Recurrence rule
            CalendarContract.Events.RDATE    // Additional recurrence dates
        )

        val sortOrder = "${CalendarContract.Events.DTSTART} ASC"
        val cursor = context.contentResolver.query(CalendarContract.Events.CONTENT_URI, projection, selection, selectionArgs, sortOrder)

        cursor?.use {
            while (it.moveToNext()) {
                try {
                    val id = it.getLongOrNull(CalendarContract.Events._ID) ?: continue
                    val title = it.getStringOrNull(CalendarContract.Events.TITLE) ?: "Untitled"
                    val description = it.getStringOrNull(CalendarContract.Events.DESCRIPTION) ?: ""
                    val startTime = it.getLongOrNull(CalendarContract.Events.DTSTART) ?: 0L
                    val endTime = it.getLongOrNull(CalendarContract.Events.DTEND) ?: 0L
                    val calendarId = it.getLongOrNull(CalendarContract.Events.CALENDAR_ID) ?: -1L
                    val location = it.getStringOrNull(CalendarContract.Events.EVENT_LOCATION) ?: ""

                    // Map RRule to RepeatOption Enum
                    val rrule = it.getStringOrNull(CalendarContract.Events.RRULE) ?: ""
                    val repeatOption = RepeatOptionConverter.parseRepeatRule(rrule)
                    // Fetch reminder time separately
                    val reminderMinutesBefore = getReminderMinutes(context, id)
                    val alertOffset: AlertOffset = AlertOffsetConverter.parseAlertOffset(reminderMinutesBefore)

                    //val customAlertOffset = (reminderMinutesBefore * 60 * 1000L).takeIf { alertOffset == AlertOffset.BEFORE_CUSTOM_TIME}
                    val customAlertOffset = if( alertOffset == AlertOffset.BEFORE_CUSTOM_TIME ) AlertOffsetConverter.getCustomTime() else null

                    if(startTime > endTime){
                        // Todo: if startTime is greater than endTime so skip that event - Log.i(BASE_TAG, "getAllCursorEvents: $endTime", )
                        continue
                    }

                    events.add(
                        CursorEvent(
                            id = id.toString(),
                            title = title,
                            description = description,
                            startTime = startTime,
                            endTime = endTime,
                            calendarId = calendarId,
                            location = location,
                            repeatOption = repeatOption,
                            alertOffset = alertOffset,
                            customAlertOffset = customAlertOffset,
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return@withContext events
    }


fun getCalendarIds(context: Context): List<Long> {
    val calendarIds = mutableListOf<Long>()

    val projection = arrayOf(
        CalendarContract.Calendars._ID, // Unique calendar ID
        CalendarContract.Calendars.NAME, // Calendar display name
        CalendarContract.Calendars.ACCOUNT_NAME // Account name (e.g., Google, Device)
    )

    val cursor = context.contentResolver.query(
        CalendarContract.Calendars.CONTENT_URI,
        projection,
        null, // No filter to get all calendars
        null,
        null
    )

    cursor?.use {
        while (it.moveToNext()) {
            val id = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
            val name = it.getString(it.getColumnIndexOrThrow(CalendarContract.Calendars.NAME))
            val accountName = it.getString(it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME))

            // Store calendar information
            calendarIds.add(id)
        }
    }

    return calendarIds
}


fun List<CursorEvent>.toEventList(): List<Event> {
    return this.map { item ->
        val startDate = DateUtil.longToString(item.startTime)
        val endDate = DateUtil.longToString(item.endTime)

        val date: Triple<String, String, String> = DateUtil.epochToDateTriple(item.startTime)

        val startTime = item.startTime
        val endTime = item.endTime.takeIf { it != 0L } ?: DateUtil.stringToLong(
            endDate,
            DateUtil.DATE_FORMAT_yyyy_MM_dd
        )

        Event(
            id = item.id,
            title = item.title,
            description = item.description.orEmpty(),
            startDate = startDate,
            endDate = endDate,
            year = date.first,
            month = date.second,
            date = date.third,
            startTime = startTime,
            endTime = endTime,
            isHoliday = false,
            sourceType = SourceType.CURSOR,
            repeatOption = item.repeatOption,
            alertOffset = item.alertOffset,
            customAlertOffset = item.customAlertOffset,
            triggerTime = startTime,
        )
    }
}

