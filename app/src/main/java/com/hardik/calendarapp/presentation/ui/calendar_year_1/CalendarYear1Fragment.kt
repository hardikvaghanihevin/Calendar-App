package com.hardik.calendarapp.presentation.ui.calendar_year_1

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.common.Constants.KEY_MONTH
import com.hardik.calendarapp.common.Constants.KEY_YEAR
import com.hardik.calendarapp.databinding.FragmentCalendarYear1Binding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch


@AndroidEntryPoint
class CalendarYear1Fragment : Fragment(R.layout.fragment_calendar_year1) {
  private val TAG = BASE_TAG + CalendarYear1Fragment::class.java.simpleName

    private val binding get() = _binding ?: throw IllegalStateException("Binding is only valid between onCreateView and onDestroyView")
    private var _binding: FragmentCalendarYear1Binding? = null
    private var toolbar: Toolbar? = null

    private lateinit var viewPager: ViewPager2
    // When swipe happens, update the year in your adapter based on the position
    var previousPosition = 50 // todo: this is necessary to give previous position (which are you want)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentCalendarYear1Binding.bind(view)
        setupUI()

        setupViewPager()
    }

    override fun onDestroy() {
        lifecycleScope.coroutineContext.cancelChildren()
        super.onDestroy()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupUI() {
        toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
    }
    private fun setupViewPager(){
        viewPager = binding.viewPagerCalendarYear
        Log.e(TAG, "setupViewPager: ${toolbar == null} ", )

        val adapter = CalendarYearPageAdapter()
        viewPager.adapter = adapter

        // Set offscreen page limit to manage how many pages are in memory
        //viewPager.offscreenPageLimit = 1

        viewPager.setCurrentItem(previousPosition,true)
        toolbar?.apply { title = "$2022" }

        //set the year calendar
        adapter.setObjectOfCustomViewYear { it.apply {
            setOnMonthClickListener { year, month ->
                //this.yearTextView.text = "$year"
                //this.currentYear = year
                //this.postInvalidate()
                Toast.makeText(context,"$year - $month" , Toast.LENGTH_SHORT).show()
                navigateToCalendarMonth(year=year, month=month)
            }
            toolbar?.apply { title = "$currentYear" }
        } }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
            @SuppressLint("NotifyDataSetChanged")
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                if (position > previousPosition) {
                    // Swiped right: increment month
                    adapter.setObjectOfCustomViewYear {
                        it.apply {
                            toolbar?.title = "${incrementYear()}"
                            updateYearAndMonths()
                            setOnMonthClickListener { year, month ->
                                Toast.makeText(context,"$year - $month" , Toast.LENGTH_SHORT).show()
                                //todo:here to go month view CalendarMonth1Fragment
                                navigateToCalendarMonth(year=year, month=month)
                            }
                        }
                    }
                }
                if (position < previousPosition) {
                    // Swiped left: decrement month
                    adapter.setObjectOfCustomViewYear {
                        it.apply {
                            toolbar?.title = "${decrementYear()}"
                            updateYearAndMonths()
                            setOnMonthClickListener { year, month ->
                                Toast.makeText(requireContext(),"$year - $month" , Toast.LENGTH_SHORT).show()
                                Log.e(TAG, "onPageSelected: $year - $month", )
                                //todo:here to go month view CalendarMonth1Fragment
                                navigateToCalendarMonth(year=year, month=month)
                            }
                        }
                    }
                }
                // Post delayed update after layout phase
                viewPager.postDelayed({
                    adapter.notifyDataSetChanged()
                }, 5) // Small delay if necessary

                // Update previous position to current one for next swipe comparison
                previousPosition = position
            }
        })

    }
    private fun navigateToCalendarMonth(year: Int, month: Int) {
        lifecycleScope.launch {
            // Make sure the navigation happens on the main thread
            Log.e(TAG, "navigateToCalendarMonth:  ${Thread.currentThread().name}", )
            val bundle = Bundle().apply {
                putInt(KEY_YEAR, year)
                putInt(KEY_MONTH, month)
            }
            val action = CalendarYear1FragmentDirections.actionCalendarYear1FragmentToCalendarMonth1Fragment()
            findNavController().navigate(R.id.calendarMonth1Fragment, bundle)
        }
    }
}