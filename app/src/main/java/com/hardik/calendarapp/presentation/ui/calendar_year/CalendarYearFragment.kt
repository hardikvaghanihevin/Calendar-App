package com.hardik.calendarapp.presentation.ui.calendar_year

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.databinding.FragmentCalendarYearBinding
import com.hardik.calendarapp.domain.model.CalendarDayModel
import com.hardik.calendarapp.domain.repository.DateItemClickListener
import com.hardik.calendarapp.presentation.ui.calendar_year.adapter.CalendarYearPagerAdapter
import com.hardik.calendarapp.utillities.createDate
import com.hardik.calendarapp.utillities.isItToday
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import javax.inject.Inject

@SuppressLint("SetTextI18n")
@AndroidEntryPoint
class CalendarYearFragment @Inject constructor() : Fragment(R.layout.fragment_calendar_year), DateItemClickListener
{
    private final val TAG = BASE_TAG + CalendarYearFragment::class.java.simpleName

    private var _binding: FragmentCalendarYearBinding? = null
    private val binding get() = _binding!!


    private var currDate = DateTime()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCalendarYearBinding.bind(view)

        binding.apply {
//            setCalendarData()
            setYearWiseCalendarData()

            btnNextYear.setOnClickListener {
                navigateToYear(1)
            }
            btnPrevYear.setOnClickListener {
                navigateToYear(-1)
            }
        }
    }

    private fun setYearWiseCalendarData() {
        Log.d(TAG, "setYearWiseCalendarData: ")
        CoroutineScope(Dispatchers.IO).launch {

            val yearsData = mutableListOf<List<List<CalendarDayModel>>>() // List of years -> List of months -> List of days
            val initialCenterYearIndex = 5 // Arbitrary starting year index (5 years before and after current year)

            // Populate yearsData with data for previous, current, and future years
            for (i in -initialCenterYearIndex..initialCenterYearIndex) {
                val yearDate = currDate.plusYears(i)
                val yearData = setYearCalendar(yearDate) // Use setYearCalendar to generate data for the year
                yearsData.add(yearData)
            }

            launch(Dispatchers.Main) {

                // Initialize YearPagerAdapter with years data and set it to ViewPager
                val yearPageAdapter = CalendarYearPagerAdapter(yearsData, this@CalendarYearFragment)
                binding.viewPagerCalendarYear.adapter = yearPageAdapter

                // Set the current year displayed in the center of the dataset
                binding.viewPagerCalendarYear.setCurrentItem(initialCenterYearIndex, false)
                updateYearDisplay(currDate)

                // Add page change listener for infinite scrolling
                binding.viewPagerCalendarYear.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        val displayedYearDate = currDate.plusYears(position - initialCenterYearIndex)
                        updateYearDisplay(displayedYearDate)

                        Log.e(TAG, "setYearWiseCalendarData: yearsData:${yearsData.size}")
                    }
                })
            }
        }
    }

    private suspend fun setYearCalendar(currDate: DateTime): List<List<CalendarDayModel>> {
        Log.d(TAG, "setYearCalendar: for year ${currDate.year}")
        val list: Deferred<MutableList<List<CalendarDayModel>>> = CoroutineScope(Dispatchers.IO).async {

            val yearData = mutableListOf<List<CalendarDayModel>>()

            // Generate data for each month of the year
            for (month in 1..12) {
                val monthDate = currDate.withMonthOfYear(month)
                val monthData = setCalender(monthDate) // Reuse setCalender to generate month data
                yearData.add(monthData)
            }
            yearData

        }
        return list.await()
    }


    private suspend fun setCalender(currDate: DateTime): List<CalendarDayModel> {
//        Log.d(TAG, "setCalender: currDate: $currDate")
        val list = CoroutineScope(Dispatchers.IO).async {

            val dateList = mutableListOf<CalendarDayModel>()
            var date = currDate.withTime(0, 0, 0, 0)
            val numOfDaysInThisMonth = date.dayOfMonth().maximumValue
            date = date.minusDays(date.dayOfMonth)

            var dayOfWeek = date.dayOfWeek
            if (dayOfWeek == 7) dayOfWeek = 0

            for (i in 1..(numOfDaysInThisMonth + dayOfWeek)) {
                val model = if (i <= dayOfWeek) {
                    CalendarDayModel(0, "", -1)
                } else {
                    val dateTemp = createDate(i - dayOfWeek, currDate.monthOfYear, currDate.year)

                    // Check if the day is Sunday (Joda-Time's dayOfWeek uses 7 for Sunday)
                    val isHoliday = dateTemp!!.dayOfWeek == 7

                    when (isItToday(dateTemp)) {
                        0 -> CalendarDayModel(i - dayOfWeek, dateTemp.toLocalDate().toString(), 3, isHoliday = isHoliday) // past day
                        1 -> CalendarDayModel(i - dayOfWeek, dateTemp.toLocalDate().toString(), 2, isSelected = true, isHoliday = isHoliday) // today
                        else -> CalendarDayModel(i - dayOfWeek, dateTemp.toLocalDate().toString(), 1, isHoliday = isHoliday) // future day
                    }
                }
                dateList.add(model)
            }
                dateList
        }

        return list.await()
    }


    private fun updateYearDisplay(displayedDate: DateTime) {
        binding.tvCurrentYear.text = "${displayedDate.year}"
    }

    private fun navigateToYear(direction: Int) {
        // Update ViewPager position and display the new month and year
        val newPosition = binding.viewPagerCalendarYear.currentItem + direction
        binding.viewPagerCalendarYear.setCurrentItem(newPosition, true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDateClick(position: Int, calendarDayModel: CalendarDayModel) {

    }
}