package com.hardik.calendarapp.presentation.ui.calendar_year_1

import android.Manifest
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.databinding.FragmentCalendarYear1Binding
import com.hardik.calendarapp.presentation.MainViewModel
import com.hardik.calendarapp.presentation.adapter.CalendarYearPageAdapter
import com.hardik.calendarapp.presentation.ui.MainActivity
import com.hardik.calendarapp.utillities.KeyboardUtils
import com.hardik.calendarapp.utillities.MyNavigation.navOptions
import com.hardik.calendarapp.utillities.PermissionHandler
import com.hardik.calendarapp.utillities.getCurrentYearPosition
import com.hardik.calendarapp.utillities.getYearKeyAtPosition
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@AndroidEntryPoint
class CalendarYear1Fragment : Fragment() {
    private val TAG = BASE_TAG + CalendarYear1Fragment::class.java.simpleName

    private val binding get() = _binding ?: throw IllegalStateException("Binding is only valid between onCreateView and onDestroyView")
    private var _binding: FragmentCalendarYear1Binding? = null
    private val viewModel: MainViewModel by activityViewModels()
    val adapter:CalendarYearPageAdapter by lazy { CalendarYearPageAdapter() }

    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCalendarYear1Binding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        (activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.includedYearView.includedBackToDate.root.setOnClickListener {
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
        (activity as MainActivity).run {
            this.binding.appBarMain.fab.setOnClickListener { view ->
                val navigateNewEvent = { findNavController().navigate(R.id.newEventFragment, null, navOptions) }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    val permissionNotification = Manifest.permission.POST_NOTIFICATIONS
                    if (PermissionHandler.checkPermission(requireActivity(), permissionNotification)) {
                        navigateNewEvent()
                    } else {
                        this.showNotificationPermissionDialog{
                            if (PermissionHandler.checkPermission(requireActivity(), permissionNotification)){
                                navigateNewEvent()}
                        }
                    }
                }else{
                    navigateNewEvent()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateSelectedDate("1999-0-0")//reset selected date for back from monthView

        if (::viewPager.isInitialized) { // Code for unselected data.
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
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.yearList.collectLatest{

                    adapter.updateYearList(it)

                    val yearPosition  = getCurrentYearPosition(currentYear = viewModel.yearState.value) // Calculate the position of the current year
                    if (::viewPager.isInitialized){
                        viewPager.setCurrentItem(yearPosition,false)
                    }
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
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.yearState.collectLatest{//collectLatest
                    binding.tvYearTitle.text = "$it"

                    val yearPosition  = getCurrentYearPosition(currentYear = it) // Calculate the position of the current year
                    viewPager.setCurrentItem(yearPosition,true)
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
        viewPager = binding.viewPagerCalendarYear
        viewPager.adapter = adapter

        lifecycleScope.launch(Dispatchers.Main) {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.yearState.collectLatest {
                    val previousPosition = getCurrentYearPosition(currentYear = it) // Update the previous position
                    // Update the viewPager's position when the year changes
                    viewPager.setCurrentItem(previousPosition, true)
                }
            }
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
            @SuppressLint("NotifyDataSetChanged")
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                setNextPrevBtnColor()

                // Get the key at the given position
                val yearKeyAtPosition = getYearKeyAtPosition(yearListMap = viewModel.yearList.value, position = position)
                if (yearKeyAtPosition != null) viewModel.updateYear(yearKeyAtPosition)
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
        lifecycleScope.launch(Dispatchers.Main) {
            val monthViewDate = "$year-$month-${0}"
            viewModel.updateMonthViewDate(monthViewDate)// update date and get that data for monthViewDate in monthView
            val bundle = Bundle().apply {
                putInt(Constants.KEY_YEAR, year)
                putInt(Constants.KEY_MONTH, month)
                putInt(Constants.KEY_DAY, 0)
            }
            findNavController().navigate(R.id.nav_month, bundle, navOptions)
        }
    }
}