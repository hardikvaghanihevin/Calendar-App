package com.hardik.calendarapp.domain.model

import com.hardik.calendarapp.data.database.entity.Event

data class CalendarDayModel (
    val day:Int,
    val date:String,
    val state:Int,
    var isSelected: Boolean = false,
    var eventIndicator: Boolean = false,
    var isHoliday: Boolean = false,
    var event: Event = Event(title = "", startTime = 0L, endTime = 0L, startDate = "", endDate = "")
)