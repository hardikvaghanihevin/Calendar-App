package com.hardik.calendarapp.domain.repository

interface CalendarRepository {
    fun registerContentObserver()
    fun unregisterContentObserver()
    fun setListener(listener: CalendarEventListener)
}

interface CalendarEventListener {
    fun onCalendarEventsChanged()
}
