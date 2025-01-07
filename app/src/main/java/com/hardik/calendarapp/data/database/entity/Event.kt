package com.hardik.calendarapp.data.database.entity

import android.content.Context
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hardik.calendarapp.R
import com.hardik.calendarapp.domain.model.HolidayApiDetail
import kotlinx.parcelize.Parcelize

@Entity(tableName = "events",
    indices = [Index(value = ["eventId"], unique = false)] // Enforce uniqueness on eventId //304 | 268
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
    val repeatOption: RepeatOption = RepeatOption.NEVER,//ONCE
    val alertOffset: AlertOffset = AlertOffset.AT_TIME,
    val customAlertOffset: Long? = null,
    val triggerTime: Long = 0L,
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
    NEVER,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}
object RepeatOptionConverter {

    // Convert enum to display string using string resources
    fun toDisplayString(context: Context, repeatOption: RepeatOption): String {
        return when (repeatOption) {
            RepeatOption.NEVER -> context.getString(R.string.never)
            RepeatOption.DAILY -> context.getString(R.string.every_day)
            RepeatOption.WEEKLY -> context.getString(R.string.every_week)
            RepeatOption.MONTHLY -> context.getString(R.string.every_month)
            RepeatOption.YEARLY -> context.getString(R.string.every_year)
        }
    }

    // Convert a display string to enum
    fun fromDisplayString(context: Context, displayString: String): RepeatOption? {
        return when (displayString) {
            context.getString(R.string.never) -> RepeatOption.NEVER
            context.getString(R.string.every_day) -> RepeatOption.DAILY
            context.getString(R.string.every_week) -> RepeatOption.WEEKLY
            context.getString(R.string.every_month) -> RepeatOption.MONTHLY
            context.getString(R.string.every_year) -> RepeatOption.YEARLY
            else -> null // Handle invalid strings gracefully
        }
    }

    // Convert RepeatOption enum to its value in milliseconds
    fun toMilliseconds(repeatOption: RepeatOption): Long? {
        return when (repeatOption) {
            RepeatOption.NEVER -> null // No interval for "NEVER"
            RepeatOption.DAILY -> 24 * 60 * 60 * 1000L // 1 day in milliseconds
            RepeatOption.WEEKLY -> 7 * 24 * 60 * 60 * 1000L // 1 week in milliseconds
            RepeatOption.MONTHLY -> 30 * 24 * 60 * 60 * 1000L // 1 month (approximation) in milliseconds
            RepeatOption.YEARLY -> 365 * 24 * 60 * 60 * 1000L // 1 year (approximation) in milliseconds
        }
    }
}


enum class AlertOffset{
    NONE,
    AT_TIME,
    BEFORE_5_MINUTES,
    BEFORE_10_MINUTES,
    BEFORE_15_MINUTES,
    BEFORE_30_MINUTES,
    BEFORE_1_HOUR,
    BEFORE_12_HOURS,
    BEFORE_1_DAY,
    BEFORE_3_DAYS,
    BEFORE_5_DAYS,
    BEFORE_1_WEEK,
    BEFORE_2_WEEKS,
    BEFORE_1_MONTH,
    BEFORE_CUSTOM_TIME,
}
object AlertOffsetConverter {

    // Convert enum to display string using string resources
    fun toDisplayString(context: Context, alertOffset: AlertOffset): String {
        return when (alertOffset) {
            AlertOffset.NONE -> context.getString(R.string.none)
            AlertOffset.AT_TIME -> context.getString(R.string.at_time)
            AlertOffset.BEFORE_5_MINUTES -> context.getString(R.string.before_5_minutes)
            AlertOffset.BEFORE_10_MINUTES -> context.getString(R.string.before_10_minutes)
            AlertOffset.BEFORE_15_MINUTES -> context.getString(R.string.before_15_minutes)
            AlertOffset.BEFORE_30_MINUTES -> context.getString(R.string.before_30_minutes)
            AlertOffset.BEFORE_1_HOUR -> context.getString(R.string.before_1_hour)
            AlertOffset.BEFORE_12_HOURS -> context.getString(R.string.before_12_hours)
            AlertOffset.BEFORE_1_DAY -> context.getString(R.string.before_1_day)
            AlertOffset.BEFORE_3_DAYS -> context.getString(R.string.before_3_days)
            AlertOffset.BEFORE_5_DAYS -> context.getString(R.string.before_5_days)
            AlertOffset.BEFORE_1_WEEK -> context.getString(R.string.before_1_week)
            AlertOffset.BEFORE_2_WEEKS -> context.getString(R.string.before_2_weeks)
            AlertOffset.BEFORE_1_MONTH -> context.getString(R.string.before_1_month)
            AlertOffset.BEFORE_CUSTOM_TIME -> context.getString(R.string.before_custom_time)
        }
    }

