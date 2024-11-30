package com.hardik.calendarapp.utillities

import android.content.Context
import com.hardik.calendarapp.R
import org.joda.time.DateTime
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

fun getDaysInMonth(year: Int, month: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> throw IllegalArgumentException("Invalid month: $month")
    }
}

fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}
fun findIndexOfYearMonth(yearMonthPairList: List<Pair<Int, Int>>, targetYear: Int, targetMonth: Int): Int {
    return yearMonthPairList.indexOfFirst { (year, month) -> year == targetYear && month == targetMonth }
}


// Example usage
//val yearData = createYearData(2023, 2025, false) // 1-based month calendar
//println(yearData)



