package com.hardik.calendarapp.presentation.ui.calendar_year_1

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.common.Constants.KEY_MONTH
import com.hardik.calendarapp.common.Constants.KEY_YEAR
import com.hardik.calendarapp.databinding.FragmentCalendarYear1Binding
import com.hardik.calendarapp.presentation.MainViewModel
import com.hardik.calendarapp.presentation.ui.MainActivity.Companion.yearList
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch


@AndroidEntryPoint
class CalendarYear1Fragment : Fragment(R.layout.fragment_calendar_year1) {
    private val TAG = BASE_TAG + CalendarYear1Fragment::class.java.simpleName

    private val binding get() = _binding ?: throw IllegalStateException("Binding is only valid between onCreateView and onDestroyView")
    private var _binding: FragmentCalendarYear1Binding? = null
    private var toolbar: Toolbar? = null
    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var viewPager: ViewPager2

    companion object{
        var year:Int = 0
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentCalendarYear1Binding.bind(view)
        CoroutineScope(Dispatchers.Main).launch {
            val job = launch { setupUI() }
            job.join()
            Log.i(TAG, "onViewCreated: year: $year")
            val job1 = launch { setupViewPager() }

        }
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
        lifecycleScope.launch {
            // Safely collect yearState during STARTED state
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.yearState.collect{//collectLatest
                    toolbar?.title = "$it"
                    year = it
                    Log.i(TAG, "setupUI: year:$it")
                }
            }
        }
    }


    val adapter = CalendarYearPageAdapter(yearList)

    private fun setupViewPager(){
        Log.i(TAG, "setupViewPager: $year ")
        val currentYear = year
        val startYear = 2000
        val yearPosition = currentYear - startYear // Calculate the position of the current year

        // When swipe happens, update the year in your adapter based on the position
        // todo: this is necessary to give previous position (which are you want)
        var previousPosition = yearPosition // Track the previous position

        viewPager = binding.viewPagerCalendarYear
        viewPager.adapter = adapter

        // Set the current item to the calculated position of the current year
        viewPager.setCurrentItem(previousPosition, false)


        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
            @SuppressLint("NotifyDataSetChanged")
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                if (position > previousPosition) {
                    // Swiped right: increment month
                    viewModel.updateYear(year+1)

                } else if (position < previousPosition) {
                    // Swiped left: decrement month
                    viewModel.updateYear(year-1)
                }
                // Update previous position to current one for next swipe comparison
                previousPosition = position
            }
        })
        adapter.getYearMonth { mYear, mMonth ->
            navigateToCalendarMonth(year = mYear, month = mMonth)
        }

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
        // setOnMonthClickListener { year, month -> navigateToCalendarMonth(year=year, month=month)}
    }
}