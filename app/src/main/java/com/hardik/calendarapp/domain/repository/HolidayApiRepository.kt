package com.hardik.calendarapp.domain.repository

import com.hardik.calendarapp.data.remote.dto.HolidayApiDto
import retrofit2.Call

interface HolidayApiRepository {
    fun getHolidayEvents(countryCode: String, languageCode: String ): HolidayApiDto?
}