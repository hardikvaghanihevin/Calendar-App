package com.hardik.calendarapp.presentation.ui.calendar_year_1

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
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
import com.hardik.calendarapp.utillities.KeyboardUtils
import com.hardik.calendarapp.utillities.MyNavigation.navOptions
import com.hardik.calendarapp.utillities.getCurrentYearPosition
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
    val adapter = CalendarYearPageAdapter()

    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentCalendarYear1Binding.bind(view)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Launch setupUI inside lifecycleScope
                if (isAdded){//TODO: Use isAdded check to confirm that the fragment is still attached.
                    setupUI()
                    // Launch setupViewPager after setupUI is complete
                    setupViewPager()
                }else {
                    // Fragment is not added, skipping repeatOnLifecycle
                }
            } catch (e: Exception) {
                //"Error during setup: ${e.message}"
            }

        }

        /** Back to current year */
        (activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.backToDateIcon.setOnClickListener {
//            val backToCurrentYear = Calendar.getInstance().get(Calendar.YEAR)
//            // Get the position of the key in the yearList
//            val yearKeyPos: Int? = getPositionFromYear(yearList, backToCurrentYear)
//            // Get the yearKey at the given position
//            val yearKeyAtPosition = yearKeyPos?.let { getYearKeyAtPosition(yearList, it) }
//            if (yearKeyAtPosition != null) viewModel.updateYear(yearKeyAtPosition)
            CoroutineScope(Dispatchers.Main).launch {
                viewModel.findYearViewPos.collect{
                    if (::viewPager.isInitialized) {
                        viewPager.setCurrentItem(it, true)  // Navigate to the desired position
                        adapter.notifyDataSetChanged() // Refresh the adapter's data if necessary
                    }
                }
            }
        }

        /** Go to newEvent */
        (activity as MainActivity).binding.appBarMain.fab.setOnClickListener { view ->
            findNavController().navigate(R.id.newEventFragment, null, navOptions)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateSelectedDate("2000-0-0")//reset selected date for back from monthView

        if (::viewPager.isInitialized) {
            //do code for unselected data.
            adapter.setSelectedDate(null)//"2025-1-5"
        }
        KeyboardUtils.hideKeyboard(requireActivity())
        requireActivity().invalidateOptionsMenu()
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

        lifecycleScope.launch(Dispatchers.Main) {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED){
                viewModel.yearList.collectLatest{

                    yearList = it
                    adapter.updateYearList(it)

                    /*val yearPosition  = getCurrentYearPosition(currentYear = year) // Calculate the position of the current year

                    viewPager.setCurrentItem(yearPosition,false)*/
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
                    binding.tvYearTitle.text = "$it"
                    year = it

                    val yearPosition  = getCurrentYearPosition(currentYear = it) // Calculate the position of the current year
                    viewPager.setCurrentItem(yearPosition,false)
                }
            }
        }

        binding.apply {
            btnPrevYear.apply {
                setOnClickListener { navigateToYear(-1) } }
            btnNextYear.apply {
                setOnClickListener { navigateToYear(1) }
            }
        }
    }


    private fun setupViewPager(){
        val yearPosition = getCurrentYearPosition(currentYear = year) // Calculate the position of the current year

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

                setNextPrevBtnColor()

                // Get the key at the given position
                val yearKeyAtPosition = getYearKeyAtPosition(yearList,position)
                if (yearKeyAtPosition != null) viewModel.updateYear(yearKeyAtPosition)

                // Update previous position to current one for next swipe comparison
                previousPosition = position
            }
        })
        adapter.getYearMonth { mYear, mMonth ->
            navigateToCalendarMonth(year = mYear, month = mMonth)
        }


    }

    //Set button color while reach last and first item of year
    private fun setNextPrevBtnColor() {
        val currentItem = binding.viewPagerCalendarYear.currentItem
        val itemCount = binding.viewPagerCalendarYear.adapter?.itemCount ?: 0
        val imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.text_secondary))

        binding.btnPrevYear.imageTintList = imageTintList.takeIf { currentItem == 0 } ?: ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.text_primary))
        binding.btnNextYear.imageTintList = imageTintList.takeIf { currentItem == itemCount -1 } ?: ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.text_primary))
    }

    private fun navigateToYear(direction: Int) {
        // Update ViewPager position and display the new month and year
        val newPosition = binding.viewPagerCalendarYear.currentItem + direction
        binding.viewPagerCalendarYear.setCurrentItem(newPosition, true)
    }

    private fun navigateToCalendarMonth(year: Int, month: Int) {
        lifecycleScope.launch {
            // Make sure the navigation happens on the main thread
            val bundle = Bundle().apply {
                putInt(KEY_YEAR, year)
                putInt(KEY_MONTH, month)
            }
            //findNavController().navigate(R.id.nav_month, bundle, navOptions)

            val monthViewDate = "$year-$month-${0}"
            viewModel.updateMonthViewDate(monthViewDate)//
            findNavController().navigate(R.id.nav_month, null, navOptions)
        }
    }
}