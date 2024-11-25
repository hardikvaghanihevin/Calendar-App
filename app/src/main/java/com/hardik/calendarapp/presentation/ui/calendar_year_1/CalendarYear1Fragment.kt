package com.hardik.calendarapp.presentation.ui.calendar_year_1

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.databinding.FragmentCalendarYear1Binding


class CalendarYear1Fragment : Fragment(R.layout.fragment_calendar_year1) {
  private val TAG = BASE_TAG + CalendarYear1Fragment::class.java.simpleName

    private val binding get() = _binding!!
    private var _binding: FragmentCalendarYear1Binding? = null

    private lateinit var viewPager: ViewPager2

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentCalendarYear1Binding.bind(view)
        viewPager = binding.viewPagerCalendarYear

        val adapter = CalendarYearPageAdapter()
        viewPager.adapter = adapter

        viewPager.setCurrentItem(previousPosition,true)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
            @SuppressLint("NotifyDataSetChanged")
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                if (position > previousPosition) {
                    // Swiped right: increment month
                    adapter.setObjectOfCustomViewYear {
                        it.apply {
                            incrementYear()
                            updateYearAndMonths()
                            setOnMonthClickListener { year, month ->
                                Toast.makeText(context,"$year - $month" , Toast.LENGTH_SHORT).show()
                                //todo:here to go month view CalendarMonth1Fragment
                            }
                        }
                    }
                }
                if (position < previousPosition) {
                    // Swiped left: decrement month
                    adapter.setObjectOfCustomViewYear {
                        it.apply {
                            decrementYear()
                            updateYearAndMonths()
                            setOnMonthClickListener { year, month ->
                                Toast.makeText(context,"$year - $month" , Toast.LENGTH_SHORT).show()
                                //todo:here to go month view CalendarMonth1Fragment
                            }
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
    var previousPosition = 50 // todo: this is necessary to give previous position (which are you want)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}