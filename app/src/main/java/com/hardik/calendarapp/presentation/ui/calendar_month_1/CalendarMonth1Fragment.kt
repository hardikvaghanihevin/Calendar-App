package com.hardik.calendarapp.presentation.ui.calendar_month_1

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.common.Constants.KEY_EVENT
import com.hardik.calendarapp.common.Constants.KEY_MONTH
import com.hardik.calendarapp.common.Constants.KEY_YEAR
import com.hardik.calendarapp.data.database.entity.DayKey
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.data.database.entity.EventValue
import com.hardik.calendarapp.data.database.entity.MonthKey
import com.hardik.calendarapp.data.database.entity.YearKey
import com.hardik.calendarapp.databinding.FragmentCalendarMonth1Binding
import com.hardik.calendarapp.domain.model.CalendarDayModel
import com.hardik.calendarapp.domain.repository.DateItemClickListener
import com.hardik.calendarapp.presentation.MainViewModel
import com.hardik.calendarapp.presentation.adapter.EventAdapter
import com.hardik.calendarapp.presentation.ui.MainActivity
import com.hardik.calendarapp.presentation.ui.MainActivity.Companion.yearMonthPairList
import com.hardik.calendarapp.presentation.ui.calendar_month_1.adapter.*
import com.hardik.calendarapp.utillities.findIndexOfYearMonth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.joda.time.DateTime
import java.util.Calendar


@AndroidEntryPoint
class CalendarMonth1Fragment : Fragment(R.layout.fragment_calendar_month1), DateItemClickListener {
    private val TAG = BASE_TAG + CalendarMonth1Fragment::class.simpleName

    private val binding get() = _binding ?: throw IllegalStateException("Binding is only valid between onCreateView and onDestroyView")
    private var _binding: FragmentCalendarMonth1Binding? = null

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var eventAdapter: EventAdapter

    var year: Int = Calendar.getInstance().get(Calendar.YEAR)
    var month: Int = Calendar.getInstance().get(Calendar.MONTH)
    var day: Int = 1

