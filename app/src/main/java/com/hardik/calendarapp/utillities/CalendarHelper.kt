package com.hardik.calendarapp.utillities

import android.content.Context
import com.hardik.calendarapp.R
import org.joda.time.DateTime
import java.util.Calendar
import java.util.Date

fun getMonth(month: Int, context: Context): String? {
    when (month) {
        1 -> return context.getString(R.string.jan)
        2 -> return context.getString(R.string.fab)
        3 -> return context.getString(R.string.mar)
        4 -> return context.getString(R.string.apr)
        5 -> return context.getString(R.string.may)
        6 -> return context.getString(R.string.jun)
        7 -> return context.getString(R.string.jul)
        8 -> return context.getString(R.string.aug)
        9 -> return context.getString(R.string.sep)
        10 -> return context.getString(R.string.oct)
        11 -> return context.getString(R.string.nov)
        12 -> return context.getString(R.string.dec)
    }
    return null
}

fun createDate(day: Int, monthOfYear: Int, year: Int): DateTime? {
    return DateTime().withYear(year).withMonthOfYear(monthOfYear).withDayOfMonth(day)
        .withTime(0, 0, 0, 0)
}

fun isItToday(date: DateTime): Int {
    val dtToCompare = date.withTime(0, 0, 0, 0)
    val dtToday = DateTime(Date()).withTime(0, 0, 0, 0)
    return if (dtToCompare.isAfter(dtToday)) {
        0
    } else if (dtToCompare.equals(dtToday)) {
        1
    } else {
        2
    }
}

/**
 * Creates a map of years with corresponding months and their days.
 *
 * This function generates a map where the keys are years and the values are maps of months,
 * with each month containing a list of days. The days in each month are determined based on
 * whether the months are zero-based (0-11) or one-based (1-12).
 *
 * @param startYear The start year (Int) of the range.
 * @param endYear The end year (Int) of the range.
 * @param isZeroBased A boolean flag to indicate if months should be zero-based (0-11) or one-based (1-12).
 *
 * @return A map of years to months, with each month containing a list of days.
 */
fun createYearData(startYear: Int, endYear: Int, isZeroBased: Boolean): Map<Int, Map<Int, List<Int>>> {
    val yearMap = mutableMapOf<Int, Map<Int, List<Int>>>()

    for (year in startYear..endYear) {
        val monthsRange = if (isZeroBased) 0..11 else 1..12
        val monthsMap = monthsRange.associateWith { month ->
            (1..getDaysInMonth(year, if (isZeroBased) month + 1 else month)).toList()
        }
        yearMap[year] = monthsMap // Set year as key and monthsMap as value
    }

    return yearMap
}

/**
 * Returns the number of days in a given month for a specific year.
 *
 * This function calculates the number of days in a given month, taking into account leap years
 * for February.
 *
 * @param year The year (Int) for which the number of days is being calculated.
 * @param month The month (Int) for which the number of days is being calculated (1-12).
 *
 * @return The number of days in the specified month.
 * @throws IllegalArgumentException If the month is invalid (less than 1 or greater than 12).
 */
fun getDaysInMonth(year: Int, month: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> throw IllegalArgumentException("Invalid month: $month")
    }
}

/**
 * Checks if a given year is a leap year.
 *
 * A year is a leap year if it is divisible by 4, but not divisible by 100 unless also divisible by 400.
 *
 * @param year The year (Int) to check.
 *
 * @return True if the year is a leap year, false otherwise.
 */
fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}

/**
 * Finds the index of a pair (year, month) in a list of pairs where the year and month match the given targetYear and targetMonth.
 *
 * @param yearMonthPairList A list of pairs, where each pair consists of a year (Int) and a month (Int).
 * @param targetYear The target year (Int) to search for in the list of pairs.
 * @param targetMonth The target month (Int) to search for in the list of pairs.
 *
 * @return The index of the first matching (year, month) pair in the list. If no match is found, returns -1.
 */
fun findIndexOfYearMonth(yearMonthPairList: List<Pair<Int, Int>>, targetYear: Int, targetMonth: Int): Int {
    return yearMonthPairList.indexOfFirst { (year, month) -> year == targetYear && month == targetMonth }
}

/**
 * Retrieves the key (i.e., the year) from the yearListMap at the given position.
 *
 * @param yearListMap A map where the keys represent years (Int) and the values are maps containing months as keys and lists of integers as values.
 * @param position The index (position) in the list of years (keys) where you want to get the year.
 *
 * @return The year at the specified position in the yearListMap, or null if the position is out of bounds.
 */
fun getYearKeyAtPosition(yearListMap: Map<Int, Map<Int, List<Int>>>, position: Int): Int? {
    return yearListMap.keys.toList().getOrNull(position)
}

/**
 * Finds the position (index) of a given year in the yearListMap.
 *
 * @param yearListMap A map where the keys represent years (Int), and the values are maps containing months as keys and lists of integers as values.
 * @param year The target year (Int) for which the position in the map is to be found.
 *
 * @return The index (position) of the specified year in the map’s keys as a list. Returns null if the year is not found.
 */
fun getPositionFromYear(yearListMap: Map<Int, Map<Int, List<Int>>>, year: Int): Int? {
    return yearListMap.keys.toList().indexOf(year).takeIf { it >= 0 } // Returns -1 if year is not found
}
// Get the current date
val calendar = Calendar.getInstance()
val currentYear = calendar.get(Calendar.YEAR)
val currentMonth = calendar.get(Calendar.MONTH) // Month is 0-based
val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

// Example usage
//val yearData = createYearData(2023, 2025, false) // 1-based month calendar
//println(yearData)



