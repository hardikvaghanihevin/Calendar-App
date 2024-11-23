package com.hardik.calendarapp.presentation.ui.calendar_month_1

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.databinding.FragmentCalendarMonth1Binding
import com.hardik.calendarapp.presentation.ui.calendar_month_1.adapter.CalendarMonthPageAdapter
import org.joda.time.LocalDate


class CalendarMonth1Fragment : Fragment(R.layout.fragment_calendar_month1) {
    private final val TAG = BASE_TAG + CalendarMonth1Fragment::class.java.simpleName

    private val binding get() = _binding!!
    private var _binding: FragmentCalendarMonth1Binding? = null

    val monthsData = mutableListOf<String>().apply { addAll(listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")) }

    val currentDate = LocalDate.now()  // Get the current date
    val currentMonth = currentDate.monthOfYear  // Get the current month (1-12)
    val currentYear = currentDate.year  // Get the current year

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentCalendarMonth1Binding.bind(view)

        Log.d(TAG, "onViewCreated: $currentDate, $currentMonth, $currentYear")
        binding.viewPagerCalendarMonth.apply { adapter = CalendarMonthPageAdapter(monthsData) }
        binding.viewPagerCalendarMonth.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                Log.d(TAG, "Selected month: ${monthsData[position]}")
            }
        })

             // Add more months for scrolling if needed}

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}