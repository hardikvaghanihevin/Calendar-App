package com.hardik.calendarapp.presentation.ui.calendar_month_1

import java.util.Calendar

class MonthCycleManager {
    val months = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    var currentMonthIndex = Calendar.getInstance().get(Calendar.MONTH)
        private set // Prevent direct modification
    var year = Calendar.getInstance().get(Calendar.YEAR)

    fun updateMonthIndex(index: Int) {
        currentMonthIndex = index
    }

    fun nextMonth() {
        currentMonthIndex++
        if (currentMonthIndex == 12) {
            currentMonthIndex = 0
            year++ // Increment year after one full cycle forward
        }
        displayState()
    }

    fun previousMonth() {
        currentMonthIndex--
        if (currentMonthIndex < 0) {
            currentMonthIndex = 11
            year-- // Decrement year after one full cycle backward
        }
        displayState()
    }

    fun displayState() {
        // Optional logging for debugging
        println("Current Month: ${months[currentMonthIndex]}, Year: $year")
    }
}
