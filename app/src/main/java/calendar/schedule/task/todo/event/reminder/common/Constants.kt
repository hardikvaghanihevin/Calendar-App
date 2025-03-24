package calendar.schedule.task.todo.event.reminder.common

object Constants {

    //https://www.googleapis.com/calendar/v3/calendars/indian__en@holiday.calendar.google.com/events?key=AIzaSyBSSVDB_R5Jh5HQl1LnCeuqOj0u1hprbqE&timeMin=2024-01-01T11:26:55Z&timeMax=2024-12-31T11:26:55Z&timezon=UTC&setSingleEvents=tru
    const val BASE_URL = "https://www.googleapis.com/calendar/v3/" //todo: https://dummyjson.com/users?limit=10&skip=10
    const val apiKey = "AIzaSyCZo6O1q9CwUmWOje5XxzHcTNWSykwdCdU"
    const val BASE_TAG = "A_"

    const val PREF_KEY_LANGUAGE = "pref_key_language"
    const val PREF_KEY_COUNTRIES = "pref_key_countries"
    const val PREF_KEY_LANGUAGE_AND_COUNTRIES = "pref_key_language_and_countries_string"
    const val PREF_KEY_APP_THEME = "pref_key_app_theme"
    const val PREF_KEY_AUTO_START_PERMISSION = "pref_key_auto_start_permission"
    const val PREF_KEY_TIME_FORMAT = "pref_key_time_format"
    const val PREF_KEY_FIRST_DAY_OF_THE_WEEK = "pref_key_first_day_of_the_week"

    const val KEY_YEAR = "key_year"
    const val KEY_MONTH = "key_month"
    const val KEY_DAY = "key_day"
    const val KEY_EVENT = "key_event"
    const val KEY_EVENT_JSON = "key_event_json"
    const val KEY_EVENT_REPEAT = "key_event_repeat"
    const val KEY_EVENT_ALERT = "key_event_alert"

    const val KEY_WHERE_TO_COMING = "Where to coming from, on language screen"

    const val KEY_LANGUAGE_CHANGE_GO_TO_SETTING_FRAG = "Change language go to setting fragment"
    const val KEY_IS_FIRST_TIME_LAUNCH_SHOW_LANGUAGE_ACTIVITY = "Is first time launch application, show language activity."

    const val EVENT_INSERT_SUCCESSFULLY = "Event insert successfully"
    const val EVENT_UPDATE_SUCCESSFULLY = "Event update successfully"
}