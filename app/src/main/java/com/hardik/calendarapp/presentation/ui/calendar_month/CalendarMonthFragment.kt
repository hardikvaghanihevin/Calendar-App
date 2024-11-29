package com.hardik.calendarapp.presentation.ui.calendar_month

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.databinding.FragmentCalendarMonthBinding
import com.hardik.calendarapp.domain.model.CalendarDayModel
import com.hardik.calendarapp.domain.repository.DateItemClickListener
import com.hardik.calendarapp.presentation.MainViewModel
import com.hardik.calendarapp.presentation.adapter.EventAdapter
import com.hardik.calendarapp.presentation.ui.calendar_month.adapter.CalendarMonthPagerAdapter
import com.hardik.calendarapp.utillities.createDate
import com.hardik.calendarapp.utillities.getMonth
import com.hardik.calendarapp.utillities.isItToday
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import javax.inject.Inject

@SuppressLint("SetTextI18n")
@AndroidEntryPoint
class CalendarMonthFragment @Inject constructor() : Fragment(R.layout.fragment_calendar_month), DateItemClickListener {
    private final val TAG = Constants.BASE_TAG + CalendarMonthFragment::class.java.simpleName

    private val binding get() = _binding!!
    private var _binding: FragmentCalendarMonthBinding? = null

    private val viewModel: MainViewModel by viewModels()

    private lateinit var eventAdapter: EventAdapter

    private var currDate = DateTime()


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentCalendarMonthBinding.bind(view)

        binding.apply {

            btnPrevMonth.setOnClickListener {
                navigateToMonth(-1)
            }

            btnNextMonth.setOnClickListener {
                navigateToMonth(1)
            }

            //region Event handlers
            //endregion
            rvEvent.layoutManager = LinearLayoutManager(requireContext())
            rvEvent.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.top = 0
                    outRect.bottom = 0
                }
            })
            rvEvent.setHasFixedSize(true)
            eventAdapter = EventAdapter(ArrayList<Event>(), this@CalendarMonthFragment)
            binding.rvEvent.adapter = eventAdapter
        }
        getFirstAndLastDateOfMonth(currDate)
        // Initialize ViewPager and load calendar data for months
        setCalendarData()


        // Collecting the StateFlow
        lifecycleScope.launch {
            viewModel.monthlyEventsState.collect { dataState ->
                if (dataState.isLoading) {
                    // Show loading indicator
                    binding.includedProgressLayout.progressBar.visibility = View.VISIBLE
                    Log.d(TAG, "onCreate: Progressing")
                    binding.tvNotify.visibility = View.GONE

                } else if (dataState.error.isNotEmpty()) {
                    // Show error message
                    Toast.makeText(requireContext(), dataState.error, Toast.LENGTH_SHORT).show()
                    binding.includedProgressLayout.progressBar.visibility = View.GONE
                    Log.d(TAG, "onCreate: hide Progressing1")
                    binding.tvNotify.text = dataState.error
                    binding.tvNotify.visibility = View.VISIBLE

                } else {
                    // Update UI with the user list
                    val data = dataState.data

                    binding.rvEvent.visibility = View.VISIBLE
                    binding.tvNotify.visibility = data.takeIf { it.isEmpty() }?.let { binding.rvEvent.visibility= View.GONE; View.VISIBLE } ?: View.GONE
                    Log.d(TAG, "onCreate: hide Progressing2")

                    data.let {
                        it.let {
                            // Update the UI with the event list
                            eventAdapter.updateData(dataState.data)
                            //eventAdapter.notifyDataSetChanged()
                            //binding.recyclerview.setPadding(0, 0, 0, 0)  // To remove the extra space on top and bottom of the RecyclerView
                        }
                    }
                    binding.includedProgressLayout.progressBar.visibility = View.GONE
                }
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setCalendarData() {
        Log.d(TAG, "setCalendarData: ")
        val monthsData = mutableListOf<List<CalendarDayModel>>()
        val initialCenterIndex = 62  // Arbitrary starting index

        // Populate initial monthsData with 12 months before and after the current month
        for (i in -initialCenterIndex..initialCenterIndex) {
            val monthDate = currDate.plusMonths(i)
            val monthData = setCalender(monthDate) // Use setCalender to generate data
            monthsData.add(monthData)
        }

        // Initialize CalendarPagerAdapter with months data and set it to ViewPager
        val monthPageAdapter = CalendarMonthPagerAdapter(monthsData, this)
        binding.viewPagerCalendarMonth.adapter = monthPageAdapter

        // Set the current month displayed in the center of the dataset
        binding.viewPagerCalendarMonth.setCurrentItem(initialCenterIndex, false)
        updateMonthYearDisplay(currDate)

        // Add page change listener for infinite scrolling
        binding.viewPagerCalendarMonth.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {

                val displayedMonthDate = currDate.plusMonths(position - initialCenterIndex)
                updateMonthYearDisplay(displayedMonthDate)
                Log.e(TAG, "setCalendarData: monthsData:${monthsData.size}")


                // Add more months for scrolling if needed
                if (position > monthsData.size - 6) {
                    for (i in 1..6) {
                        monthsData.add(setCalender(currDate.plusMonths(monthsData.size - initialCenterIndex + i)))
                    }
                    monthPageAdapter.notifyItemRangeInserted(monthsData.size - 6, 6)
                }
                if (position < 6) {
                    for (i in 1..6) {
                        monthsData.add(0, setCalender(currDate.minusMonths(initialCenterIndex + i)))
                    }
                    monthPageAdapter.notifyItemRangeInserted(0, 6)
                    binding.viewPagerCalendarMonth.setCurrentItem(position + 6, false)
                }

                CoroutineScope(Dispatchers.Main).launch {
                    launch {
                        delay(350)
                        monthPageAdapter.setNotifyDataSetChanged()
                    }
                // Update first and last date when the month changes
                val (firstDate, lastDate) = getFirstAndLastDateOfMonth(displayedMonthDate)
                Log.d(TAG, "Updated First Date: $displayedMonthDate -> $firstDate, Last Date: $lastDate")
                }
            }
        })
    }

    private fun setCalender(currDate: DateTime): List<CalendarDayModel> {
        Log.d(TAG, "setCalender: ")
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

        return dateList
    }


    private fun navigateToMonth(direction: Int) {
        // Update ViewPager position and display the new month and year
        val newPosition = binding.viewPagerCalendarMonth.currentItem + direction
        binding.viewPagerCalendarMonth.setCurrentItem(newPosition, true)
    }


    private fun updateMonthYearDisplay(displayedDate: DateTime) {
        binding.tvMonthYear.text = "${getMonth(displayedDate.monthOfYear, requireContext())}, ${displayedDate.year}"
    }

    // Function to get the first and last date of the month in milliseconds (Long)
    private fun getFirstAndLastDateOfMonth(date: DateTime): Pair<Long, Long> {
        // First day of the current month at 00:00:00
        val firstDayOfMonth = date.withDayOfMonth(1).withTimeAtStartOfDay()

        // Last day of the current month at 23:59:59.999
        val lastDayOfMonth = date.withDayOfMonth(date.dayOfMonth().maximumValue).withTime(23, 59, 59, 999)

        // Fetch events for the first and last dates from the database
        viewModel.getMonthlyEvents(startOfMonth = firstDayOfMonth.millis, endOfMonth = lastDayOfMonth.millis)

        // Return the first and last dates as Pair<Long> (timestamps)
        return Pair(firstDayOfMonth.millis, lastDayOfMonth.millis)
    }


    override fun onDateClick(position: Int, calendarDayModel: CalendarDayModel) {}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}