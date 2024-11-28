package com.hardik.calendarapp.presentation.ui.calendar_month

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.domain.use_case.FetchEventsForMonthUseCase
import com.hardik.calendarapp.domain.use_case.ObserveAllEventsUseCase
import com.hardik.calendarapp.presentation.DataListState
import com.hardik.calendarapp.utillities.DateUtil.getFormattedDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalendarMonthViewModel @Inject constructor(private val fetchEventsForMonthUseCase: FetchEventsForMonthUseCase, private val observeAllEventsUseCase : ObserveAllEventsUseCase) : ViewModel() {
    private val TAG = BASE_TAG + CalendarMonthViewModel::class.java.simpleName

    private val _text = MutableLiveData<String>().apply { value = "No Events" }
    val text: LiveData<String> = _text

    private val _stateEventsOfMonth = MutableStateFlow<DataListState<Event>>(DataListState(isLoading = true))
    val stateEventsOfMonth: StateFlow<DataListState<Event>> get() = _stateEventsOfMonth

    private val _stateEventsOfDateMap = MutableStateFlow<List<Map<String, String>>>(listOf(emptyMap()))
    val stateEventsOfDateMap: StateFlow<List<Map<String, String>>> get() = _stateEventsOfDateMap


    init { fetchAllEventsOfDateMap() }
    fun fetchEventsForMonth(startOfMonth: Long, endOfMonth: Long) {
        Log.i(TAG, "fetchEventsForMonth: ")
        // Set initial loading state
        _stateEventsOfMonth.value = DataListState(isLoading = true)

        viewModelScope.launch {
            try {
                fetchEventsForMonthUseCase.invoke(startOfMonth = startOfMonth, endOfMonth = endOfMonth)
                    .collect { events ->
                        // Update state with data
                        val dummyList = mutableListOf<Event>()
                        //repeat(10) { index -> dummyList.add(Event(title = "Dummy", startTime = 0L, endTime = 0L, startDate = "", endDate = "", isHoliday = false, description = "Dummy Event")) }
                        delay(500)//for showing progressbar
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

    fun fetchAllEventsOfDateMap(){
        Log.i(TAG, "fetchAllEventsOfDateMap: ")
        viewModelScope.launch {
            try {
                observeAllEventsUseCase.invoke().collect{events: List<Event> ->
                    //val evetns: List<String> = events.map { it.startDate.getFormattedDate() }.distinct() // Remove duplicates
                    // Convert List<Event> to a List<Map<String, String>>
                    val eventsMap: List<Map<String, String>> = events.map { event ->
                        mapOf(event.startDate.getFormattedDate() to event.startDate) // Create a map for each event
                    }

                    // Emit the resulting map
                    _stateEventsOfDateMap.emit(eventsMap)
                }
            }catch (e: Exception) {
                // Handle errors
                val error = e.message ?: "An unknown error occurred"
            }
        }
    }
}
