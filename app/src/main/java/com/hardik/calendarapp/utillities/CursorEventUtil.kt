package com.hardik.calendarapp.utillities

import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import com.hardik.calendarapp.common.Constants.BASE_TAG
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
    val alertOffset:AlertOffset = AlertOffset.AT_TIME
)

/*fun CursorEvent.toEvent(): Event {
    val startDate = DateFormat.getDateInstance().format(Date(this.startTime))
    val endDate = DateFormat.getDateInstance().format(Date(this.endTime))
    val calendar = Calendar.getInstance().apply { timeInMillis = this@toEvent.startTime }
    val year = calendar.get(Calendar.YEAR).toString()
    val month = calendar.get(Calendar.MONTH).toString() // 0-based for January
    val date = calendar.get(Calendar.DAY_OF_MONTH).toString()

    return Event(
        id = this.id?.toString() ?: "",
        title = this.title,
        startTime = this.startTime,
        endTime = this.endTime,
        startDate = startDate,
        endDate = endDate,
        year = year,
        month = month,
        date = date,
        isHoliday = false, // Default or derive from logic
        eventType = EventType.NATIONAL_HOLIDAY, // Default or derive from logic
        sourceType = SourceType.CURSOR,
        description = this.description ?: ""
    )
}*/


fun getAllCursorEvents(context: Context): List<CursorEvent> {
    val events = mutableListOf<CursorEvent>()

    val projection = arrayOf(
        CalendarContract.Events._ID,
        CalendarContract.Events.TITLE,
        CalendarContract.Events.DESCRIPTION,
        CalendarContract.Events.DTSTART,
        CalendarContract.Events.DTEND,
        CalendarContract.Events.CALENDAR_ID,
        CalendarContract.Events.EVENT_LOCATION
    )

    val uri = CalendarContract.Events.CONTENT_URI
    val selection = null // You can apply a filter if needed
    val selectionArgs = null
    val sortOrder = "${CalendarContract.Events.DTSTART} ASC" // Sort by start date

    val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)

    cursor?.use {
        while (it.moveToNext()) {
            val id = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events._ID))
            val title = it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.TITLE))
            val description = it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION))
            val startTime = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events.DTSTART))
            val endTime = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events.DTEND))
            val calendarId = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_ID))
            val location = it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION))

            events.add(CursorEvent(id = id, title = title, description = description, startTime = startTime, endTime = endTime, calendarId = calendarId, location = location))
        }
    }

    return events
}

fun getUserCustomEvents(context: Context): List<CursorEvent> {
    Log.i(BASE_TAG, "getUserCustomEvents: ")
    val events = mutableListOf<CursorEvent>()

    // Step 1: Get all calendar IDs
    val calendarIds = getCalendarIds(context)
    Log.i(BASE_TAG, "Calendar IDs: $calendarIds")

    if (calendarIds.isEmpty()) {
        Log.i(BASE_TAG, "No calendars found")
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
    Log.i(BASE_TAG, "Selection Args: ${selectionArgs.joinToString(", ")}")

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

    Log.i(BASE_TAG, "Cursor count: ${cursor?.count}")

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

    Log.i(BASE_TAG, "getUserCustomEvents: Event size = ${events.size}")

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

            // Log or store calendar information
            println("Calendar ID: $id, Name: $name, Account: $accountName")
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

// Example helper method to parse alert offset (you can extend this logic)
fun parseAlertOffset(description: String?): AlertOffset {
    return when {
        description.isNullOrEmpty() -> AlertOffset.NONE
        description.contains("5 minutes", ignoreCase = true) -> AlertOffset.BEFORE_5_MINUTES
        description.contains("10 minutes", ignoreCase = true) -> AlertOffset.BEFORE_10_MINUTES
        description.contains("15 minutes", ignoreCase = true) -> AlertOffset.BEFORE_15_MINUTES
        description.contains("30 minutes", ignoreCase = true) -> AlertOffset.BEFORE_30_MINUTES
        description.contains("1 hour", ignoreCase = true) -> AlertOffset.BEFORE_1_HOUR
        description.contains("12 hours", ignoreCase = true) -> AlertOffset.BEFORE_12_HOURS
        description.contains("1 day", ignoreCase = true) -> AlertOffset.BEFORE_1_DAY
        description.contains("3 days", ignoreCase = true) -> AlertOffset.BEFORE_3_DAYS
        description.contains("5 days", ignoreCase = true) -> AlertOffset.BEFORE_5_DAYS
        description.contains("1 week", ignoreCase = true) -> AlertOffset.BEFORE_1_WEEK
        description.contains("2 weeks", ignoreCase = true) -> AlertOffset.BEFORE_2_WEEKS
        description.contains("1 month", ignoreCase = true) -> AlertOffset.BEFORE_1_MONTH
        description.contains("custom time", ignoreCase = true) -> AlertOffset.BEFORE_CUSTOM_TIME
        else -> AlertOffset.AT_TIME // Default to AT_TIME if no matches found
    }
}
