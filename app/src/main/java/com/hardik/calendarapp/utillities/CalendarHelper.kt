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