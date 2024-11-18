package com.hardik.calendarapp.utillities

import android.os.Build
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

object DateUtil {

    // Define common date formats
    const val TIME_FORMAT = "h:mm a"
    const val DATE_FORMAT = "yyyy-MM-dd"
    const val DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm"
    const val DATE_TIME_FORMAT_1 = "yyyy-MM-dd'T'HH:mm:ss'Z'"

    /** Function to format Date object to String*/
    /** Get SimpleDateFormat for a given pattern */
    private fun getDateFormat(pattern: String): SimpleDateFormat = SimpleDateFormat(pattern, Locale.getDefault())

    /** Format Date -> String */
    fun dateToString(date: Date, pattern: String = DATE_FORMAT): String {
        val format = getDateFormat(pattern)
        return format.format(date)
    }

    /** Convert Date -> Long (Timestamp) */
    fun dateToLong(date: Date): Long = date.time

    /** Parse String -> Date */
    fun stringToDate(dateString: String, pattern: String = DATE_FORMAT): Date? {
        val format = getDateFormat(pattern)
        return try {
            format.parse(dateString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Convert String -> Long (Timestamp) */
    fun stringToLong(dateString: String, pattern: String = DATE_FORMAT): Long {
        return stringToDate(dateString, pattern)?.time ?: 0L
    }

    /** Convert Long (Timestamp) -> Date */
    fun longToDate(timestamp: Long): Date = Date(timestamp)

    /** Convert Long (Timestamp) -> String */
    fun longToString(timestamp: Long, pattern: String = DATE_FORMAT): String {
        val date = longToDate(timestamp)
        return dateToString(date, pattern)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun calculateTimePeriod(startTimeEpoch: Long, endTimeEpoch: Long): Map<String, String> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

        val startInstant = Instant.ofEpochMilli(startTimeEpoch)
        val endInstant = Instant.ofEpochMilli(endTimeEpoch)

        // Convert Instant to LocalDateTime
        val startDateTime = LocalDateTime.ofInstant(startInstant, ZoneId.systemDefault())
        val endDateTime = LocalDateTime.ofInstant(endInstant, ZoneId.systemDefault())

        // Calculate the difference using Period for years, months, and days
        val period = Period.between(startDateTime.toLocalDate(), endDateTime.toLocalDate())

        // Calculate the difference in seconds
        val duration = Duration.between(startDateTime, endDateTime)

        // Extract hours, minutes, and seconds from the duration
        val totalSeconds = duration.seconds
        val hours = totalSeconds / 3600 % 24
        val minutes = totalSeconds / 60 % 60
        val seconds = totalSeconds % 60

        // Format the start and end time to the required format
        val startFormatted = startDateTime.format(formatter)
        val endFormatted = endDateTime.format(formatter)

        // region Times gaps
        // Create the "total_time_gaps" formatted string
        //Todo:val totalTimeGaps = String.format("%02d Year, %02d Month, %02d Days, %02d:%02d:%02d hours", period.years, period.months, period.days, hours, minutes, seconds)
            // or
        // Build the "total_time_gaps" string dynamically
        val timeGaps = mutableListOf<String>()
        if (period.years > 0) timeGaps.add(String.format("%02d Year", period.years))
        if (period.months > 0) timeGaps.add(String.format("%02d Month", period.months))
        if (period.days > 0) timeGaps.add(String.format("%02d Days", period.days))
        if (hours > 0 || minutes > 0 || seconds > 0) {
            timeGaps.add(String.format("%02d:%02d:%02d hours", hours, minutes, seconds))
        }
        // If all components are zero, return "NA time duration"
        val totalTimeGaps = if (timeGaps.isEmpty()) "NA time duration" else timeGaps.joinToString(", ")
        //endregion

        // Return the results as a map
        return mapOf(
            "start_time" to startFormatted,
            "end_time" to endFormatted,
            "duration_hours" to (duration.toHours()).toString(),
            "duration_minutes" to (duration.toMinutes()).toString(),
            "years" to period.years.toString(),
            "months" to period.months.toString(),
            "days" to period.days.toString(),
            "hours" to hours.toString(),
            "minutes" to minutes.toString(),
            "seconds" to seconds.toString(),
            "total_time_gaps" to totalTimeGaps
        )
    }
}