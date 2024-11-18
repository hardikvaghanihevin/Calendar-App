package com.hardik.calendarapp.presentation.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.domain.use_case.FetchEventsForMonthUseCase
import com.hardik.calendarapp.presentation.DataListState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val fetchEventsForMonthUseCase: FetchEventsForMonthUseCase) : ViewModel() {
    private val TAG = BASE_TAG + HomeViewModel::class.java.simpleName

    private val _text = MutableLiveData<String>().apply { value = "No Events" }
    val text: LiveData<String> = _text

    private val _stateEventsOfMonth = MutableStateFlow<DataListState<Event>>(DataListState(isLoading = true))
    val stateEventsOfMonth: StateFlow<DataListState<Event>> get() = _stateEventsOfMonth


    fun fetchEventsForMonth(startOfMonth: Long, endOfMonth: Long) {
        Log.d(TAG, "fetchEventsForMonth: ")
        // Set initial loading state
        _stateEventsOfMonth.value = DataListState(isLoading = true)

        viewModelScope.launch {
            try {
                fetchEventsForMonthUseCase.invoke(startOfMonth = startOfMonth, endOfMonth = endOfMonth)
                    .collect { events ->
                        // Update state with data
                        val dummyList = mutableListOf<Event>()
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
}
