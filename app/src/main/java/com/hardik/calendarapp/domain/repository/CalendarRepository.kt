package com.hardik.calendarapp.domain.repository

import android.content.Context
import com.hardik.calendarapp.data.remote.dto.CalendarDto

interface CalendarRepository {
    suspend fun getCalendar(): CalendarDto
}