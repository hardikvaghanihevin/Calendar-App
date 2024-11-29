package com.hardik.calendarapp.presentation.ui.calendar_year_1

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
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


    var year:Int=0
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
                viewModel.yearState.collect{
                    toolbar?.title = "$it"
                    year = it
                    Log.i(TAG, "setupUI: year:$it")
                }
            }
        }
    }


    val adapter = CalendarYearPageAdapter()

    private fun setupViewPager(){
        Log.i(TAG, "setupViewPager: $year")
        // When swipe happens, update the year in your adapter based on the position
        var previousPosition = year // todo: this is necessary to give previous position (which are you want)
        viewPager = binding.viewPagerCalendarYear

        viewPager.adapter = adapter

        // Set offscreen page limit to manage how many pages are in memory
        //viewPager.offscreenPageLimit = 1

        viewPager.setCurrentItem(previousPosition,false)

        //set the year calendar
        adapter.apply {
            viewModel.updateYear(year)
            updateYear(year)
            setObjectOfCustomViewYear {
                it.apply {
                    setOnMonthClickListener { year, month ->
                        this.yearTextView.text = "$year"
                        this.currentYear = year
                        this.postInvalidate()
                        Toast.makeText(context,"$year - $month" , Toast.LENGTH_SHORT).show()
                        navigateToCalendarMonth(year=year, month=month)
                    }
                }
            }
        }

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
                                navigateToCalendarMonth(year=year, month=month)
                            }
                            viewModel.updateYear(currentYear)
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
                                Toast.makeText(requireContext(),"$year - $month" , Toast.LENGTH_SHORT).show()
                                Log.e(TAG, "onPageSelected: $year - $month", )
                                //todo:here to go month view CalendarMonth1Fragment
                                navigateToCalendarMonth(year=year, month=month)
                            }
                            viewModel.updateYear(currentYear)
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