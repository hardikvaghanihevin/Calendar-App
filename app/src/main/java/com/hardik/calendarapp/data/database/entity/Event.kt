package com.hardik.calendarapp.data.database.entity

import android.content.Context
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hardik.calendarapp.R
import kotlinx.parcelize.Parcelize

@Entity(tableName = "events",
    //indices = [Index(value = ["eventId"], unique = false)] // Enforce uniqueness on eventId //304 | 268
)
@Parcelize
data class Event(
    @PrimaryKey(autoGenerate = false)
    val id: String = "", // Primary key
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
    val alertOffset: AlertOffset = AlertOffset.AT_TIME_OF_EVENT,
    val customAlertOffset: Long? = null,
    val triggerTime: Long = 0L,
) : Parcelable

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


enum class AlertOffset(var value:Long?){
    NONE(-1),
    AT_TIME_OF_EVENT(-1),
    BEFORE_5_MINUTES(-1),
    BEFORE_10_MINUTES(-1),
    BEFORE_15_MINUTES(-1),
    BEFORE_30_MINUTES(-1),
    BEFORE_1_HOUR(-1),
    BEFORE_1_DAY(-1),
    BEFORE_CUSTOM_TIME(-1),
}
object AlertOffsetConverter {

    // Convert enum to display string using string resources
    fun toDisplayString(context: Context, alertOffset: AlertOffset): String {
        return when (alertOffset) {
            AlertOffset.NONE -> context.getString(R.string.none)
            AlertOffset.AT_TIME_OF_EVENT -> context.getString(R.string.at_time)
            AlertOffset.BEFORE_5_MINUTES -> context.getString(R.string.before_5_minutes)
            AlertOffset.BEFORE_10_MINUTES -> context.getString(R.string.before_10_minutes)
            AlertOffset.BEFORE_15_MINUTES -> context.getString(R.string.before_15_minutes)
            AlertOffset.BEFORE_30_MINUTES -> context.getString(R.string.before_30_minutes)
            AlertOffset.BEFORE_1_HOUR -> context.getString(R.string.before_1_hour)
            AlertOffset.BEFORE_1_DAY -> context.getString(R.string.before_1_day)
            AlertOffset.BEFORE_CUSTOM_TIME -> context.getString(R.string.before_custom_time)
        }
    }

    // Convert a display string to enum
    fun fromDisplayString(context: Context, displayString: String): AlertOffset? {
        return when (displayString) {
            context.getString(R.string.none) -> AlertOffset.NONE
            context.getString(R.string.at_time) -> AlertOffset.AT_TIME_OF_EVENT
            context.getString(R.string.before_5_minutes) -> AlertOffset.BEFORE_5_MINUTES
            context.getString(R.string.before_10_minutes) -> AlertOffset.BEFORE_10_MINUTES
            context.getString(R.string.before_15_minutes) -> AlertOffset.BEFORE_15_MINUTES
            context.getString(R.string.before_30_minutes) -> AlertOffset.BEFORE_30_MINUTES
            context.getString(R.string.before_1_hour) -> AlertOffset.BEFORE_1_HOUR
            context.getString(R.string.before_1_day) -> AlertOffset.BEFORE_1_DAY
            context.getString(R.string.before_custom_time) -> AlertOffset.BEFORE_CUSTOM_TIME
            else -> null // Handle invalid strings gracefully
        }
    }

    // Convert AlertOffset enum to its value in milliseconds
    fun toMilliseconds(alertOffset: AlertOffset): Long? {
        return when (alertOffset) {
            AlertOffset.NONE -> null
            AlertOffset.AT_TIME_OF_EVENT -> AT_TIME
            AlertOffset.BEFORE_5_MINUTES -> MINUTES_5
            AlertOffset.BEFORE_10_MINUTES -> MINUTES_10
            AlertOffset.BEFORE_15_MINUTES -> MINUTES_15
            AlertOffset.BEFORE_30_MINUTES -> MINUTES_30
            AlertOffset.BEFORE_1_HOUR -> HOUR_1
            AlertOffset.BEFORE_1_DAY -> DAY_1
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
    private const val DAY_1 = 24 * 60 * 60 * 1000L
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