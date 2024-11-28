package com.hardik.calendarapp.presentation.ui.calendar_month_1

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.common.Constants.KEY_MONTH
import com.hardik.calendarapp.common.Constants.KEY_YEAR
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.databinding.FragmentCalendarMonth1Binding
import com.hardik.calendarapp.domain.model.CalendarDayModel
import com.hardik.calendarapp.domain.repository.DateItemClickListener
import com.hardik.calendarapp.presentation.adapter.EventAdapter
import com.hardik.calendarapp.presentation.ui.MainActivity
import com.hardik.calendarapp.presentation.ui.calendar_month.CalendarMonthViewModel
import com.hardik.calendarapp.presentation.ui.calendar_month_1.adapter.*
import com.hardik.calendarapp.utillities.DateUtil.getFirstAndLastDateOfMonth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.joda.time.DateTime
import java.util.Calendar


@AndroidEntryPoint
class CalendarMonth1Fragment : Fragment(R.layout.fragment_calendar_month1), DateItemClickListener {
    private val TAG = BASE_TAG + CalendarMonth1Fragment::class.simpleName

    private val binding get() = _binding ?: throw IllegalStateException("Binding is only valid between onCreateView and onDestroyView")
    private var _binding: FragmentCalendarMonth1Binding? = null
    lateinit var toolbar:Toolbar
    private val viewModel: CalendarMonthViewModel by viewModels()
    private lateinit var eventAdapter: EventAdapter

    var year:Int=0
    var month:Int=0
    var date:Int=1

    private lateinit var viewPager: ViewPager2
    // When swipe happens, update the year in your adapter based on the position
    var previousPosition = 500 // todo: this is necessary to give previous position (which are you want)

