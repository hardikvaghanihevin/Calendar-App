package calendar.schedule.task.todo.event.reminder.data.remote.api

import calendar.schedule.task.todo.event.reminder.data.remote.dto.HolidayApiDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiInterface {

    @GET("calendars/{countryCode}__{languageCode}@holiday.calendar.google.com/events")
    suspend fun getCalendar(
        @Path("countryCode") countryCode: String = "indian",
        @Path("languageCode") languageCode: String = "en",//todo: The @Path parameter appears before any @Query parameters (Otherwise face this error : java.lang.IllegalArgumentException: A @Path parameter must not come after a @Query.).
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