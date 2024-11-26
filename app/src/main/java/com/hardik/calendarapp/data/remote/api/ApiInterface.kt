package com.hardik.calendarapp.data.remote.api

import com.hardik.calendarapp.data.remote.dto.HolidayApiDto
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiInterface {

    //indian__en@holiday.calendar.google.com/events?key=AIzaSyBSSVDB_R5Jh5HQl1LnCeuqOj0u1hprbqE&timeMin=2024-01-01T11:26:55Z&timeMax=2024-12-31T11:26:55Z&timezon=UTC&setSingleEvents=tru
    @GET("calendars/indian__en@holiday.calendar.google.com/events")
    suspend fun getCalendar(
        @Query("key") apiKey: String,
        @Query("timeMin") timeMin: String,
        @Query("timeMax") timeMax: String,
        @Query("timezone") timezone: String = "UTC",
        @Query("setSingleEvents") setSingleEvents: Boolean = true
    ): HolidayApiDto

    /**
     * Without suspend keyword,it is necessary to use Call<Post>,
     * With suspend keyword, It isn't necessary to use Call<Post>, You have two option Call<Post> or Post.
     * */
}