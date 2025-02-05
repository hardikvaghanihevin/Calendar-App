package com.hardik.calendarapp.presentation

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.common.DataListState
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
import com.hardik.calendarapp.domain.use_case.GetEventsByDateOfMonthOfTheYear
import com.hardik.calendarapp.domain.use_case.GetEventsByMonthOfTheYear
import com.hardik.calendarapp.domain.use_case.GetHolidayApiUseCase
import com.hardik.calendarapp.presentation.adapter.CountryItem
import com.hardik.calendarapp.utillities.DateUtil
import com.hardik.calendarapp.utillities.DateUtil.DATE_FORMAT_yyyy_MM_dd
import com.hardik.calendarapp.utillities.DateUtil.calculateNextOccurrence
import com.hardik.calendarapp.utillities.DateUtil.epochToDateTriple
import com.hardik.calendarapp.utillities.DateUtil.longToString
import com.hardik.calendarapp.utillities.DateUtil.stringToDateTriple
import com.hardik.calendarapp.utillities.createYearData
import com.hardik.calendarapp.utillities.createYearMonthPairs
import com.hardik.calendarapp.utillities.getAllCursorEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val application: Application,
    private val getHolidayApiUseCase: GetHolidayApiUseCase,// For API compatibility
    private val eventRepository: EventRepository,// For Database compatibility
    private val getAllEventsUseCase : GetAllEventsUseCase,// For getting all events (indicator use)
    private val getEventsByMonthOfYear: GetEventsByMonthOfTheYear,
    private val getEventsByDateOfMonthOfYear: GetEventsByDateOfMonthOfTheYear,
) : AndroidViewModel(application) {
    private val TAG = BASE_TAG + MainViewModel::class.java.simpleName

    private val _toolbarTitle = MutableStateFlow<String>("")
    val toolbarTitle: StateFlow<String> = _toolbarTitle // Public read-only StateFlow
    fun updateToolbarTitle(title: String) { _toolbarTitle.value = title }

    private val _tvMonthTitle = MutableStateFlow<String>("")
    val tvMonthTitle: StateFlow<String> = _tvMonthTitle // Public read-only StateFlow
    fun updateTvMonthTitle(tvMTitle: String) { _tvMonthTitle.value = tvMTitle }

    //----------------------------------------------------------------//

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)

    private val _languageCode = MutableStateFlow<String>(sharedPreferences.getString("language", "en") ?: "en")
    val languageCode: StateFlow<String> = _languageCode // Public read-only StateFlow

    fun updateLanguageCode(languageCode: String){
        viewModelScope.launch {
            _languageCode.value = languageCode
        }
    }

    //----------------------------------------------------------------//

    // StateFlow to hold the list of countries
    private val _countryItems = MutableStateFlow<List<CountryItem>>(emptyList())
    val countryItems: StateFlow<List<CountryItem>> get() = _countryItems

    // Initialize the list with saved and default data
    fun initializeCountries(savedCodes: Set<String>, countryItems: List<CountryItem>) {
        val updatedCountries = countryItems.map { countryItem ->
            countryItem.copy(isSelected = savedCodes.contains(countryItem.code))
        }
        _countryItems.value = updatedCountries
    }

    // Toggle selection for a country
    fun toggleCountrySelection(countryCode: String) {
        //todo: at least one last item always contain in list

        // Get the list of selected country codes
        val selectedCountryCodes: Set<String> = _countryItems.value
            .filter { it.isSelected } // Filter only selected items
            .map { it.code }          // Map to country codes
            .toSet()

        // Check if only one item is selected and it matches the last item
        if (selectedCountryCodes.size == 1 && selectedCountryCodes.contains(countryCode)) {
            // Prevent unselecting the last item if it's the only one selected
            return
        }

        //todo: clean all items when unselect all items its allow
        _countryItems.value = _countryItems.value.map { country ->
            if (country.code == countryCode) {
                country.copy(isSelected = !country.isSelected)
            } else {
                country
            }
        }
    }

    // Save selected countries
    fun saveSelectedCountries(selectedCountries: Set<String>) {
        sharedPreferences.edit()
            .putStringSet("countries", selectedCountries)
            .apply()
    }

    //----------------------------------------------------------------//

    //private val _holidayApiState = MutableStateFlow<DataState<HolidayApiDetail>>(DataState(isLoading = true))
    //private val holidayApiState: StateFlow<DataState<HolidayApiDetail>> get() = _holidayApiState

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
        viewModelScope.launch(Dispatchers.IO) {
            collectCursorEventsState(application.applicationContext)// fetched all cursor events (from cursor)
        }
    }

