package com.hardik.calendarapp.common

object Constants {

    //https://www.googleapis.com/calendar/v3/calendars/indian__en@holiday.calendar.google.com/events?key=AIzaSyBSSVDB_R5Jh5HQl1LnCeuqOj0u1hprbqE&timeMin=2024-01-01T11:26:55Z&timeMax=2024-12-31T11:26:55Z&timezon=UTC&setSingleEvents=tru
    const val BASE_URL = "https://www.googleapis.com/calendar/v3/" //todo: https://dummyjson.com/users?limit=10&skip=10
    const val apiKey = "AIzaSyCZo6O1q9CwUmWOje5XxzHcTNWSykwdCdU"
    const val timeMin = "2023-01-01T11:26:55Z"
    const val timeMax = "2026-12-31T11:26:55Z"
    const val BASE_TAG = "A_"
    const val KEY_YEAR = "key_year"
    const val KEY_MONTH = "key_month"
    const val KEY_EVENT = "key_event"

    const val EVENT_INSERT_SUCCESSFULLY = "Event inserted successfully"
}