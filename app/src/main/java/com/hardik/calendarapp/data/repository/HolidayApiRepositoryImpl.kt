package com.hardik.calendarapp.data.repository

import com.hardik.calendarapp.common.Constants.apiKey
import com.hardik.calendarapp.data.remote.api.ApiInterface
import com.hardik.calendarapp.data.remote.dto.HolidayApiDto
import com.hardik.calendarapp.domain.repository.HolidayApiRepository
import com.hardik.calendarapp.utillities.DateUtil
import java.util.Calendar
import javax.inject.Inject

class HolidayApiRepositoryImpl@Inject constructor(private val apiInterface: ApiInterface) : HolidayApiRepository {

    override suspend fun getHolidayEvents(countryCode: String, languageCode:String): HolidayApiDto {
        val pair: Pair<String, String> = getTimeRange()

        //return apiInterface.getCalendar(apiKey = apiKey, timeMin = timeMin, timeMax = timeMax)
        return apiInterface.getCalendar(apiKey = apiKey,countryCode = countryCode, languageCode = languageCode, timeMin = pair.first, timeMax = pair.second)
    }

    private fun getTimeRange(): Pair<String, String> {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)

        // Calculate min year (3 years ago)
        val minYear = currentYear - 3
        // Set the min date to the first day of the min year at 00:00:00
        calendar.set(minYear, Calendar.JANUARY, 1, 0, 0, 0)
        val minDate = calendar.time

        // Calculate max year (3 years ahead)
        val maxYear = currentYear + 3
        // Set the max date to the last day of the max year at 23:59:59
        calendar.set(maxYear, Calendar.DECEMBER, 31, 23, 59, 59)
        val maxDate = calendar.time

        // SimpleDateFormat to convert Date to the desired format (e.g., "yyyy-MM-dd'T'HH:mm:ss'Z'")
        val minDateString = DateUtil.dateToString(minDate, DateUtil.DATE_TIME_FORMAT_yyyy_MM_dd_T_HH_MM_ss_Z)
        val maxDateString = DateUtil.dateToString(maxDate, DateUtil.DATE_TIME_FORMAT_yyyy_MM_dd_T_HH_MM_ss_Z)

        // Convert dates to string and return as a pair
        return Pair(minDateString, maxDateString)
    }
}