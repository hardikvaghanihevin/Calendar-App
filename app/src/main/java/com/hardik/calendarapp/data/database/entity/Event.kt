package com.hardik.calendarapp.data.database.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hardik.calendarapp.domain.model.HolidayApiDetail
import kotlinx.parcelize.Parcelize

@Entity(tableName = "events",
    indices = [Index(value = ["eventId"], unique = true)] // Enforce uniqueness on eventId
)
@Parcelize
data class Event(
    @PrimaryKey(autoGenerate = false)
    val id: String = "", // Primary key
    var eventId: Long = 0L, // Auto-incremented, unique
    val title: String,
    val startTime: Long, // Timestamp
    val endTime: Long,   // Timestamp
    val startDate: String,
    val endDate: String,
    val year: String,//2024
    val month: String,// 0 to 11 for january to december
    val date: String,
    val isHoliday: Boolean = false, // To differentiate holiday events
    val eventType: EventType = EventType.GLOBAL_HOLIDAY,
    val sourceType: SourceType = SourceType.REMOTE,
    val description: String = "",
    val repeatOption: RepeatOption = RepeatOption.ONCE,
    val alertOffset: Long = 0L,
) : Parcelable

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
enum class SourceType(val value: Int) {
    REMOTE(0), CURSOR(1), LOCAL(2)
}

enum class RepeatOption {
    ONCE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

fun getAlertOffset(option: String): Long {
    return when (option) {
        "At a time" -> 0L
        "Before 10 minutes" -> -10 * 60 * 1000
        "Before 15 minutes" -> -15 * 60 * 1000
        "Before 30 minutes" -> -30 * 60 * 1000
        "Before 1 hour" -> -60 * 60 * 1000
        "Before 12 hours" -> -12 * 60 * 60 * 1000
        "Before 1 day" -> -24 * 60 * 60 * 1000
        else -> 0L // Default to "at a time"
    }
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
