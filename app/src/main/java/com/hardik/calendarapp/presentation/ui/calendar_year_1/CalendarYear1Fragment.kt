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
import com.hardik.calendarapp.presentation.ui.MainActivity
import com.hardik.calendarapp.utillities.MyNavigation.navOptions
import com.hardik.calendarapp.utillities.getCurrentYearPosition
import com.hardik.calendarapp.utillities.getPositionFromYear
import com.hardik.calendarapp.utillities.getYearKeyAtPosition
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar


@AndroidEntryPoint
class CalendarYear1Fragment : Fragment(R.layout.fragment_calendar_year1) {
    private val TAG = BASE_TAG + CalendarYear1Fragment::class.java.simpleName

    private val binding get() = _binding ?: throw IllegalStateException("Binding is only valid between onCreateView and onDestroyView")
    private var _binding: FragmentCalendarYear1Binding? = null
    private val viewModel: MainViewModel by activityViewModels()
    private var year = Calendar.getInstance().get(Calendar.YEAR)
    var yearList: Map<Int, Map<Int, List<Int>>> = emptyMap()
    val adapter = CalendarYearPageAdapter(yearList)

    private lateinit var viewPager: ViewPager2

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "onViewCreated: ")

        _binding = FragmentCalendarYear1Binding.bind(view)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Launch setupUI inside lifecycleScope
                if (isAdded){//TODO: Use isAdded check to confirm that the fragment is still attached.
                    setupUI()
                    // Launch setupViewPager after setupUI is complete
                    setupViewPager()
                }else {
                    Log.d("LanguageFragment", "Fragment is not added, skipping repeatOnLifecycle")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during setup: ${e.message}")
            }

        }
/*
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Inflate the menu resource for the fragment
                menuInflater.inflate(R.menu.main, menu)
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_refresh -> {
                        val backToCurrentYear = Calendar.getInstance().get(Calendar.YEAR)
                        // Get the position of the key in the yearList
                        val yearKeyPos: Int? = getPositionFromYear(yearList,backToCurrentYear)
                        // Get the yearKey at the given position
                        val yearKeyAtPosition = yearKeyPos?.let { getYearKeyAtPosition(yearList, it) }
                        if (yearKeyAtPosition != null) viewModel.updateYear(yearKeyAtPosition)
                        //Log.d(TAG, "refreshToYear: $yearKeyPos = $yearKeyAtPosition")
                        if (::viewPager.isInitialized) {
                            if (yearKeyPos != null) { viewPager.setCurrentItem(yearKeyPos, true) } // Navigate to the desired position
                            adapter.notifyDataSetChanged() // Refresh the adapter's data if necessary
                        }
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED) // Add it for this fragment's lifecycle
*/
        /** Back to current year */
        (activity as MainActivity).binding.backToDateIcon.setOnClickListener {
            val backToCurrentYear = Calendar.getInstance().get(Calendar.YEAR)
            // Get the position of the key in the yearList
            val yearKeyPos: Int? = getPositionFromYear(yearList, backToCurrentYear)
            // Get the yearKey at the given position
            val yearKeyAtPosition = yearKeyPos?.let { getYearKeyAtPosition(yearList, it) }
            if (yearKeyAtPosition != null) viewModel.updateYear(yearKeyAtPosition)
            //Log.d(TAG, "refreshToYear: $yearKeyPos = $yearKeyAtPosition")
            if (::viewPager.isInitialized) {
                if (yearKeyPos != null) { viewPager.setCurrentItem(yearKeyPos, true) } // Navigate to the desired position
                adapter.notifyDataSetChanged() // Refresh the adapter's data if necessary
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume: ")
        requireActivity().invalidateOptionsMenu()
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy: ")
        lifecycleScope.coroutineContext.cancelChildren()
        super.onDestroy()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        Log.i(TAG, "onDestroyView: ")
        _binding = null
    }

    private fun setupUI() {
        Log.i(TAG, "setupUI: ")

        lifecycleScope.launch(Dispatchers.Main) {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.yearList.collectLatest{
                    Log.e(TAG, "observeViewModelState: $it", )

                    yearList = it
                    adapter.updateYearList(it)

                    val yearPosition  = getCurrentYearPosition(currentYear = year) // Calculate the position of the current year
                    Log.i(TAG, "setupUI: current year: $year")

                    viewPager.setCurrentItem(yearPosition,false)
                }
            }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED){
                viewModel.firstDayOfTheWeek.collectLatest { firstDay->
                    adapter.updateFirstDayOfTheWeek(firstDay)
                }
            }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            // Safely collect yearState during STARTED state
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.yearState.collectLatest{//collectLatest
                    Log.i(TAG, "setupUI: year: $it")
                    binding.tvYearTitle.text = "$it"
                    updateToolbarTitle("$it")
                    year = it
                }
            }
        }

        binding.apply {
            btnPrevYear.setOnClickListener {
                navigateToYear(-1)
            }
            btnNextYear.setOnClickListener {
                navigateToYear(1)
            }
        }
    }


    private fun setupViewPager(){
        Log.i(TAG, "setupViewPager: ")
        val yearPosition = getCurrentYearPosition(currentYear = year) // Calculate the position of the current year
        Log.i(TAG, "setupViewPager: $year")

        // When swipe happens, update the year in your adapter based on the position
        // todo: this is necessary to give previous position (which are you want)
        var previousPosition = yearPosition // Track the previous position

        viewPager = binding.viewPagerCalendarYear
        viewPager.adapter = adapter

        // Set the current item to the calculated position of the current year
        viewPager.setCurrentItem( previousPosition, false )


        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
            @SuppressLint("NotifyDataSetChanged")
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                // Get the key at the given position
                val yearKeyAtPosition = getYearKeyAtPosition(yearList,position)
                if (yearKeyAtPosition != null) viewModel.updateYear(yearKeyAtPosition)

                if (position > previousPosition) { // Swiped right: increment month
                    Log.d(TAG, "onPageSelected: Swiped Right (Next Year)")
                } else if (position < previousPosition) { // Swiped left: decrement month
                    Log.d(TAG, "onPageSelected: Swiped Left (Next Year)")
                }
                // Update previous position to current one for next swipe comparison
                previousPosition = position
            }
        })
        adapter.getYearMonth { mYear, mMonth ->
            navigateToCalendarMonth(year = mYear, month = mMonth)
        }


    }

    private fun navigateToYear(direction: Int) {
        // Update ViewPager position and display the new month and year
        val newPosition = binding.viewPagerCalendarYear.currentItem + direction
        binding.viewPagerCalendarYear.setCurrentItem(newPosition, true)
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
            findNavController().navigate(R.id.nav_month, bundle, navOptions)
        }
        // setOnMonthClickListener { year, month -> navigateToCalendarMonth(year=year, month=month)}
    }

    private val toolbar: Toolbar? by lazy {
        requireActivity().findViewById<Toolbar>(R.id.toolbar)
    }
    private fun updateToolbarTitle(title: String) {
        toolbar?.title = title
    }
}