//    private val _holidayApiState = MutableStateFlow<DataListState<Event>>(DataListState(isLoading = true))
//    private val holidayApiState: StateFlow<DataListState<Event>> get() = _holidayApiState

    private val _isLoading = MutableStateFlow<Boolean>(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isCursorDataCollected = MutableStateFlow<Boolean>(false)
    val isCursorDataCollected: StateFlow<Boolean> = _isCursorDataCollected

    val allEvents : MutableList<Event> = mutableListOf()

    /**Observe [holidayApiState] after getting data from API*/
    private fun collectCursorEventsState(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isCursorDataCollected.value = false

                val cursorEvent = withContext(Dispatchers.IO) { getAllCursorEvents(context) }

                val events = cursorEvent.map { item ->
                    val startDate = longToString(item.startTime)
                    val endDate = longToString(item.endTime)
                    val date: Triple<String, String, String> = epochToDateTriple(item.startTime)

                    val startTime = DateUtil.stringToLong(startDate, DateUtil.DATE_FORMAT_yyyy_MM_dd)
                    val endTime = DateUtil.stringToLong(endDate, DateUtil.DATE_FORMAT_yyyy_MM_dd)

                    //val id = "${item.startTime} | ${item.title}"
                    Event(
                        id = item.id,
                        title = item.title,
                        description = item.description.orEmpty(),
                        startDate = startDate,
                        endDate = endDate,
                        year = date.first,
                        month = date.second,
                        date = date.third,
                        startTime = startTime,
                        endTime = endTime,
                        isHoliday = false,
                        sourceType = SourceType.CURSOR,
                        repeatOption = RepeatOption.NEVER,
                        alertOffset = AlertOffset.AT_TIME_OF_EVENT,
                        customAlertOffset = null,
                        triggerTime = startTime,
                    )
                }

                // Insert events into DB sequentially to avoid concurrency issues
                withContext(Dispatchers.IO) {
                    if (events.isNotEmpty()) {
                        insertEvents(events)
                    }
                }

            } catch (e: Exception) {
                // Log or handle errors here
                e.printStackTrace()
            } finally {
                _isCursorDataCollected.value = true
            }
        }
    }

    /**Get holiday list by using API*/
    fun getHolidayCalendarData() {
        viewModelScope.launch (Dispatchers.IO) {

            //todo: delete all event which already in DB from 'REMOTE'
            withContext(Dispatchers.IO) { eventRepository.deleteEventsHoliday() }

            allEvents.clear()

            _isLoading.value = true

            val languageCode = sharedPreferences.getString("language", "en") ?: "en" // Default to "en"
            val countryCodes: Set<String> = sharedPreferences.getStringSet("countries", setOf("indian")) ?: setOf("indian")

            // Create a list of deferred results for API calls
            val apiCalls = countryCodes.map { countryCode ->
                async(Dispatchers.IO) {
                    // Call the API for each country and collect results
                    getHolidayApiUseCase.invoke(countryCode = countryCode, languageCode = languageCode).collect { result: Resource<HolidayApiDetail> ->
                        when (result) {
                            is Resource.Success -> {
                                //_holidayApiState.value = DataState(data = result.data)
                                //collectHolidayApiState() // assuming this is a suspending function // fetched all events (from API)
                                if (result.data != null) {
                                    // Handle success case (trigger actions like logging, analytics, etc.)
                                    val calendarDetails = result.data

                                    // Process events
                                    val events: List<Event> = calendarDetails.items
                                        .map { item ->

                                            val date: Triple<String, String, String> = stringToDateTriple(item.start.date)

                                            val startTime = DateUtil.stringToLong(item.start.date, DateUtil.DATE_FORMAT_yyyy_MM_dd)
                                            val endTime = DateUtil.stringToLong(item.end.date, DateUtil.DATE_FORMAT_yyyy_MM_dd)

                                            Event(
                                                id = item.id,
                                                title = item.summary,
                                                description = item.description,
                                                startDate = item.start.date,
                                                endDate = item.end.date,
                                                year = date.first,
                                                month = date.second,
                                                date = date.third,
                                                startTime = startTime,
                                                endTime = endTime,
                                                isHoliday = true,
                                                sourceType = SourceType.REMOTE,
                                                repeatOption = RepeatOption.NEVER,//*
                                                alertOffset = AlertOffset.AT_TIME_OF_EVENT,//*
                                                customAlertOffset = null,//*
                                                triggerTime = startTime, // todo: set triggerTime as start time
                                            )

                                        }
                                    allEvents.addAll(events)
                                }
                            }

                            is Resource.Error -> { }

                            is Resource.Loading -> { }
                        }
                    }
                }
            }

            // Await all API calls to finish
            apiCalls.awaitAll()

            withContext(Dispatchers.IO) {
                insertEvents(allEvents)
                _isLoading.value = false
                allEvents.clear()
            }
        }
    }

    //----------------------------------------------------------------//

    private val insertEventsMutex = Mutex()
    private suspend fun insertEvents(events: List<Event>) {
        // Ensure only one coroutine executes this block at a time
        insertEventsMutex.withLock {
            _isLoading.value = true
            try {
                // Step 1: Cancel all existing alarms concurrently
                // Cancel all alarms concurrently (Cancel all existing alarms first)
                //val cancelJobs = events.map { event -> async(Dispatchers.IO) { eventRepository.cancelAlarm(event.id) } } // before UpsertEvents
                val cancelJobs = coroutineScope { events.map { event -> async { withContext(Dispatchers.IO) { eventRepository.cancelAlarm(event.id) } } } } // before UpsertEvents
                // Wait for all cancellation jobs to complete
                cancelJobs.awaitAll()

                //eventRepository.upsertEvents(events)
                // Upsert events after all alarms are canceled

                // Step 2: Update each event's nextTriggerTime
                val updatedEvents = coroutineScope {
                    events.map { event ->
                        async(Dispatchers.Default) {
                            var nextTriggerTime = event.triggerTime

                            // Calculate nextTriggerTime if needed
                            if (nextTriggerTime <= System.currentTimeMillis() && event.repeatOption != RepeatOption.NEVER) {
                                val calculatedTriggerTime = calculateNextOccurrence(nextTriggerTime, event.repeatOption)
                                if (calculatedTriggerTime != null) {
                                    nextTriggerTime = calculatedTriggerTime
                                }
                            }

                            // Return the updated event
                            event.copy(triggerTime = nextTriggerTime)
                        }
                    }.awaitAll() // Collect all updated events
                }

                withContext(Dispatchers.IO) { eventRepository.upsertEvents(updatedEvents) }

            } catch (e: Exception) {
                // Handle any errors
            }finally {
                _isLoading.value = false
            }
        }
    }

    //----------------------------------------------------------------//
    // todo:for event showing below inside month view

    private val _text = MutableLiveData<String>().apply { value = "No Events" }
    val text: LiveData<String> = _text

    private val _monthlyEventsState = MutableStateFlow<DataListState<Event>>(DataListState(isLoading = true))
    val monthlyEventsState: StateFlow<DataListState<Event>> get() = _monthlyEventsState

    fun getEventsByMonthOfYear(year: String, month: String){//todo: use in CalendarMonth1Fragment for onMonthSwipe or onMonthClick
        _monthlyEventsState.value = DataListState(isLoading = true)

        viewModelScope.launch {
            isLoading.collect{
                if (it == true){
                    _monthlyEventsState.value = DataListState(isLoading = true)
                }else{
                    try {
                        getEventsByMonthOfYear.invoke(year = year, month = month).collectLatest { events ->
                            // Update state with data
                            _monthlyEventsState.value = DataListState(isLoading = false, data = events)
                            setFirstEventOfEachWeek(events)
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
        }
    }

    fun getEventsByDateOfMonthOfYear(year: String, month: String, date: String) {//todo: use in CalendarMonth1Fragment for onDateClick
        _monthlyEventsState.value = DataListState(isLoading = true)

        viewModelScope.launch {
            isLoading.collect{
                if (it == true){
                    _monthlyEventsState.value = DataListState(isLoading = true)
                }else {
                    try {
                        getEventsByDateOfMonthOfYear.invoke(year = year, month = month, date = date)
                            .collectLatest { events ->
                                // Update state with data
                                _monthlyEventsState.value = DataListState(isLoading = false, data = events)
                                setFirstEventOfEachWeek(events)
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
        }
    }

    fun fetchEventsForMonthView(sDate: String){
        val date: Triple<String, String, String> = stringToDateTriple(sDate, isZeroBased = false)
        if (sDate.last() == '0' && !sDate.endsWith("10") && !sDate.endsWith("20") && !sDate.endsWith("30")){//0,10,20,30
            getEventsByMonthOfYear(year = date.first, month = date.second)
        }else{
            getEventsByDateOfMonthOfYear(year = date.first, month = date.second, date = date.third)
        }
    }//Use this for both combo base on date it-selves call

    //----------------------------------------------------------------//

    private val _firstEventOfEachWeek = MutableStateFlow<Map<String, Event>>(emptyMap())
    val firstEventOfEachWeek: StateFlow<Map<String, Event>> = _firstEventOfEachWeek

    // Function to update events grouped by week
    private fun setFirstEventOfEachWeek(newData: List<Event>) {
        val dateFormatter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DateTimeFormatter.ofPattern(DATE_FORMAT_yyyy_MM_dd)
        } else {
            TODO("VERSION.SDK_INT < O")
        }

        val firstDayOfWeek = when (_firstDayOfTheWeek.value) {
            "Monday" -> DayOfWeek.MONDAY
            "Saturday" -> DayOfWeek.SATURDAY
            else -> DayOfWeek.SUNDAY
        }

        val groupedByYearWeek = newData.groupBy { event ->
            val localDate = LocalDate.parse(event.startDate, dateFormatter)
            val weekField = WeekFields.of(firstDayOfWeek, 1).weekOfWeekBasedYear()
            val year = localDate.getYear()
            val month = localDate.monthValue - 1  // Convert to 0-based month
            val week = localDate.get(weekField)
            "$year-$month-$week"  // Unique key for year-week combination
        }

        val firstEvents = groupedByYearWeek.mapValues { (key, events) ->
            events.minByOrNull { LocalDate.parse(it.startDate, dateFormatter) }!!
        }

        // Update StateFlow with new values
        _firstEventOfEachWeek.update { firstEvents.mapKeys { it.key } }
    }

    //----------------------------------------------------------------//

    private val _currentEventPos = MutableStateFlow<Int>(0)
    val currentEventPos: StateFlow<Int> = _currentEventPos

    fun findPositionOfEvent(data: List<Event>) {
        val currentDate = DateUtil.getCurrentDate(pattern =  DATE_FORMAT_yyyy_MM_dd)//"2025-06-05"

        /// Map events to their start dates and associate with original indices
        val dateList: List<Pair<String, Int>> = data.mapIndexed { index, event -> event.startDate to index }

        // Try to find the exact match
        val exactIndex = dateList.indexOfFirst { it.first == currentDate }

        if (exactIndex != -1) {
            _currentEventPos.value = exactIndex
            return
        }

        // Find the nearest future event (not sorted)
        val nextFutureIndex = dateList.filter { it.first > currentDate }
            .minByOrNull { it.first }?.second

        // Assign the result; fallback to 0 if no future event
        _currentEventPos.value = nextFutureIndex ?: 0
    }


    private val _allEventsState = MutableStateFlow<DataListState<Event>>(DataListState(isLoading = true))
    val allEventsState: StateFlow<DataListState<Event>> get() = _allEventsState
    fun getAllEvents() {//todo: use in CalendarMonthFragment for onMonthSwipe
        // Set initial loading state
        _allEventsState.value = DataListState(isLoading = true)

        viewModelScope.launch {
            isLoading.collect{
                if (it == true){
                    _allEventsState.value = DataListState(isLoading = true)
                }else {
                    try {
                        getAllEventsUseCase.invoke().collectLatest { events ->
                            // Update state with data
                            setFirstEventOfEachWeek(events)
                            _allEventsState.value = DataListState(isLoading = false, data = events )
                        }
                    } catch (e: Exception) {
                        // Handle errors
                        _allEventsState.value = DataListState(
                            isLoading = false,
                            error = e.message ?: "An unknown error occurred"
                        )
                    }
                }
            }
        }
    }

    // for month view's indicator
    private val _allEventsDateInMapState = MutableStateFlow<MutableMap<YearKey, MutableMap<MonthKey, MutableMap<DayKey, EventValue>>>>(mutableMapOf())
    val allEventsDateInMapState: StateFlow<MutableMap<YearKey, MutableMap<MonthKey, MutableMap<DayKey, EventValue>>>> get() = _allEventsDateInMapState


    //todo:for event indicator showing in month view using map
    private fun getAllEventsDateInMap(){
        viewModelScope.launch {
            try {
                getAllEventsUseCase.invoke().collectLatest{ events: List<Event> ->
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
        viewModelScope.launch {
            _yearState.value = year
        }
    }

    private val _monthViewDate = MutableStateFlow<String>("2000-0-0")//null, "2000-0-1" //Triple<String, String, String>
    val monthViewDate: StateFlow<String> = _monthViewDate
    fun updateMonthViewDate(monthViewDate: String){
        viewModelScope.launch {
            _monthViewDate.value = monthViewDate
        }
    }

    private val _selectedDate = MutableStateFlow<String>("2000-0-0")//null, "2000-0-1"
    val selectedDate: StateFlow<String> = _selectedDate

    fun updateSelectedDate(selectedDate: String){
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

    // Unregister listener to avoid memory leaks when ViewModel is cleared
    override fun onCleared() {
        super.onCleared()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener { _, _ -> }
    }
}