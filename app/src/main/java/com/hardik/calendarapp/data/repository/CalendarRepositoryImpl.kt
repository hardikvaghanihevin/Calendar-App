package com.hardik.calendarapp.data.repository

import com.hardik.calendarapp.common.Constants.apiKey
import com.hardik.calendarapp.common.Constants.timeMax
import com.hardik.calendarapp.common.Constants.timeMin
import com.hardik.calendarapp.data.remote.api.ApiInterface
import com.hardik.calendarapp.data.remote.dto.CalendarDto
import com.hardik.calendarapp.domain.repository.CalendarRepository
import javax.inject.Inject

class CalendarRepositoryImpl@Inject constructor(private val apiInterface: ApiInterface) : CalendarRepository {


    override suspend fun getCalendar(): CalendarDto {
        return apiInterface.getCalendar(apiKey = apiKey, timeMin = timeMin, timeMax = timeMax)
    }

}