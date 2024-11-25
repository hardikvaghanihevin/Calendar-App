package com.hardik.calendarapp.presentation.ui.calendar_month_1

import android.annotation.SuppressLint
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
import com.hardik.calendarapp.presentation.ui.calendar_month_1.adapter.*
import java.util.Calendar


class CalendarMonth1Fragment : Fragment(R.layout.fragment_calendar_month1) {
    private val TAG = BASE_TAG + CalendarMonth1Fragment::class.simpleName

    private val binding get() = _binding!!
    private var _binding: FragmentCalendarMonth1Binding? = null

    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentCalendarMonth1Binding.bind(view)
        viewPager = binding.viewPagerCalendarMonth

        val adapter = CalendarMonthPageAdapter()
        viewPager.adapter = adapter

        var month = Calendar.getInstance().get(Calendar.MONTH)
        // Start in the middle for infinite scrolling and set to the current month
        viewPager.setCurrentItem(previousPosition, true)

        // Register a callback to handle swipe events
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            @SuppressLint("NotifyDataSetChanged")
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                //Log.d(TAG, "onPageSelected: A ($position position),  $previousPosition ")

                // Check if swipe is to the right (next month) or left (previous month)
                if (position > previousPosition ) {
                    Log.d(TAG, "onPageSelected: A")
                    // Swiped right: increment month
                    adapter.setObjectOfCustomView {
                        it.apply {
                            incrementMonth() // Increment the month
                        }
                    }
                }
                if (position < previousPosition) {
                    Log.d(TAG, "onPageSelected: B")
                    // Swiped left: decrement month
                    adapter.setObjectOfCustomView {
                        it.apply {
                            decrementMonth() // Decrement the month
                        }
                    }
                }
                adapter.notifyDataSetChanged()

                // Update previous position to current one for next swipe comparison
                previousPosition = position
            }
        })
    }

    // When swipe happens, update the year in your adapter based on the position
    var previousPosition = 500 // todo: this is necessary to give previous position (which are you want)


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
