package com.hardik.calendarapp.utillities

import android.content.Context
import android.provider.CalendarContract
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
    val timeZone: String = TimeZone.getDefault().id
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