    private lateinit var viewPager: ViewPager2
    private var currDate = DateTime()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            year = it.getInt(KEY_YEAR)
            month = it.getInt(KEY_MONTH)
            day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        }

        viewModel.updateYear(year)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentCalendarMonth1Binding.bind(view)
        setupUI()
        //setupEventHandling()

        // Fetch initial events for the month
        fetchEventsForSelectedMonth()

        CoroutineScope(Dispatchers.Main).launch {
            // Observe and update the event list
            observeViewModelState()

            setupViewPager()
        }

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
                        val backToCurrentMonth = Calendar.getInstance().get(Calendar.MONTH)
                        val currentMonthPosition = findIndexOfYearMonth(yearMonthPairList, backToCurrentYear, backToCurrentMonth)
                        // Get the position of the key in the yearList
                        val yearKeyPos: Int = MainActivity.yearList.keys.toList().indexOf(backToCurrentYear)
                        // Get the yearKey at the given position
                        val yearKeyAtPosition = MainActivity.yearList.keys.toList().getOrNull(yearKeyPos)
                        if (yearKeyAtPosition != null) viewModel.updateYear(yearKeyAtPosition)
                        Log.d(TAG, "onMenuItemSelected: action_refresh: keyPosition:$yearKeyPos = year:$yearKeyAtPosition")
                        if (::viewPager.isInitialized) {
                            viewPager.setCurrentItem(currentMonthPosition, true) // Navigate to the desired position
                            pageAdapter.notifyDataSetChanged() // Refresh the adapter's data if necessary
                        }
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED) // Add it for this fragment's lifecycle

    }

    override fun onResume() {
        super.onResume()
        requireActivity().invalidateOptionsMenu()
    }

    override fun onDestroy() {
        lifecycleScope.coroutineContext.cancelChildren()
        super.onDestroy()
    }
    override fun onDestroyView() {
        super.onDestroyView()

        // Check if arguments are present and contain the required keys
        if (arguments?.containsKey(KEY_YEAR) == true && arguments?.containsKey(KEY_MONTH) == true) {
            year = arguments?.getInt(KEY_YEAR) ?: Calendar.getInstance().get(Calendar.YEAR)
            month = arguments?.getInt(KEY_MONTH) ?: Calendar.getInstance().get(Calendar.MONTH)
            day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        } else {
            // Fallback: No arguments, use the current year
            viewModel.updateYear(Calendar.getInstance().get(Calendar.YEAR))
        }

        _binding = null
    }


    private fun setupUI(){
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
            eventAdapter = EventAdapter(ArrayList<Event>(), this@CalendarMonth1Fragment)
            binding.rvEvent.adapter = eventAdapter
            eventAdapter.setConfigureEventCallback {event:Event->
                // got event update
                navigateToNewEventFrag(event = event)
            }
        }
    }
    private fun navigateToNewEventFrag(event: Event) {
        lifecycleScope.launch {
            // Make sure the navigation happens on the main thread
            Log.e(TAG, "navigateToNewEventFrag:  ${Thread.currentThread().name}", )
            val bundle = Bundle().apply {
                putParcelable(KEY_EVENT, event)// Pass the event object
            }
            findNavController().navigate(R.id.newEventFragment, bundle)
        }
        // setOnMonthClickListener { year, month -> navigateToCalendarMonth(year=year, month=month)}
    }
    private fun fetchEventsForSelectedMonth() {
//        val (firstDayOfMonth, lastDayOfMonth) = if (year == 0 && month == 0) {
//            val today = DateTime.now()
//            getFirstAndLastDateOfMonth(today)
//        } else {
//            getFirstAndLastDateOfMonth(year = year, month = month + 1) // 1-based month
//        }
//        //todo: Fetch events for the first and last dates from the database
//        viewModel.getMonthlyEvents(startOfMonth = firstDayOfMonth, endOfMonth = lastDayOfMonth)
        viewModel.getEventsByMonthOfYear(year = year.toString(),month = month.toString())

    }

    var _eventsOfDateMap: MutableMap<YearKey, MutableMap<MonthKey, MutableMap<DayKey, EventValue>>> = mutableMapOf()

    @SuppressLint("NotifyDataSetChanged")
    private suspend fun observeViewModelState() {
        //Log.d(TAG, "observeViewModelState: ")
        // Collecting the StateFlow
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.yearState.collect{
                    Log.e(TAG, "observeViewModelState: $it", )
                    updateToolbarTitle("$it")
                    year = it
                }
            }
        }

        lifecycleScope.launch {
            viewModel.allEventsDateInMapState.collect { data: MutableMap<YearKey, MutableMap<MonthKey, MutableMap<DayKey, EventValue>>> ->
                _eventsOfDateMap = data
                //Log.v(TAG, "observeViewModelState: $_eventsOfDateMap")
            }
        }

        lifecycleScope.launch {
            viewModel.monthlyEventsState.collect { dataState ->
                val safeBinding = _binding // Safely reference the binding
                if (safeBinding != null) {

                    if (dataState.isLoading) {
                        // Show loading indicator
                        safeBinding.includedProgressLayout.progressBar.visibility = View.VISIBLE
                        //Log.d(TAG, "observeViewModelState: Progressing")
                        safeBinding.tvNotify.visibility = View.GONE

                    } else if (dataState.error.isNotEmpty()) {
                        // Show error message
                        Toast.makeText(requireContext(), dataState.error, Toast.LENGTH_SHORT).show()
                        safeBinding.includedProgressLayout.progressBar.visibility = View.GONE
                        //Log.d(TAG, "observeViewModelState: hide Progressing1")
                        safeBinding.tvNotify.text = dataState.error
                        safeBinding.tvNotify.visibility = View.VISIBLE

                    } else {
                        // Update UI with the user list
                        val data = dataState.data
                        safeBinding.rvEvent.visibility = if (data.isEmpty()) View.GONE else View.VISIBLE
                        safeBinding.tvNotify.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
                        //Log.d(TAG, "observeViewModelState: hide Progressing2")

                        eventAdapter.updateData(data)
                        //binding.recyclerview.setPadding(0, 0, 0, 0)  // To remove the extra space on top and bottom of the RecyclerVie
                        safeBinding.includedProgressLayout.progressBar.visibility = View.GONE
                    }
                }else {
                    Log.w(TAG, "observeViewModelState: Binding is null, skipping UI update.")
                }
            }
        }

    }

    val pageAdapter = CalendarMonthPageAdapter(yearMonthPairList)
    private suspend fun setupViewPager() {
        Log.d(TAG, "setupViewPager: ")
        yield()

        // When swipe happens, update the year in your adapter based on the position
        val currentMonthPosition = findIndexOfYearMonth(yearMonthPairList, year, month)
        var previousPosition = currentMonthPosition // todo: this is necessary to give previous position (which are you want)

        viewPager = binding.viewPagerCalendarMonth

        viewPager.adapter = pageAdapter
        pageAdapter.updateEventsOfDate(_eventsOfDateMap)
        //Todo: Start in the middle for infinite scrolling and set to the current month
        viewPager.setCurrentItem(previousPosition, true)

        viewModel.updateYear(year)

        // Register a callback to handle swipe events
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            @SuppressLint("NotifyDataSetChanged")
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // Retrieve year and month directly from yearMonthPairList
                val (year, month) = yearMonthPairList[position]
                viewModel.updateYear(year)
                //val (firstDayOfMonth, lastDayOfMonth) = getFirstAndLastDateOfMonth(year = year, month = month +1)
                //viewModel.getMonthlyEvents(startOfMonth = firstDayOfMonth, endOfMonth = lastDayOfMonth)
                viewModel.getEventsByMonthOfYear(year = year.toString(), month = month.toString() )

                // Log current position and year/month
                Log.d(TAG, "onPageSelected: Current Position = $position, Year = $year, Month = $month")

                // Determine swipe direction
                if (position > previousPosition) {
                    Log.d(TAG, "onPageSelected: Swiped Right (Next Month)")
                } else if (position < previousPosition) {
                    Log.d(TAG, "onPageSelected: Swiped Left (Previous Month)")
                }

                // Update previous position
                previousPosition = position
            }
        })
    }

    private fun navigateToMonth(direction: Int) {
        // Update ViewPager position and display the new month and year
        val newPosition = binding.viewPagerCalendarMonth.currentItem + direction
        binding.viewPagerCalendarMonth.setCurrentItem(newPosition, true)
    }
    override fun onDateClick(position: Int, calendarDayModel: CalendarDayModel) {}

    private val toolbar: Toolbar? by lazy {
        requireActivity().findViewById<Toolbar>(R.id.toolbar)
    }
    private fun updateToolbarTitle(title: String) {
        toolbar?.title = title
    }

}