    // Convert a display string to enum
    fun fromDisplayString(context: Context, displayString: String): AlertOffset? {
        return when (displayString) {
            context.getString(R.string.none) -> AlertOffset.NONE
            context.getString(R.string.at_time) -> AlertOffset.AT_TIME
            context.getString(R.string.before_5_minutes) -> AlertOffset.BEFORE_5_MINUTES
            context.getString(R.string.before_10_minutes) -> AlertOffset.BEFORE_10_MINUTES
            context.getString(R.string.before_15_minutes) -> AlertOffset.BEFORE_15_MINUTES
            context.getString(R.string.before_30_minutes) -> AlertOffset.BEFORE_30_MINUTES
            context.getString(R.string.before_1_hour) -> AlertOffset.BEFORE_1_HOUR
            context.getString(R.string.before_12_hours) -> AlertOffset.BEFORE_12_HOURS
            context.getString(R.string.before_1_day) -> AlertOffset.BEFORE_1_DAY
            context.getString(R.string.before_3_days) -> AlertOffset.BEFORE_3_DAYS
            context.getString(R.string.before_5_days) -> AlertOffset.BEFORE_5_DAYS
            context.getString(R.string.before_1_week) -> AlertOffset.BEFORE_1_WEEK
            context.getString(R.string.before_2_weeks) -> AlertOffset.BEFORE_2_WEEKS
            context.getString(R.string.before_1_month) -> AlertOffset.BEFORE_1_MONTH
            context.getString(R.string.before_custom_time) -> AlertOffset.BEFORE_CUSTOM_TIME
            else -> null // Handle invalid strings gracefully
        }
    }

    // Convert AlertOffset enum to its value in milliseconds
    fun toMilliseconds(alertOffset: AlertOffset): Long? {
        return when (alertOffset) {
            AlertOffset.NONE -> null
            AlertOffset.AT_TIME -> AT_TIME
            AlertOffset.BEFORE_5_MINUTES -> MINUTES_5
            AlertOffset.BEFORE_10_MINUTES -> MINUTES_10
            AlertOffset.BEFORE_15_MINUTES -> MINUTES_15
            AlertOffset.BEFORE_30_MINUTES -> MINUTES_30
            AlertOffset.BEFORE_1_HOUR -> HOUR_1
            AlertOffset.BEFORE_12_HOURS -> HOURS_12
            AlertOffset.BEFORE_1_DAY -> DAY_1
            AlertOffset.BEFORE_3_DAYS -> DAYS_3
            AlertOffset.BEFORE_5_DAYS -> DAYS_5
            AlertOffset.BEFORE_1_WEEK -> WEEK_1
            AlertOffset.BEFORE_2_WEEKS -> WEEKS_2
            AlertOffset.BEFORE_1_MONTH -> MONTH_1
            AlertOffset.BEFORE_CUSTOM_TIME -> CUSTOM_TIME
        }
    }

    // Constants for clarity
    //private const val NONE = null
    private const val AT_TIME = 0L
    private const val MINUTES_5 = 5 * 60 * 1000L
    private const val MINUTES_10 = 10 * 60 * 1000L
    private const val MINUTES_15 = 15 * 60 * 1000L
    private const val MINUTES_30 = 30 * 60 * 1000L
    private const val HOUR_1 = 60 * 60 * 1000L
    private const val HOURS_12 = 12 * 60 * 60 * 1000L
    private const val DAY_1 = 24 * 60 * 60 * 1000L
    private const val DAYS_3 = 3 * 24 * 60 * 60 * 1000L
    private const val DAYS_5 = 5 * 24 * 60 * 60 * 1000L
    private const val WEEK_1 = 7 * 24 * 60 * 60 * 1000L
    private const val WEEKS_2 = 2 * 7 * 24 * 60 * 60 * 1000L
    private const val MONTH_1 = 30L * 24 * 60 * 60 * 1000L // Approximation
    private var CUSTOM_TIME = -1L // Here store custom time (long) get from user

    // Get the current custom time
    fun getCustomTime(): Long = CUSTOM_TIME

    // Update the custom time
    fun setCustomTime(newTime: Long) {
        CUSTOM_TIME = newTime
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


val dummyEvent = Event(
    id = "", // Unique ID
    eventId = 1L, // Example auto-incremented ID
    title = "Dummy Event",
    startTime = 0L, // Current timestamp
    endTime = 0L, // Two hours later
    startDate = "", // Example start date
    endDate = "", // Example end date
    year = "", // Year of the event
    month = "", // April (0-based index)
    date = "", // 13th day of the month
    isHoliday = false, // Not a holiday
    eventType = EventType.PERSONAL, // Example event type
    sourceType = SourceType.LOCAL, // Example source type
    description = "Dummy event",
    repeatOption = RepeatOption.NEVER, // Occurs once
    alertOffset = AlertOffset.AT_TIME, // Alert 10 minutes before
    customAlertOffset = null // No custom alert offset
)