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
import com.hardik.calendarapp.data.database.entity.organizeEvents
import com.hardik.calendarapp.domain.model.HolidayApiDetail
import com.hardik.calendarapp.domain.repository.EventRepository
import com.hardik.calendarapp.domain.use_case.GetAllEventsUseCase
import com.hardik.calendarapp.domain.use_case.GetHolidayApiUseCase
import com.hardik.calendarapp.domain.use_case.GetMonthlyEventsUseCase
import com.hardik.calendarapp.utillities.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getHolidayApiUseCase: GetHolidayApiUseCase,// For API compatibility
    private val eventRepository: EventRepository,// For Database compatibility
    private val getAllEventsUseCase : GetAllEventsUseCase,// For getting all events (indicator use)
    private val getMonthlyEventsUseCase: GetMonthlyEventsUseCase,// For getting monthly events compatibility (start to end date) (eventAdapter use)
) : ViewModel() {
    private val TAG = BASE_TAG + MainViewModel::class.java.simpleName

    private val _holidayApiState = MutableStateFlow<DataState<HolidayApiDetail>>(DataState(isLoading = true))
    val holidayApiState: StateFlow<DataState<HolidayApiDetail>> get() = _holidayApiState

    init {
        getHolidayCalendarData()
        getAllEventsDateInMap()
    }

    /**Get holiday list by using API*/
    private fun getHolidayCalendarData() {
        Log.i(TAG, "getHolidayCalendarData: ")
        viewModelScope.launch {
            getHolidayApiUseCase.invoke().collect { result: Resource<HolidayApiDetail> ->
                when (result) {
                    is Resource.Success -> {
                        _holidayApiState.value = DataState(data = result.data);
                        collectHolidayApiState()//all fetched events (form API)
                    }

                    is Resource.Error -> {
                        _holidayApiState.value =
                            DataState(error = result.message ?: "An unexpected error occurred")
                    }

                    is Resource.Loading -> {
                        _holidayApiState.value = DataState(isLoading = true)
                    }

                    else -> {}
                }
            }
        }
    }

    /**Observe [holidayApiState] after getting data from API*/
    private fun collectHolidayApiState() {//insert in to DB
        Log.i(TAG, "collectState: ")
        viewModelScope.launch {
            holidayApiState.collect { state ->
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
                                    id = "${DateUtil.stringToLong(item.start.date,DateUtil.DATE_FORMAT)} | ${item.summary}",
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

    private val _monthlyEventsState = MutableStateFlow<DataListState<Event>>(DataListState(isLoading = true))
    val monthlyEventsState: StateFlow<DataListState<Event>> get() = _monthlyEventsState
    // todo:for event showing below inside month view
    fun getMonthlyEvents(startOfMonth: Long, endOfMonth: Long) {
        ///Log.i(TAG, "fetchEventsForMonth: ")
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

}