package com.hardik.calendarapp.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hardik.calendarapp.domain.model.HolidayApiDetail

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startTime: Long, // Timestamp
    val endTime: Long,   // Timestamp
    val startDate: String,
    val endDate: String,
    val isHoliday: Boolean = false, // To differentiate holiday events
    val eventType: EventType = EventType.GLOBAL_HOLIDAY,
    val description: String = ""
)

fun Event.toCalendarDetailItem(): HolidayApiDetail.Item {
    return HolidayApiDetail.Item(
        created = "", // Provide value if needed
        creator = HolidayApiDetail.Item.Creator(
            displayName = "",
            email = "",
            self = false
        ),
        description = this.description,
        end = HolidayApiDetail.Item.End(date = endDate),
        etag = "",
        eventType = if (this.isHoliday) "holiday" else "regular",
        htmlLink = "",
        iCalUID = "",
        id = this.id.toString(),
        kind = "calendar#event",
        organizer = HolidayApiDetail.Item.Organizer(
            displayName = "",
            email = "",
            self = false
        ),
        sequence = 0,
        start = HolidayApiDetail.Item.Start(date = startDate),
        status = "confirmed",
        summary = this.title,
        transparency = "opaque",
        updated = "",
        visibility = "default"
    )
}
enum class EventType {
    PERSONAL, GLOBAL_HOLIDAY, NATIONAL_HOLIDAY, CULTURAL_HOLIDAY, WORK_MEETING
}

typealias YearKey = String
typealias MonthKey = String
typealias DayKey = String
typealias EventValue = String // Or replace with Event if you want to store the whole event

fun organizeEvents(events: List<Event>): MutableMap<YearKey, MutableMap<MonthKey, MutableMap<DayKey, EventValue>>> {
    val mapOfEvents = mutableMapOf<YearKey, MutableMap<MonthKey, MutableMap<DayKey, EventValue>>>()

    for (event in events) {
        // Parse the startDate (e.g., "2024-01-01") into year, month, day
        val parts = event.startDate.split("-")
        val year: YearKey = parts[0]
        val month: MonthKey = (parts[1].toInt() - 1).toString() // Convert to zero-based month
        val day: DayKey = parts[2].toInt().toString() // Remove leading zero

        // Initialize maps if not already present
        val yearMap = mapOfEvents.getOrPut(year) { mutableMapOf() }
        val monthMap = yearMap.getOrPut(month) { mutableMapOf() }

        // Use the day as key and store the event's startDate or any required data
        monthMap[day] = event.startDate // Or use a custom value like event.title, event.description, etc.
    }

    return mapOfEvents
}
fun getAllKeysAsList(
    map: MutableMap<YearKey, MutableMap<MonthKey, MutableMap<DayKey, EventValue>>>
): List<String> {
    val keyList = mutableListOf<String>()

    for ((yearKey, monthMap) in map) {
        for ((monthKey, dayMap) in monthMap) {
            for ((dayKey, _) in dayMap) {
                // Combine keys into "yearKey-monthKey-dayKey" format
                keyList.add("$yearKey-$monthKey-$dayKey")
            }
        }
    }

    return keyList
}
