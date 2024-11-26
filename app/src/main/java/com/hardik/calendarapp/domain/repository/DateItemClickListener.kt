package com.hardik.calendarapp.domain.repository

import com.hardik.calendarapp.domain.model.CalendarDayModel

interface DateItemClickListener {
    fun onDateClick(position: Int, calendarDayModel: CalendarDayModel)
}