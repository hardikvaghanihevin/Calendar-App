package com.hardik.calendarapp.presentation

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.common.DataListState
import com.hardik.calendarapp.common.DataState
import com.hardik.calendarapp.common.Resource
import com.hardik.calendarapp.data.database.entity.AlertOffset
import com.hardik.calendarapp.data.database.entity.DayKey
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.data.database.entity.EventValue
import com.hardik.calendarapp.data.database.entity.MonthKey
import com.hardik.calendarapp.data.database.entity.RepeatOption
import com.hardik.calendarapp.data.database.entity.SourceType
import com.hardik.calendarapp.data.database.entity.YearKey
import com.hardik.calendarapp.data.database.entity.organizeEvents
import com.hardik.calendarapp.domain.model.HolidayApiDetail
import com.hardik.calendarapp.domain.repository.EventRepository
import com.hardik.calendarapp.domain.use_case.GetAllEventsUseCase
import com.hardik.calendarapp.domain.use_case.GetEventsByDateOfMonthOfYear
import com.hardik.calendarapp.domain.use_case.GetEventsByMonthOfYear
import com.hardik.calendarapp.domain.use_case.GetHolidayApiUseCase
import com.hardik.calendarapp.domain.use_case.GetMonthlyEventsUseCase
import com.hardik.calendarapp.utillities.CursorEvent
import com.hardik.calendarapp.utillities.DateUtil
import com.hardik.calendarapp.utillities.DateUtil.epochToDateTriple
import com.hardik.calendarapp.utillities.DateUtil.longToString
import com.hardik.calendarapp.utillities.DateUtil.stringToDateTriple
import com.hardik.calendarapp.utillities.createYearData
import com.hardik.calendarapp.utillities.createYearMonthPairs
import com.hardik.calendarapp.utillities.getAllCursorEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val application: Application,
    private val getHolidayApiUseCase: GetHolidayApiUseCase,// For API compatibility
    private val eventRepository: EventRepository,// For Database compatibility
    private val getAllEventsUseCase : GetAllEventsUseCase,// For getting all events (indicator use)
    private val getMonthlyEventsUseCase: GetMonthlyEventsUseCase,// For getting monthly events compatibility (start to end date) (eventAdapter use)
    private val getEventsByMonthOfYear: GetEventsByMonthOfYear,
    private val getEventsByDateOfMonthOfYear: GetEventsByDateOfMonthOfYear,
) : AndroidViewModel(application) {
    private val TAG = BASE_TAG + MainViewModel::class.java.simpleName

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)

    private val _languageCode = MutableStateFlow<String>(sharedPreferences.getString("language", "en") ?: "en")
    val languageCode: StateFlow<String> = _languageCode // Public read-only StateFlow

    fun updateLanguageCode(languageCode: String){
        viewModelScope.launch {
            _languageCode.value = languageCode
        }
    }

    private val _holidayApiState = MutableStateFlow<DataState<HolidayApiDetail>>(DataState(isLoading = true))
    val holidayApiState: StateFlow<DataState<HolidayApiDetail>> get() = _holidayApiState

    init {
        generateYearList(2000, 2100, isZeroBased = true)
        //getHolidayCalendarData()
        getAllEventsDateInMap()
    }

    private val _yearList = MutableStateFlow<Map<Int, Map<Int, List<Int>>>>(emptyMap())
    val yearList: StateFlow<Map<Int, Map<Int, List<Int>>>> = _yearList

    /** The yearList and perform the data generation in a coroutine.*/
    private fun generateYearList(startYear: Int, endYear: Int, isZeroBased: Boolean) {
        viewModelScope.launch {
            val data = withContext(Dispatchers.Default) {
                createYearData(startYear, endYear, isZeroBased)
            }
            generateYearMonthPairs(startYear,endYear,isZeroBased)
            _yearList.value = data
        }
    }

    private val _yearMonthPairList = MutableStateFlow<List<Pair<Int, Int>>>(emptyList())
    val yearMonthPairList: StateFlow<List<Pair<Int, Int>>> = _yearMonthPairList

    /** The yearMonthPairList and perform the data generation in a coroutine.*/
    private fun generateYearMonthPairs(startYear: Int, endYear: Int, isZeroBased: Boolean) {
        viewModelScope.launch {
            val data = withContext(Dispatchers.Default) {
                createYearMonthPairs(startYear, endYear, isZeroBased)
            }
            _yearMonthPairList.value = data
        }
    }



    fun initializeViewModel() {
        collectCursorEventsState(application.applicationContext)// fetched all cursor events (from cursor)
    }

    /**Get holiday list by using API*/
    fun getHolidayCalendarData() {
        Log.i(TAG, "getHolidayCalendarData: ")
        viewModelScope.launch(Dispatchers.IO) {
            val languageCode = sharedPreferences.getString("language", "en") ?: "en" // Default to "en"
            val countryCodes: Set<String> = sharedPreferences.getStringSet("countries",setOf("indian")) ?: setOf("indian")

            Log.d(TAG, "getHolidayCalendarData: countryCode:$countryCodes")
            countryCodes.forEach { countryCode ->
                getHolidayApiUseCase.invoke(countryCode = countryCode, languageCode = languageCode)
                    .collect { result: Resource<HolidayApiDetail> ->
                        when (result) {
                            is Resource.Success -> {
                                _holidayApiState.value = DataState(data = result.data);
                                launch(Dispatchers.IO) {
                                    collectHolidayApiState()// fetched all events (from API)
                                }
                            }

                            is Resource.Error -> {
                                _holidayApiState.value =
                                    DataState(
                                        error = result.message ?: "An unexpected error occurred"
                                    )
                            }

                            is Resource.Loading -> {
                                _holidayApiState.value = DataState(isLoading = true)
                            }

                            else -> {}
                        }
                    }
            }
        }
    }

    /**Observe [holidayApiState] after getting data from API*/
    private fun collectHolidayApiState() {//insert in to DB
        Log.i(TAG, "collectHolidayApiState: ")
        viewModelScope.launch(Dispatchers.IO) {
            holidayApiState.collectLatest { state ->
                when {
                    state.isLoading -> {
                        // Handle loading state (maybe trigger other UI-related actions or logging)
                        Log.d(TAG, "collectHolidayApiState: isLoading state: ${state.isLoading}")
                    }

                    state.error.isNotEmpty() -> {
                        // Handle error state (maybe trigger logging, analytics, etc.)
                        Log.d(TAG, "collectHolidayApiState: Error state: ${state.error}")
                    }

                    state.data != null -> {
                        // Handle success case (trigger actions like logging, analytics, etc.)
                        val calendarDetails = state.data
                        Log.d(TAG, "collectHolidayApiState: data available")

                        // Process events
                        val events: List<Event> = calendarDetails.items
                            .mapNotNull { item ->

                                val date: Triple<String, String, String> = stringToDateTriple(item.start.date)

                                Event(
                                    id = "${DateUtil.stringToLong(item.start.date,DateUtil.DATE_FORMAT_yyyy_MM_dd)} | ${item.summary}",
                                    title = item.summary,
                                    description = item.description,
                                    startDate = item.start.date,
                                    endDate = item.end.date,
                                    year = date.first,
                                    month = date.second,
                                    date = date.third,
                                    startTime = DateUtil.stringToLong(item.start.date, DateUtil.DATE_FORMAT_yyyy_MM_dd),
                                    endTime = DateUtil.stringToLong(item.end.date, DateUtil.DATE_FORMAT_yyyy_MM_dd),
                                    isHoliday = true,
                                    sourceType = SourceType.REMOTE,
                                    repeatOption = RepeatOption.NEVER,//*
                                    alertOffset = AlertOffset.AT_TIME,//*
                                    customAlertOffset = null,//*
                                    eventId = DateUtil.stringToLong(item.start.date, DateUtil.DATE_FORMAT_yyyy_MM_dd), //as event id
                                )

                            }
                            .filterNotNull() // Filter out null values resulting from mapNotNull

                        // Insert events into your database or UI
                        insertEvents(events)
                    }
                }
            }
        }
    }

    private fun collectCursorEventsState(context: Context){
        val cursorEvent: List<CursorEvent> = getAllCursorEvents(context = context)

        val events: List<Event> = cursorEvent
            .mapNotNull { item ->

                val date: Triple<String, String, String> = epochToDateTriple(item.startTime)

                val startTime = DateUtil.separateDateTime(item.startTime).first
                val endTime = DateUtil.separateDateTime(item.endTime).first
                Event(
                    id = "${startTime} | ${item.title}",
                    title = item.title,
                    description = item.description ?: "",
                    startDate = longToString(item.startTime),
                    endDate = longToString(item.endTime),
                    year = date.first,
                    month = date.second,
                    date = date.third,
                    startTime = startTime,
                    endTime = endTime,
                    isHoliday = true,
                    sourceType = SourceType.CURSOR,
                    repeatOption = RepeatOption.NEVER,//*
                    alertOffset = AlertOffset.AT_TIME,//*
                    customAlertOffset = null,//*
                    eventId = startTime//todo: here to set unique things set for loop by 1 to n list size
                )

            }
            .filterNotNull() // Filter out null values resulting from mapNotNull

        // Insert events into your database or UI
        insertEvents(events)

    }

    //----------------------------------------------------------------//

    fun insertEvent(event: Event) {
        viewModelScope.launch {
            eventRepository.upsertEvent(event)
        }
    }

    private fun insertEvents(events: List<Event>) {
        viewModelScope.launch(Dispatchers.IO) {
            // Get the current counter value
            var currentCounter = counterEventId.value

            // Update events and increment the counter for each one
            val updatedEvents = events.map { event ->
                val updatedEvent = event.copy(eventId = event.eventId + currentCounter)
                currentCounter += 1 // Increment locally
                updatedEvent
            }

            // Update the global counter with the total increment
            updateCounterEventId(events.size)

            // Insert updated events into the repository
            eventRepository.upsertEvents(updatedEvents)
        }
    }

    // Global counter
    private val _counterEventId = MutableStateFlow(1L)
    val counterEventId: StateFlow<Long> = _counterEventId

    fun updateCounterEventId(count:Int = 1 ){
        viewModelScope.launch(Dispatchers.IO) {
            // Update the counter atomically
            _counterEventId.value += count
        }
    }

    //----------------------------------------------------------------//
    // todo:for event showing below inside month view

    private val _text = MutableLiveData<String>().apply { value = "No Events" }
    val text: LiveData<String> = _text

    private val _monthlyEventsState = MutableStateFlow<DataListState<Event>>(DataListState(isLoading = true))
    val monthlyEventsState: StateFlow<DataListState<Event>> get() = _monthlyEventsState
    fun getMonthlyEvents(startOfMonth: Long, endOfMonth: Long) {//todo: use in CalendarMonthFragment for onMonthSwipe
        Log.i(TAG, "fetchEventsForMonth: ")
        // Set initial loading state
        _monthlyEventsState.value = DataListState(isLoading = true)

        viewModelScope.launch {
            try {
                getMonthlyEventsUseCase.invoke(startOfMonth = startOfMonth, endOfMonth = endOfMonth)
                    .collect { events ->
                        // Update state with data
                        val dummyList = mutableListOf<Event>()
                        //repeat(10) { index -> dummyList.add(Event(title = "Dummy", startTime = 0L, endTime = 0L, startDate = "", endDate = "", isHoliday = false, description = "Dummy Event")) }
                        //delay(100)//for showing progressbar

                        _monthlyEventsState.value = DataListState(isLoading = false, data = events + dummyList)
                    }
            } catch (e: Exception) {
                // Handle errors
                _monthlyEventsState.value = DataListState(
                    isLoading = false,
                    error = e.message ?: "An unknown error occurred"
                )
            }
        }
    }

    fun getEventsByMonthOfYear(year: String, month: String){//todo: use in CalendarMonth1Fragment for onMonthSwipe or onMonthClick
        Log.i(TAG, "getEventsByMonthOfYear: $year-$month")
        _monthlyEventsState.value = DataListState(isLoading = true)

        viewModelScope.launch {
            try {
                getEventsByMonthOfYear.invoke(year = year, month = month).collectLatest { events ->
                    // Update state with data
                    _monthlyEventsState.value = DataListState(isLoading = false, data = events)
                }
            } catch (e: Exception) {
                // Handle errors
                _monthlyEventsState.value = DataListState(
                    isLoading = false,
                    error = e.message ?: "An unknown error occurred"
                )
            }
        }
    }

    fun getEventsByDateOfMonthOfYear(year: String, month: String, date: String) {//todo: use in CalendarMonth1Fragment for onDateClick
        Log.i(TAG, "getEventsByDateOfMonthOfYear: $year-$month-$date")
        _monthlyEventsState.value = DataListState(isLoading = true)

        viewModelScope.launch {
            try {
                getEventsByDateOfMonthOfYear.invoke(year = year, month = month, date = date).collectLatest { events ->
                    // Update state with data
                    _monthlyEventsState.value = DataListState(isLoading = false, data = events)
                }
            } catch (e: Exception){
                // Handle errors
                _monthlyEventsState.value = DataListState(
                    isLoading = false,
                    error = e.message ?: "An unknown error occurred"
                )
            }
        }
    }
    //----------------------------------------------------------------//

    private val _allEventsDateInMapState = MutableStateFlow<MutableMap<YearKey, MutableMap<MonthKey, MutableMap<DayKey, EventValue>>>>(mutableMapOf())
    val allEventsDateInMapState: StateFlow<MutableMap<YearKey, MutableMap<MonthKey, MutableMap<DayKey, EventValue>>>> get() = _allEventsDateInMapState


    //todo:for event indicator showing in month view using map
    private fun getAllEventsDateInMap(){
        Log.i(TAG, "fetchAllEventsOfDateMap: ")
        viewModelScope.launch {
            try {
                getAllEventsUseCase.invoke().collect{ events: List<Event> ->
                    val organizedEvents = organizeEvents(events)
                    _allEventsDateInMapState.emit(organizedEvents)
                }
            }catch (e: Exception) {
                // Handle errors
                val error = e.message ?: "An unknown error occurred"
            }
        }
    }


    private val _yearState = MutableStateFlow<Int>(Calendar.getInstance().get(Calendar.YEAR))
    val yearState: StateFlow<Int> = _yearState

    fun updateYear(year: Int) {
        Log.i(TAG, "updateYear: $year")
        viewModelScope.launch {
            _yearState.value = year
        }
    }

    private val _selectedDate = MutableStateFlow<String?>(null)//null, "2000-0-1"
    val selectedDate: StateFlow<String?> = _selectedDate

    fun updateSelectedDate(selectedDate: String?){
        viewModelScope.launch {
            _selectedDate.value = selectedDate
        }
    }

    //----------------------------------------------------------------//

    //Todo: First day of the week
    private val defaultFirstDayOfWeek = sharedPreferences.getString("firstDayOfWeek", "Sunday")?: "Sunday"// Default to Sunday
    private val _firstDayOfTheWeek = MutableStateFlow<String>(defaultFirstDayOfWeek)
    val firstDayOfTheWeek: StateFlow<String> = _firstDayOfTheWeek

    fun updateFirstDayOfTheWeek(refresh: String = "Sunday"){
        viewModelScope.launch {
            _firstDayOfTheWeek.value = refresh
        }
    }

    //----------------------------------------------------------------//

    //Todo:Jump to date
    private val _yearJTD = MutableStateFlow<Int>(Calendar.getInstance().get(Calendar.YEAR))
    val yearJTD: StateFlow<Int> = _yearJTD
    fun updateYearJTD(year: Int){
        viewModelScope.launch {
            _yearJTD.value = year
        }
    }

    private val _monthJTD = MutableStateFlow<Int>(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val monthJTD: StateFlow<Int> = _monthJTD
    fun updateMonthJTD(month: Int){
        viewModelScope.launch {
            _monthJTD.value = month
        }
    }

    private val _dateJTD = MutableStateFlow<Int>(Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
    val dateJTD: StateFlow<Int> = _dateJTD
    fun updateDateJTD(date: Int){
        viewModelScope.launch {
            _dateJTD.value = date
        }
    }

    //todo: Dynamically compute dateMaxJTD based on yearJTD and monthJTD changes (jan,fab,mar,apr..eg.31,29/29,31,30)
    val dateMaxJTD: StateFlow<Int> = combine(_yearJTD, _monthJTD) { year, month ->
        val maxDays = getMinMaxDays(year, month - 1) // Adjust to 0-based month
        maxDays.second ?: 1
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        Calendar.getInstance().get(Calendar.DAY_OF_MONTH) // Initial default value
    )

    // Function to get the min and max days for a specific year and month for jump to date
    private fun getMinMaxDays(year: Int, month: Int): Pair<Int?, Int?> {
        val daysInMonth = yearList.value[year]?.get(month)
        return if (daysInMonth != null) {
            Pair(daysInMonth.minOrNull(), daysInMonth.maxOrNull())
        } else {
            Pair(null, null) // If no days are found for the month, return null
        }
    }

    //todo: Combined StateFlow to generate the full date string (eg.Monday 1 January 2025)
    val fullDateJTD: StateFlow<String> = combine(_yearJTD, _monthJTD, _dateJTD, _languageCode) { year, month, date, languageCode ->
        getFormattedDate(year, month, date, languageCode)
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        getFormattedDate(
            _yearJTD.value,
            _monthJTD.value,
            _dateJTD.value,
            _languageCode.value
        ) // Initial value
    )

    private fun getFormattedDate(year: Int, month: Int, day: Int, languageCode: String): String {//todo: generate the full date string (eg.Monday 1 January 2025)
        // Create a Calendar instance and set the provided year, month, and day
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, day) // month - 1 because Calendar months are 0-based

        // Set the appropriate Locale based on the language code
        val locale = Locale(languageCode)

        // Format the date
        val dateFormat = SimpleDateFormat("EEEE dd MMMM yyyy", locale)
        return dateFormat.format(calendar.time)
    }
    //----------------------------------------------------------------//

    //Todo: Event start date
    /*private val _startDate= MutableStateFlow<Long>(Calendar.getInstance().timeInMillis)
    val startDate: StateFlow<Long> = _startDate
    fun updateStartDate(startDate: Long) {
        Log.i(TAG, "updateStartDate: $startDate")
        viewModelScope.launch {
            _startDate.value = startDate
        }
    }

    //todo: Event end date
    private val _endDate= MutableStateFlow<Long>(Calendar.getInstance().timeInMillis)
    val endDate: StateFlow<Long> = _endDate
    fun updateEndDate(endDate: Long) {
        Log.i(TAG, "updateEndDate: $endDate")
        viewModelScope.launch {
            _endDate.value = endDate
        }
    }

    //----------------------------------------------------------------//

    //todo: Event All-Day
    private val _isAllDay = MutableStateFlow(false) // Default to false (not all day)
    val isAllDay: StateFlow<Boolean> = _isAllDay

    fun updateAllDayStatus(isAllDay: Boolean) {
        Log.i(TAG, "updateAllDayStatus: $isAllDay")
        viewModelScope.launch {
            _isAllDay.value = isAllDay
        }
        val mergeDataTime = mergeDateAndTime(startDate.value,startTime.value)
        Log.e(TAG, "updateAllDayStatus: $mergeDataTime", )
        Log.e(TAG, "updateAllDayStatus: ${separateDateTime(mergeDataTime)}")
    }

    //----------------------------------------------------------------//

    private val calendar = Calendar.getInstance()

    // Set default start time to current time (hour and minute)
    private val defaultStartTime = calendar.apply {
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    // Set default end time to one hour ahead
    private val defaultEndTime = calendar.apply {
        add(Calendar.HOUR_OF_DAY, 1)
    }.timeInMillis

    //todo: Event start time
    private val _startTime= MutableStateFlow<Long>(defaultStartTime)
    val startTime: StateFlow<Long> = _startTime

    //todo: Event end time
    private val _endTime= MutableStateFlow<Long>(defaultEndTime)
    val endTime: StateFlow<Long> = _endTime

    fun updateStartTime(startTime: Long) {
        Log.i(TAG, "updateStartTime: $startTime")
        viewModelScope.launch {
            _startTime.value = startTime
        }
    }

    fun updateEndTime(endTime: Long) {
        Log.i(TAG, "updateEndTime: $endTime")
        viewModelScope.launch {
            _endTime.value = endTime
        }
    }

    //todo: Event title
    private val _title= MutableStateFlow("")
    val title: StateFlow<String> = _title

    fun updateTitle(title: String) {
        Log.i(TAG, "updateTitle: $title")
        viewModelScope.launch {
            _title.value = title
        }
    }

    //todo: Event description
    private val _description= MutableStateFlow("")
    val description: StateFlow<String> = _description

    fun updateDescription(description: String) {
        Log.i(TAG, "updateDescription: $description")
        viewModelScope.launch {
            _description.value = description
        }
    }

    fun insertCustomEvent(){
        val currentEpochTime = System.currentTimeMillis()

        val date: Triple<String, String, String> = epochToDateTriple(
            startDate.value)

        val event = Event(
            id = "$currentEpochTime | ${title.value}",
            title = title.value,
            description = description.value,
            startDate = DateUtil.longToString(startDate.value, DateUtil.DATE_FORMAT),
            endDate = DateUtil.longToString(endDate.value, DateUtil.DATE_FORMAT),
            startTime = startTime.value,
            endTime = endTime.value,
            year = date.first,
            month = date.second,
            date = date.third,
            eventType = EventType.PERSONAL,
            isHoliday = false
        )

        insertEvent(event)
    }
*/

    // Unregister listener to avoid memory leaks when ViewModel is cleared
    override fun onCleared() {
        super.onCleared()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener { _, _ -> }
    }
}