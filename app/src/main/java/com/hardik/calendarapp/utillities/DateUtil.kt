package com.hardik.calendarapp.utillities

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.hardik.calendarapp.common.Constants.BASE_TAG
import org.joda.time.DateTime
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtil {
    private val TAG = BASE_TAG + DateUtil::class.java.simpleName

    // Define common date formats
    const val TIME_FORMAT = "h:mm a"
    const val TIME_FORMAT_1 = "hh:mm a"
    const val DATE_FORMAT = "yyyy-MM-dd"
    const val DATE_FORMAT_1 = "dd MM yyyy"
    const val DATE_FORMAT_2 =  "dd MMM yyyy" // For "03 Dec 2024" format
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

    fun formatDate(epochTime: Long): String {
        val dateFormat = SimpleDateFormat(DATE_FORMAT_1, Locale.getDefault())
        val date = Date(epochTime)
        return dateFormat.format(date)
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


    // Function to get the first and last date of the month in milliseconds (Long)
    fun getFirstAndLastDateOfMonth(date: DateTime): Pair<Long, Long> {
        // First day of the current month at 00:00:00
        val firstDayOfMonth = date.withDayOfMonth(1).withTimeAtStartOfDay()

        // Last day of the current month at 23:59:59.999
        val lastDayOfMonth = date.withDayOfMonth(date.dayOfMonth().maximumValue).withTime(23, 59, 59, 999)

        // Return the first and last dates as Pair<Long> (timestamps)
        return Pair(firstDayOfMonth.millis, lastDayOfMonth.millis)
    }

    /**
     * Get the first and last dates of a specific month and year.
     *
     * @param year The year of the target month.
     * @param month The month (1 for January, 12 for December).
     * @return Pair<Long, Long> where the first is the start timestamp of the month
     *         and the second is the end timestamp of the month.
     */
    fun getFirstAndLastDateOfMonth(year: Int, month: Int): Pair<Long, Long> {
        // Ensure the month is valid (1-12)
        require(month in 1..12) { "Invalid month: $month. Must be between 1 and 12." }

        // Create a DateTime object for the first day of the given month and year
        val firstDayOfMonth = DateTime(year, month, 1, 0, 0, 0)

        // Get the last day of the month
        val lastDayOfMonth = firstDayOfMonth
            .withDayOfMonth(firstDayOfMonth.dayOfMonth().maximumValue)
            .withTime(23, 59, 59, 999)

        // Return the first and last dates as Pair<Long> (timestamps)
        return Pair(firstDayOfMonth.millis, lastDayOfMonth.millis)
    }

    /**
     * Converts a date string to a formatted string with a zero-based month.
     *
     * This extension function takes a date string in the format "yyyy-MM-dd",
     * adjusts the month to be zero-based (0 for January, 11 for December),
     * and removes leading zeros from the day of the month.
     *
     * @receiver A string representing a date in the format "yyyy-MM-dd".
     * @return A string in the format "yyyy-M-d", where the month is zero-based
     *         and leading zeros are removed from the day.
     *
     * Example:
     * ```
     * val date = "2024-12-01"
     * val formattedDate = date.getFormattedDate()
     * println(formattedDate) // Output: "2024-11-1"
     * ```
     *
     * Note:
     * - This function assumes the input string is in a valid "yyyy-MM-dd" format.
     * - If the input string is not correctly formatted, it may throw an exception.
     */
    fun String.getFormattedDate(): String {
        val parts = this.split("-") // Split the date into year, month, and day
        val year = parts[0]
        val month = parts[1].toInt()// - 1 // Convert to zero-based month by subtracting 1
        val day = parts[2].toInt()       // Convert to integer to remove leading zeros
        return "$year-${month-1}-$day"       // Combine into the new format
    }

    /**
     * Merges the date and time represented by two separate epoch times into one combined epoch time.
     *
     * The `dateEpoch` represents the date part, and `timeEpoch` represents the time part. The function
     * combines these to produce a new epoch time that includes both the date and the time.
     *
     * For example:
     * - If `dateEpoch` represents "2024-12-03" at 00:00:00, and
     * - `timeEpoch` represents "12:30:00" on the same day,
     *
     * The result will be the epoch time corresponding to "2024-12-03 12:30:00".
     *
     * @param dateEpoch The epoch time representing the date part (in milliseconds).
     * @param timeEpoch The epoch time representing the time part (in milliseconds).
     * @return The combined epoch time that includes both date and time (in milliseconds).
     */
    fun mergeDateAndTime(dateEpoch: Long, timeEpoch: Long): Long {
        // Get the Calendar instance for the date
        val dateCalendar = Calendar.getInstance().apply {
            timeInMillis = dateEpoch
        }

        // Get the time parts from the timeEpoch
        val timeCalendar = Calendar.getInstance().apply {
            timeInMillis = timeEpoch
        }

        // Set the time of the dateCalendar to the time from timeEpoch
        dateCalendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
        dateCalendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
        dateCalendar.set(Calendar.SECOND, timeCalendar.get(Calendar.SECOND))
        dateCalendar.set(Calendar.MILLISECOND, timeCalendar.get(Calendar.MILLISECOND))

        // Return the merged date and time as epoch
        return dateCalendar.timeInMillis
    }

    /**
     * Separates a combined epoch time into two separate epoch times: one for the date and one for the time.
     *
     * The `mergedEpoch` represents both the date and time. This function splits it into:
     * - The date part (midnight of the same day) and
     * - The time part (the exact time of the `mergedEpoch`).
     *
     * For example:
     * - If `mergedEpoch` represents "2024-12-03 12:30:00",
     *
     * The result will be:
     * - `dateEpoch` as the epoch time for "2024-12-03 00:00:00" (midnight),
     * - `timeEpoch` as the epoch time for "2024-12-03 12:30:00".
     *
     * @param mergedEpoch The combined epoch time (in milliseconds) representing both the date and the time.
     * @return A pair of epoch times:
     *         - The first element is the date part (midnight of the same day),
     *         - The second element is the time part.
     */
    fun separateDateTime(mergedEpoch: Long): Pair<Long, Long> {
        // Get the Calendar instance for the merged epoch
        val calendar = Calendar.getInstance().apply {
            timeInMillis = mergedEpoch
        }

        // Extract date part (without time)
        val dateCalendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, calendar.get(Calendar.YEAR))
            set(Calendar.MONTH, calendar.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0) // Set time to 00:00:00
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dateEpoch = dateCalendar.timeInMillis // Epoch time for the date part

        // Extract time part (without date)
        val timeEpoch = calendar.timeInMillis // Time part will be the full epoch time

        return Pair(dateEpoch, timeEpoch)
    }

    /**
     * Converts an epoch time (in milliseconds) to a `Triple` containing the year, month, and day as strings.
     *
     * @param epochTime The epoch time in milliseconds (e.g., System.currentTimeMillis()).
     * @return A `Triple` where:
     *  - `first`: The year as a string (e.g., "2024").
     *  - `second`: The month as a string (1-based, e.g., "12" for December).
     *  - `third`: The day of the month as a string (e.g., "3").
     *
     * Example:
     * ```
     * val epochTime = 1733164200000L  // Epoch time for 03-Dec-2024
     * val dateTriple = epochToDateTriple(epochTime)
     * println("Year: ${dateTriple.first}, Month: ${dateTriple.second}, Day: ${dateTriple.third}")
     * // Output: Year: 2024, Month: 12, Day: 3
     * ```
     */
    fun epochToDateTriple(epochTime: Long): Triple<String, String, String> {
        val stringDate = longToString(epochTime)
        Log.i(TAG, "epochToDateTriple: $stringDate")
        return stringToDateTriple(stringDate)
    }

    fun stringToDateTriple(stringDate:String): Triple<String, String, String>{
        return stringDate.split("-").let { parts ->
            val year = parts[0]
            val month = (parts[1].toInt() - 1).toString()  // Adjust month (1-based to 0-based)
            val day = parts[2].toInt().toString()  // Get the day as a string
            //Log.e(TAG, "collectState: ${item.start.date} -> $year,$month,$day")

            // Return a Triple with year, month, and day
            Triple(year, month, day)
        }
    }

    /**
    val firstApproachTime = measureExecutionTime {inside your block of code}
    Log.d(TAG, "First approach execution time: ${firstApproachTime / 1_000_000} ms")

    1 second (s) is equal to:
        1,000 milliseconds (ms)
        1,000,000 microseconds (µs)
        1,000,000,000 nanoseconds (ns)

    Log.d(TAG, "${(endTime - startTime)} ns")
    Log.d(TAG, "${(endTime - startTime) / 1_000} µs")
    Log.d(TAG, "${(endTime - startTime) / 1_000_000} ms")
    //Or
    val startTime = System.nanoTime()
    val endTime = System.nanoTime()
    Log.d(TA
    fun measureExecutionTime(block: () -> Unit): Long {
    val startTime = System.nanoTime()
    block()
    val endTime = System.nanoTime()
    return endTime - startTime // Returns time in nanoseconds
    }G, "execution time: ${(endTime - startTime)} ns, ${(endTime - startTime) / 1_000} µs, ${(endTime - startTime) / 1_000_000} ms")

     */

}

//// Convert to epoch time
//            val selectedEpochTime = Calendar.getInstance().apply {
//                set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
//                set(Calendar.MILLISECOND, 0) // Reset milliseconds
//            }.timeInMillis

