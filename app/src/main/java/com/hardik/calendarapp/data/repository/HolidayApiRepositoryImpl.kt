package com.hardik.calendarapp.data.repository

import com.hardik.calendarapp.common.Constants.apiKey
import com.hardik.calendarapp.common.Constants.timeMax
import com.hardik.calendarapp.common.Constants.timeMin
import com.hardik.calendarapp.data.remote.api.ApiInterface
import com.hardik.calendarapp.data.remote.dto.HolidayApiDto
import com.hardik.calendarapp.domain.repository.HolidayApiRepository
import javax.inject.Inject

class HolidayApiRepositoryImpl@Inject constructor(private val apiInterface: ApiInterface) : HolidayApiRepository {

    override suspend fun getHolidayEvents(): HolidayApiDto {
        return apiInterface.getCalendar(apiKey = apiKey, timeMin = timeMin, timeMax = timeMax)
    }

}