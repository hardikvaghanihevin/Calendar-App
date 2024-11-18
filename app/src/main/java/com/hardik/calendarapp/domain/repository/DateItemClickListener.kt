package com.hardik.calendarapp.domain.repository

import com.hardik.calendarapp.domain.model.CalendarModel

interface DateItemClickListener {
    fun onDateClick(position: Int,calendarModel: CalendarModel)
}