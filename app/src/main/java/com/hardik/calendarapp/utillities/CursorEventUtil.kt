package com.hardik.calendarapp.utillities

import android.content.ContentUris
import android.content.ContentValues
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
import com.hardik.calendarapp.utillities.DateUtil.calculateEndTimeForCursor
import com.hardik.calendarapp.utillities.DateUtil.formatDurationForCursor
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
    val duration: Long,
    val isAllDay: Boolean = false,
    val timeZone: String = TimeZone.getDefault().id,
    val repeatOption: RepeatOption = RepeatOption.NEVER,
    val alertOffset: AlertOffset = AlertOffset.AT_TIME_OF_EVENT,
    val customAlertOffset: Long? = null,
)

class CalendarContentObserver(
    private val handler: Handler,
    private val onChangeCallback: () -> Unit
) : ContentObserver(handler) {

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        onChangeCallback()
    }
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

private fun Cursor.getIntOrNull(columnName: String): Int? =
    getColumnIndex(columnName).takeIf { it >= 0 }?.let { getInt(it) }


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
        val selection1 = """
            ${CalendarContract.Events.CALENDAR_ID} IN ($inClause)
            AND ${CalendarContract.Events.DESCRIPTION} NOT LIKE ?  -- Exclude holidays or predefined events
        """
        val selection = "${CalendarContract.Events.CALENDAR_ID} IN ($inClause) AND ${CalendarContract.Events.DESCRIPTION} NOT LIKE ?"

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
            CalendarContract.Events.DURATION,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.RRULE,   // Recurrence rule
            CalendarContract.Events.RDATE    // Additional recurrence dates
        )

        val sortOrder = "${CalendarContract.Events.DTSTART} ASC"
        val cursor = context.contentResolver.query(CalendarContract.Events.CONTENT_URI, projection, selection, selectionArgs, sortOrder)

        cursor?.use {
            while (it.moveToNext()) {
                Log.d(BASE_TAG, "Row: ${it.getStringOrNull(CalendarContract.Events.TITLE)}")
                try {
                    val id = it.getLongOrNull(CalendarContract.Events._ID) ?: continue
                    val title = it.getStringOrNull(CalendarContract.Events.TITLE) ?: "Untitled"
                    val description = it.getStringOrNull(CalendarContract.Events.DESCRIPTION)
                    val startTime = it.getLongOrNull(CalendarContract.Events.DTSTART) ?: 0L
                    var endTime = it.getLongOrNull(CalendarContract.Events.DTEND) ?: 0L
                    val dur = it.getStringOrNull(CalendarContract.Events.DURATION)
                    val isAllDay = it.getIntOrNull(CalendarContract.Events.ALL_DAY) == 1

                    //val isAllDay = it.getInt(it.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)) == 1

                    val calendarId = it.getLongOrNull(CalendarContract.Events.CALENDAR_ID) ?: -1L
                    val location = it.getStringOrNull(CalendarContract.Events.EVENT_LOCATION)

                    if (endTime == 0L && dur != null) {
                        endTime = calculateEndTimeForCursor(startTime, dur)
                    }

                    // Map RRule to RepeatOption Enum
                    val rrule = it.getStringOrNull(CalendarContract.Events.RRULE) //TODO: FREQ=YEARLY/MONTHLY/WEEKLY/DAILY;INTERVAL=12;WKST=SU/MO
                    val rdate = it.getStringOrNull(CalendarContract.Events.RDATE)
                    val repeatOption = RepeatOptionConverter.parseRepeatRule(rrule)

                    // Fetch reminder time separately
                    val reminderMinutesBefore = getReminderMinutes(context, id)
                    val alertOffset: AlertOffset = AlertOffsetConverter.parseAlertOffset(reminderMinutesBefore)

                    //val customAlertOffset = (reminderMinutesBefore * 60 * 1000L).takeIf { alertOffset == AlertOffset.BEFORE_CUSTOM_TIME}
                    val customAlertOffset = if( alertOffset == AlertOffset.BEFORE_CUSTOM_TIME ) AlertOffsetConverter.getCustomTime() else null

                    if(startTime > endTime){
                        Log.e(BASE_TAG, "getUserCustomEvents: here --------------------invalidacase or return startTime: $startTime - endTime: $endTime", )
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
                            duration = (endTime - startTime),
                            isAllDay = isAllDay,
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
            duration = item.duration,
            isAllDay = item.isAllDay,
            isHoliday = false,
            sourceType = SourceType.CURSOR,
            repeatOption = item.repeatOption,
            alertOffset = item.alertOffset,
            customAlertOffset = item.customAlertOffset,
            triggerTime = startTime,
        )
    }
}

fun Event.toCursorEvent(): CursorEvent {
    return CursorEvent(
        id = this.id,
        calendarId = 0L, // Set a default or retrieve from another source if necessary
        title = this.title,
        description = this.description.takeIf { it.isNotEmpty() },
        startTime = this.startTime,
        endTime = this.endTime,
        duration = (this.endTime - this.startTime),
        isAllDay = this.isAllDay, // Adjust based on actual data if necessary
        repeatOption = this.repeatOption,
        alertOffset = this.alertOffset,
        customAlertOffset = this.customAlertOffset,
        location = null, // Set location if applicable
        timeZone = TimeZone.getDefault().id, // Use appropriate timezone
    )
}

fun deleteCursorEvent(context: Context, eventId: Long): Boolean {
    return try {
        val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val rowsDeleted = context.contentResolver.delete(deleteUri, null, null)
        rowsDeleted > 0
    } catch (e: Exception) {
        Log.e("CursorEventUtil", "Error deleting event with ID: $eventId", e)
        false
    }
}


fun updateCursorEvent(context: Context, event: Event): Boolean {
    return try {
        val cursorEvent = event.toCursorEvent() // Convert event to CursorEvent
        val eventId = cursorEvent.id.toLongOrNull() ?: return false // Ensure ID is valid

        val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val values = ContentValues().apply {
            put(CalendarContract.Events.TITLE, cursorEvent.title)
            put(CalendarContract.Events.DESCRIPTION, cursorEvent.description ?: "")
            put(CalendarContract.Events.DTSTART, cursorEvent.startTime)

            Log.e(BASE_TAG, "updateCursorEvent: ${cursorEvent.startTime} - ${cursorEvent.duration}", )
            /*if (cursorEvent.endTime > 0) {
                // Use DTEND if endTime is defined
                put(CalendarContract.Events.DTEND, cursorEvent.endTime)
            } else {
                // Otherwise, use DURATION
                //put(CalendarContract.Events.DURATION, "P${cursorEvent.duration}S") // ISO 8601 duration format
                put(CalendarContract.Events.DURATION, formatDurationForCursor(cursorEvent.duration))
            }*/
            if (cursorEvent.duration > 0) {
                // Use DURATION if it's defined
                put(CalendarContract.Events.DURATION, formatDurationForCursor(cursorEvent.duration))
            } else if (cursorEvent.endTime > 0) {
                // Otherwise, use DTEND
                put(CalendarContract.Events.DTEND, cursorEvent.endTime)
            }

            put(CalendarContract.Events.ALL_DAY, if (cursorEvent.isAllDay) 1 else 0)
            put(CalendarContract.Events.EVENT_LOCATION, cursorEvent.location ?: "")
            put(CalendarContract.Events.EVENT_TIMEZONE, cursorEvent.timeZone)

            put(CalendarContract.Events.RRULE, RepeatOptionConverter.toRepeatRule(cursorEvent.repeatOption))

            setEventReminder(context, eventId, cursorEvent.alertOffset)
        }

        val rowsUpdated = context.contentResolver.update(updateUri, values, null, null)
        rowsUpdated > 0
    } catch (e: Exception) {
        Log.e(BASE_TAG, "CursorEventUtil:- Error updating event with ID: ${event.id}", e)
        false
    }
}

fun setEventReminder(context: Context, eventId: Long, alertOffset: AlertOffset) {
    try {
        val reminderMinutesBefore = AlertOffsetConverter.toReminderMinutesBefore(alertOffset)

        if (reminderMinutesBefore < 0) {
            Log.d("CursorEventUtil", "No valid reminder to set for event ID: $eventId")
            return
        }

        val contentResolver = context.contentResolver
        val values = ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.MINUTES, reminderMinutesBefore)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
        }

        // Insert the reminder into the CalendarContract.Reminders table
        val uri = contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values)

        if (uri != null) {
            Log.d("CursorEventUtil", "Reminder set successfully for event ID: $eventId")
        } else {
            Log.e("CursorEventUtil", "Failed to set reminder for event ID: $eventId")
        }
    } catch (e: Exception) {
        Log.e("CursorEventUtil", "Error setting reminder for event ID: $eventId", e)
    }
}
