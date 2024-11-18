package com.hardik.calendarapp.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hardik.calendarapp.domain.model.CalendarDetail

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startTime: Long, // Timestamp
    val endTime: Long,   // Timestamp
    val startDate: String,
    val endDate: String,
    val isHoliday: Boolean = false, // To differentiate holiday events
    val description: String = ""
)

fun Event.toCalendarDetailItem(): CalendarDetail.Item {
    return CalendarDetail.Item(
        created = "", // Provide value if needed
        creator = CalendarDetail.Item.Creator(
            displayName = "",
            email = "",
            self = false
        ),
        description = this.description,
        end = CalendarDetail.Item.End(date = endDate),
        etag = "",
        eventType = if (this.isHoliday) "holiday" else "regular",
        htmlLink = "",
        iCalUID = "",
        id = this.id.toString(),
        kind = "calendar#event",
        organizer = CalendarDetail.Item.Organizer(
            displayName = "",
            email = "",
            self = false
        ),
        sequence = 0,
        start = CalendarDetail.Item.Start(date = startDate),
        status = "confirmed",
        summary = this.title,
        transparency = "opaque",
        updated = "",
        visibility = "default"
    )
}
