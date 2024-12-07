package com.hardik.calendarapp.common

data class DataListState<T>(
    val isLoading : Boolean = false,
    val data : List<T> = emptyList(),
    val error : String = "",
)
data class DataState<T>(
    val isLoading: Boolean = false,
    val data: T? = null,
    val error: String = ""
)