    private var currDate = DateTime()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            year = it.getInt(KEY_YEAR)
            month = it.getInt(KEY_MONTH)
            date = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        }
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

    }

    override fun onDestroy() {
        lifecycleScope.coroutineContext.cancelChildren()
        super.onDestroy()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun setupUI(){
        toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
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
        }
    }
    private fun fetchEventsForSelectedMonth() {
        val (firstDayOfMonth, lastDayOfMonth) = if (year == 0 && month == 0) {
            val today = DateTime.now()
            getFirstAndLastDateOfMonth(today)
        } else {
            getFirstAndLastDateOfMonth(year = year, month = month + 1) // 1-based month
        }
        toolbar.title = "${if (year == 0) Calendar.getInstance().get(Calendar.YEAR) else year}"
        // Fetch events for the first and last dates from the database
        viewModel.fetchEventsForMonth(startOfMonth = firstDayOfMonth, endOfMonth = lastDayOfMonth)

    }
    companion object{
        var eventsOfDate: List<String> = emptyList<String>()
        var _eventsOfDateMap: List<Map<String, String>> = listOf(emptyMap())
    }
    @SuppressLint("NotifyDataSetChanged")
    private suspend fun observeViewModelState() {
        Log.d(TAG, "observeViewModelState: ")
        // Collecting the StateFlow
        lifecycleScope.launch {
            (requireActivity() as MainActivity).mainViewModel.stateEventsOfDate.collect { data ->
                eventsOfDate = data
                Log.v(TAG, "observeViewModelState: $eventsOfDate :size- ${eventsOfDate.size}")
            }
        }
        lifecycleScope.launch {
            viewModel.stateEventsOfDateMap.collect { data ->
                _eventsOfDateMap = data
                eventsOfDate  = data.flatMap { it.keys }.distinct()
            }
        }
        lifecycleScope.launch {
            viewModel.stateEventsOfMonth.collect { dataState ->
                val safeBinding = _binding // Safely reference the binding
                if (safeBinding != null) {

                    if (dataState.isLoading) {
                        // Show loading indicator
                        safeBinding.includedProgressLayout.progressBar.visibility = View.VISIBLE
                        Log.d(TAG, "observeViewModelState: Progressing")
                        safeBinding.tvNotify.visibility = View.GONE

                    } else if (dataState.error.isNotEmpty()) {
                        // Show error message
                        Toast.makeText(requireContext(), dataState.error, Toast.LENGTH_SHORT).show()
                        safeBinding.includedProgressLayout.progressBar.visibility = View.GONE
                        Log.d(TAG, "observeViewModelState: hide Progressing1")
                        safeBinding.tvNotify.text = dataState.error
                        safeBinding.tvNotify.visibility = View.VISIBLE

                    } else {
                        // Update UI with the user list
                        val data = dataState.data
                        safeBinding.rvEvent.visibility = if (data.isEmpty()) View.GONE else View.VISIBLE
                        safeBinding.tvNotify.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
                        Log.d(TAG, "observeViewModelState: hide Progressing2")

                        eventAdapter.updateData(data)
                        //eventsOfDate = data.map { it.startDate.getFormattedDate() }.distinct() // Remove duplicates
                        //eventAdapter.notifyDataSetChanged()
                        //binding.recyclerview.setPadding(0, 0, 0, 0)  // To remove the extra space on top and bottom of the RecyclerVie
                        safeBinding.includedProgressLayout.progressBar.visibility = View.GONE
                    }
                }else {
                    Log.w(TAG, "observeViewModelState: Binding is null, skipping UI update.")
                }
            }
        }

    }
    val pageAdapter = CalendarMonthPageAdapter()
    private suspend fun setupViewPager() {
        Log.d(TAG, "setupViewPager: ")
        yield()
        delay(300)
        viewPager = binding.viewPagerCalendarMonth

        viewPager.adapter = pageAdapter
        //adapter.updateEventsOfDateMap(dates = eventsOfDateMap)
        pageAdapter.updateEventsOfDate(eventsOfDate)
        //Todo: Start in the middle for infinite scrolling and set to the current month
        viewPager.setCurrentItem(previousPosition, true)

        // Set the year and month to the adapter
        pageAdapter.configureCustomView { it.apply {
            arguments?.let {// coming from year calendar to month
                this.currentYear = it.getInt(KEY_YEAR)
                this.currentMonth = it.getInt(KEY_MONTH)
                val (firstDayOfMonth, lastDayOfMonth) = getFirstAndLastDateOfMonth(year=currentYear,month=currentMonth+1)
                // Fetch events for the first and last dates from the database
                viewModel.fetchEventsForMonth(startOfMonth = firstDayOfMonth, endOfMonth = lastDayOfMonth)
                this.postInvalidate()
            }
            //addEvent("2024-0-26", Event(title = "", startTime = 0L, endTime = 0L, startDate = "", endDate = ""))
        }}

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
                    pageAdapter.apply {
                        configureCustomView {
                            it.apply {
                                incrementMonth() // Increment the month
                                val (firstDayOfMonth, lastDayOfMonth) = getFirstAndLastDateOfMonth(year=currentYear,month=currentMonth+1)
                                // Fetch events for the first and last dates from the database
                                viewModel.fetchEventsForMonth(startOfMonth = firstDayOfMonth, endOfMonth = lastDayOfMonth)
                                this.postInvalidate()
                                toolbar.title = "$currentYear"
                            }
                            it.postInvalidate()
                        }
                    }
                }
                if (position < previousPosition) {
                    Log.d(TAG, "onPageSelected: B")
                    // Swiped left: decrement month
                    pageAdapter.apply {
                        configureCustomView {
                            it.apply {
                                decrementMonth() // Decrement the month
                                val (firstDayOfMonth, lastDayOfMonth) = getFirstAndLastDateOfMonth(year=currentYear,month=currentMonth+1)
                                // Fetch events for the first and last dates from the database
                                viewModel.fetchEventsForMonth(startOfMonth = firstDayOfMonth, endOfMonth = lastDayOfMonth)
                                this.postInvalidate()
                                toolbar.title = "$currentYear"
                            }
                            it.postInvalidate()
                        }
                    }
                }
                pageAdapter.notifyDataSetChanged()

                // Update previous position to current one for next swipe comparison
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

}
