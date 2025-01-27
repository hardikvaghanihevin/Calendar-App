package com.hardik.calendarapp.utillities

import android.content.Context
import android.provider.CalendarContract
import com.hardik.calendarapp.data.database.entity.AlertOffset
import com.hardik.calendarapp.data.database.entity.RepeatOption
import java.util.TimeZone

data class CursorEvent(
    val id: Long? = null,
    val calendarId: Long,
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val startTime: Long,
    val endTime: Long,
    val isAllDay: Boolean = false,
    val timeZone: String = TimeZone.getDefault().id,
    val repeatOption:RepeatOption = RepeatOption.NEVER,
    val alertOffset:AlertOffset = AlertOffset.AT_TIME_OF_EVENT
)

fun getUserCustomEvents(context: Context): List<CursorEvent> {
    val events = mutableListOf<CursorEvent>()

    // Step 1: Get all calendar IDs
    val calendarIds = getCalendarIds(context)

    if (calendarIds.isEmpty()) {
        return events
    }

    // Step 2: Dynamically construct IN clause and selectionArgs
    val inClause = calendarIds.joinToString(",") { "?" } // Generates "?, ?, ?"
//    val selection = "${CalendarContract.Events.CALENDAR_ID} IN ($inClause)"
//    val selectionArgs = calendarIds.map { it.toString() }.toTypedArray()
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

    // Step 4: Process query results
    cursor?.use {
        while (it.moveToNext()) {
            val id = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events._ID))
            val title = it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.TITLE))
            val description = it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION))
            val startTime = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events.DTSTART))
            val endTime = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events.DTEND))
            val calendarId = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_ID))
            val location = it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION))
            val rrule = it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.RRULE))
            val rdate = it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.RDATE))

            // Process recurrence rule to set RepeatOption
            val repeatOption = parseRecurrenceRule(rrule)

            events.add(CursorEvent(id = id, title = title, description = description, startTime = startTime, endTime = endTime, calendarId = calendarId, location = location, repeatOption = repeatOption))
        }
    }

    return events
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

// Example helper method to parse recurrence rules (RRULE) into a RepeatOption
fun parseRecurrenceRule(rrule: String?): RepeatOption {
    return when {
        rrule.isNullOrEmpty() -> RepeatOption.NEVER
        rrule.contains("DAILY") -> RepeatOption.DAILY
        rrule.contains("WEEKLY") -> RepeatOption.WEEKLY
        rrule.contains("MONTHLY") -> RepeatOption.MONTHLY
        rrule.contains("YEARLY") -> RepeatOption.YEARLY
        else -> RepeatOption.NEVER
    }
}

