package com.hardik.calendarapp.domain.repository

import com.hardik.calendarapp.data.remote.dto.HolidayApiDto

interface HolidayApiRepository {
    suspend fun getHolidayEvents(countryCode: String, languageCode: String ): HolidayApiDto
}