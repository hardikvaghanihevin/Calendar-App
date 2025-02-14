package com.hardik.calendarapp.utillities

import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import android.util.Log
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.data.database.entity.AlertOffset
import com.hardik.calendarapp.data.database.entity.AlertOffsetConverter
import com.hardik.calendarapp.data.database.entity.RepeatOption
import com.hardik.calendarapp.data.database.entity.RepeatOptionConverter
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
    val alertOffset: AlertOffset = AlertOffset.AT_TIME_OF_EVENT
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
                    val alertOffset = AlertOffsetConverter.parseAlertOffset(reminderMinutesBefore)

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


