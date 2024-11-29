package com.hardik.calendarapp.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.common.DataListState
import com.hardik.calendarapp.common.DataState
import com.hardik.calendarapp.common.Resource
import com.hardik.calendarapp.data.database.entity.DayKey
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.data.database.entity.EventValue
import com.hardik.calendarapp.data.database.entity.MonthKey
import com.hardik.calendarapp.data.database.entity.YearKey
import com.hardik.calendarapp.data.database.entity.getAllKeysAsList
import com.hardik.calendarapp.data.database.entity.organizeEvents
import com.hardik.calendarapp.domain.model.HolidayApiDetail
import com.hardik.calendarapp.domain.repository.EventRepository
import com.hardik.calendarapp.domain.use_case.GetEventIndicatorMapUseCase
import com.hardik.calendarapp.domain.use_case.GetHolidayApiUseCase
import com.hardik.calendarapp.domain.use_case.GetMonthlyEventsUseCase
import com.hardik.calendarapp.domain.use_case.ObserveAllEventsUseCase
import com.hardik.calendarapp.utillities.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getHolidayApiUseCase: GetHolidayApiUseCase,
    private val eventRepository: EventRepository,
    private val getMonthlyEventsUseCase: GetMonthlyEventsUseCase,
    private val observeAllEventsUseCase : ObserveAllEventsUseCase,
    private val getEventIndicatorMapUseCase: GetEventIndicatorMapUseCase,
) : ViewModel() {
    private val TAG = BASE_TAG + MainViewModel::class.java.simpleName
    var currentPosition: Int = 50 // Default position
    var toolbarTitle: String = "2022" // Default toolbar title
    var currentYear: Int = 2022 // Default year

    private val _state = MutableStateFlow<DataState<HolidayApiDetail>>(DataState(isLoading = true))
    val state: StateFlow<DataState<HolidayApiDetail>> get() = _state

    init {
        getHolidayCalendarData()
        fetchAllEventsOfDateMap()
    }

    private fun getHolidayCalendarData() {
        Log.i(TAG, "getHolidayCalendarData: ")
        viewModelScope.launch {
            getHolidayApiUseCase.invoke().collect { result: Resource<HolidayApiDetail> ->
                when (result) {
                    is Resource.Success -> {
                        _state.value = DataState(data = result.data); collectState()
                    }

                    is Resource.Error -> {
                        _state.value =
                            DataState(error = result.message ?: "An unexpected error occurred")
                    }

                    is Resource.Loading -> {
                        _state.value = DataState(isLoading = true)
                    }

                    else -> {}
                }
            }
        }
    }

    private fun collectState() {//insert int DB
        Log.i(TAG, "collectState: ")
        viewModelScope.launch {
            state.collect { state ->
                when {
                    state.isLoading -> {
                        // Handle loading state (maybe trigger other UI-related actions or logging)
                        Log.d(TAG, "collectState: isLoading state: ${state.isLoading}")
                    }

                    state.error.isNotEmpty() -> {
                        // Handle error state (maybe trigger logging, analytics, etc.)
                        Log.d(TAG, "collectState: Error state: ${state.error}")
                    }

                    state.data != null -> {
                        // Handle success case (trigger actions like logging, analytics, etc.)
                        val calendarDetails = state.data
                        Log.d(TAG, "collectState: data available")

                        // Process events
                        val events: List<Event> = calendarDetails.items
                            .mapNotNull { item ->

                                val date: Triple<String, String, String> = item.start.date.split("-").let { parts ->
                                    val year = parts[0]
                                    val month = (parts[1].toInt() - 1).toString()  // Adjust month (1-based to 0-based)
                                    val day = parts[2].toInt().toString()  // Get the day as a string
                                    //Log.e(TAG, "collectState: ${item.start.date} -> $year,$month,$day")

                                    // Return a Triple with year, month, and day
                                    Triple(year, month, day)
                                }

                                Event(
                                    id = DateUtil.stringToLong(item.start.date,DateUtil.DATE_FORMAT),
                                    title = item.summary,
                                    description = item.description,
                                    startDate = item.start.date,
                                    endDate = item.end.date,
                                    year = date.first,
                                    month = date.second,
                                    date = date.third,
                                    startTime = DateUtil.stringToLong(item.start.date, DateUtil.DATE_FORMAT),
                                    endTime = DateUtil.stringToLong(item.end.date, DateUtil.DATE_FORMAT),
                                    isHoliday = true
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


    fun insertEvent(event: Event) {
        viewModelScope.launch {
            eventRepository.upsertEvent(event)
        }
    }

    private fun insertEvents(events: List<Event>) {
        viewModelScope.launch {
            eventRepository.upsertEvents(events)
        }
    }


    private val _text = MutableLiveData<String>().apply { value = "No Events" }
    val text: LiveData<String> = _text

    private val _stateEventsOfMonth = MutableStateFlow<DataListState<Event>>(DataListState(isLoading = true))
    val stateEventsOfMonth: StateFlow<DataListState<Event>> get() = _stateEventsOfMonth

    private val _stateEventsOfDateMap = MutableStateFlow<MutableMap<YearKey, MutableMap<MonthKey, MutableMap<DayKey, EventValue>>>>(mutableMapOf())
    val stateEventsOfDateMap: StateFlow<MutableMap<YearKey, MutableMap<MonthKey, MutableMap<DayKey, EventValue>>>> get() = _stateEventsOfDateMap

    private val _stateEventsOfDate = MutableStateFlow<List<String>>(emptyList())
    val stateEventsOfDate: StateFlow<List<String>> get() = _stateEventsOfDate
    // todo:for event showing below inside month view
    fun fetchEventsForMonth(startOfMonth: Long, endOfMonth: Long) {
        Log.i(TAG, "fetchEventsForMonth: ")
        // Set initial loading state
        _stateEventsOfMonth.value = DataListState(isLoading = true)

        viewModelScope.launch {
            try {
                getMonthlyEventsUseCase.invoke(startOfMonth = startOfMonth, endOfMonth = endOfMonth)
                    .collect { events ->
                        // Update state with data
                        val dummyList = mutableListOf<Event>()
                        //repeat(10) { index -> dummyList.add(Event(title = "Dummy", startTime = 0L, endTime = 0L, startDate = "", endDate = "", isHoliday = false, description = "Dummy Event")) }
                        //delay(100)//for showing progressbar

                        _stateEventsOfMonth.value = DataListState(isLoading = false, data = events + dummyList)
                    }
            } catch (e: Exception) {
                // Handle errors
                _stateEventsOfMonth.value = DataListState(
                    isLoading = false,
                    error = e.message ?: "An unknown error occurred"
                )
            }
        }
    }

    //todo:for event indicator showing in month view
    private fun fetchAllEventsOfDateMap(){
        Log.i(TAG, "fetchAllEventsOfDateMap: ")
        viewModelScope.launch {
            try {
                observeAllEventsUseCase.invoke().collect{events: List<Event> ->
                    //val evetns: List<String> = events.map { it.startDate.getFormattedDate() }.distinct() // Remove duplicates
                    //val eventsMap: List<Map<String, String>> = events.map { event -> mapOf(event.startDate.getFormattedDate() to event.startDate) }

                    val organizedEvents = organizeEvents(events)
                    _stateEventsOfDateMap.emit(organizedEvents)
                    //_stateEventsOfDateMap.value = organizedEvents // Emit the new map

                    val startTime = System.nanoTime()
                    val allKeys = getAllKeysAsList(organizedEvents)
                    val endTime = System.nanoTime()
                    Log.d(TAG, "fetchAllEventsOfDateMap: execution time: ${(endTime - startTime)} ns, ${(endTime - startTime) / 1_000} µs, ${(endTime - startTime) / 1_000_000} ms")
                    _stateEventsOfDate.emit(allKeys) // Emit the new list of dates

                    //Log.e(TAG, "collectState: \n$organizedEvents", )
                    //Log.v(TAG, "collectState: \n$allKeys", )
                }
            }catch (e: Exception) {
                // Handle errors
                val error = e.message ?: "An unknown error occurred"
            }
        }
    }

    //todo:for event indicator showing in month view using map
    fun getEventIndicatorMapForMonth(year:String, month: String){
        Log.i(TAG, "getEventIndicatorMapForMonth: ")
        viewModelScope.launch {
            try {
                getEventIndicatorMapUseCase.invoke(year = year, month = month).collect{
                    //_stateEventsOfDateMap.emit(it)
                }
            }catch (e: Exception) {
                // Handle errors
                val error = e.message ?: "An unknown error occurred"
                Log.e(TAG, "getEventIndicatorMapForMonth: $error", e)
            }
        }
    }


    private val _yearState = MutableStateFlow<Int>(Calendar.getInstance().get(Calendar.YEAR))
    private val _monthState = MutableStateFlow<Int>(Calendar.getInstance().get(Calendar.MONTH))

    val yearState = _yearState
    val monthState = _monthState
    fun updateYear(year: Int) {
        Log.i(TAG, "updateYear: $year")
        viewModelScope.launch {
            _yearState.emit(year)
        }
    }

    fun updateMonth(month: Int) {
        Log.i(TAG, "updateMonth: $month")
        viewModelScope.launch {
            _monthState.emit(month)
        }
    }


}
//fetchAllEventsOfDateMap: execution time: 864115 ns, 864 µs, 0 ms
//repository.getEventsByYearAndMonth(year, month): execution time: 57292 ns, 57 µs, 0 ms