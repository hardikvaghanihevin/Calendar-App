package com.hardik.calendarapp.presentation.ui.calendar_month_1

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import com.hardik.calendarapp.common.Constants.KEY_DAY
import com.hardik.calendarapp.common.Constants.KEY_EVENT
import com.hardik.calendarapp.common.Constants.KEY_MONTH
import com.hardik.calendarapp.common.Constants.KEY_YEAR
import com.hardik.calendarapp.data.database.entity.DayKey
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.data.database.entity.EventValue
import com.hardik.calendarapp.data.database.entity.MonthKey
import com.hardik.calendarapp.data.database.entity.YearKey
import com.hardik.calendarapp.data.database.entity.dummyEvent
import com.hardik.calendarapp.databinding.FragmentCalendarMonth1Binding
import com.hardik.calendarapp.presentation.MainViewModel
import com.hardik.calendarapp.presentation.adapter.EventAdapter1
import com.hardik.calendarapp.presentation.ui.MainActivity
import com.hardik.calendarapp.presentation.ui.calendar_month_1.adapter.*
import com.hardik.calendarapp.utillities.DateUtil.stringToDateTriple
import com.hardik.calendarapp.utillities.DisplayUtil.dpToPx
import com.hardik.calendarapp.utillities.MyNavigation.navOptions
import com.hardik.calendarapp.utillities.findIndexOfYearMonth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.text.DateFormatSymbols
import java.util.Calendar


@AndroidEntryPoint
class CalendarMonth1Fragment : Fragment(R.layout.fragment_calendar_month1) {
    private val TAG = BASE_TAG + CalendarMonth1Fragment::class.simpleName

    private val binding get() = _binding ?: throw IllegalStateException("Binding is only valid between onCreateView and onDestroyView")
    private var _binding: FragmentCalendarMonth1Binding? = null

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var eventAdapter: EventAdapter1
    private var yearMonthPairList: List<Pair<Int, Int>> = emptyList()
    private var yearList: Map<Int, Map<Int, List<Int>>> = emptyMap()
    var pageAdapter = CalendarMonthPageAdapter()

    var year: Int = Calendar.getInstance().get(Calendar.YEAR)
    var month: Int = Calendar.getInstance().get(Calendar.MONTH)
    var day: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

    var bundle: Bundle? = null

    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate: ")
        arguments?.let {
            year = it.getInt(KEY_YEAR)
            month = it.getInt(KEY_MONTH)//0 base month 0-11 (jan-dec0
            day = it.getInt(KEY_DAY)
            selectedDate = if(day == 0) null else "$year-$month-$day"//todo: when not get full date like [2024-0-'0'] set null
            Log.e(TAG, "onCreate: $selectedDate", )
        }

        viewModel.updateYear(year)
    }

    private lateinit var menuHost: MenuHost
    private lateinit var menuProvider: MenuProvider

    @SuppressLint("NotifyDataSetChanged")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "onViewCreated: ")


        _binding = FragmentCalendarMonth1Binding.bind(view)
        viewPager = binding.viewPagerCalendarMonth
        viewPager.adapter = pageAdapter

        // Fetch initial events for the month
        fetchEventsForSelectedMonth()

        CoroutineScope(Dispatchers.Main).launch {
            // Observe and update the event list
            observeViewModelState()

            setupViewPager()

            setupUI()

        }


        /** Back to current month */
        (activity as MainActivity).binding.backToDateIcon.setOnClickListener {
            val backToCurrentYear = Calendar.getInstance().get(Calendar.YEAR)
            val backToCurrentMonth = Calendar.getInstance().get(Calendar.MONTH)
            val currentMonthPosition = findIndexOfYearMonth(yearMonthPairList, backToCurrentYear, backToCurrentMonth)
            if (::viewPager.isInitialized) {
                viewPager.setCurrentItem(currentMonthPosition, true) // Navigate to the desired position
                pageAdapter.notifyDataSetChanged() // Refresh the adapter's data if necessary
            }

            // Get the position of the key in the yearList
            val yearKeyPos: Int = yearList.keys.toList().indexOf(backToCurrentYear)
            // Get the yearKey at the given position
            val yearKeyAtPosition = yearList.keys.toList().getOrNull(yearKeyPos)
            if (yearKeyAtPosition != null) viewModel.updateYear(yearKeyAtPosition)
            Log.d(TAG, "onMenuItemSelected: action_refresh: keyPosition:$yearKeyPos = year:$yearKeyAtPosition")

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

        bundle = (bundle ?: Bundle()).apply {
            putParcelable(KEY_EVENT, dummyEvent.copy( year = year.toString(), month = month.toString(), date = day.toString() ) )
        }
        // Check if arguments are present and contain the required keys
        if (arguments?.containsKey(KEY_YEAR) == true && arguments?.containsKey(KEY_MONTH) == true) {
            year = arguments?.getInt(KEY_YEAR) ?: Calendar.getInstance().get(Calendar.YEAR)
            month = arguments?.getInt(KEY_MONTH) ?: Calendar.getInstance().get(Calendar.MONTH)
            day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            //viewModel.updateYear(year)
        } else {
            // Fallback: No arguments, use the current year
            viewModel.updateYear(Calendar.getInstance().get(Calendar.YEAR))
        }

        _binding = null
    }


    @SuppressLint("SetTextI18n")
    private fun setupUI(){
        Log.i(TAG, "setupUI: ")
        binding.apply {
            tvMonthTitle.text = DateFormatSymbols().months[month]+" " + year

            btnPrevMonth.setOnClickListener {
                navigateToMonth(-1)
            }

            btnNextMonth.setOnClickListener {
                navigateToMonth(1)
            }

            //region Event handlers
            //endregion
            rvEvent.layoutManager = LinearLayoutManager(requireContext())
            rvEvent.setHasFixedSize(true)

            // Add a custom ItemDecoration to handle padding/margin
            rvEvent.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    //outRect.top = 0
                    //outRect.bottom = 0

                    val position = parent.getChildAdapterPosition(view)
                    val totalItemCount = state.itemCount

                    if (position == totalItemCount - 1) {
                        // Apply bottom padding/margin for the last item
                        outRect.bottom = 80.dpToPx()
                    } else {
                        // No additional padding/margin for other items
                        outRect.bottom = 0
                    }
                }
            })
            eventAdapter = EventAdapter1(ArrayList<Event>())
            binding.rvEvent.adapter = eventAdapter
            eventAdapter.updateFirstDayOfWeek()
            eventAdapter.setConfigureEventCallback {event:Event->
                // got event update
                navigateToViewEventFrag(event = event)
            }
            /*rvEvent.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val totalItemCount = layoutManager.itemCount
                    val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                    //todo: If we're at the last item, add bottom padding
                    if (lastVisibleItemPosition == totalItemCount - 1) {
                        recyclerView.setPadding(0, 0, 0, 80.dpToPx()) // Add padding at the bottom

                        // Add margin at the bottom
//                        val params = recyclerView.layoutParams as ViewGroup.MarginLayoutParams
//                        params.bottomMargin = 80.dpToPx() // Add margin at the bottom
//                        recyclerView.layoutParams = params

                    } else {
                        recyclerView.setPadding(0, 0, 0, 0) // Remove padding when not at the last item

                        // Remove margin when not at the last item
//                        val params = recyclerView.layoutParams as ViewGroup.MarginLayoutParams
//                        params.bottomMargin = 0 // Reset margin
//                        recyclerView.layoutParams = params
                    }
                }
            })*/

        }
    }
    private fun navigateToViewEventFrag(event: Event) {
        Log.i(TAG, "navigateToViewEventFrag: ")
        lifecycleScope.launch {
            // Make sure the navigation happens on the main thread
            Log.e(TAG, "navigateToViewEventFrag:  ${Thread.currentThread().name}",)
            bundle = (bundle ?: Bundle()).apply {
                putParcelable(KEY_EVENT, event)// Pass the event object
            }
            findNavController().navigate(R.id.viewEventFragment, bundle, navOptions)
            //findNavController().navigate(R.id.newEventFragment, bundle)
        }
    }
    private fun fetchEventsForSelectedMonth() {
        Log.i(TAG, "fetchEventsForSelectedMonth: ")
//        val (firstDayOfMonth, lastDayOfMonth) = if (year == 0 && month == 0) {
//            val today = DateTime.now()
//            getFirstAndLastDateOfMonth(today)
//        } else {
//            getFirstAndLastDateOfMonth(year = year, month = month + 1) // 1-based month
//        }
//        //todo: Fetch events for the first and last dates from the database
//        viewModel.getMonthlyEvents(startOfMonth = firstDayOfMonth, endOfMonth = lastDayOfMonth)
        if (arguments?.containsKey(KEY_YEAR) == true && arguments?.containsKey(KEY_MONTH) == true && arguments?.containsKey(KEY_DAY) == true) {
            viewModel.getEventsByDateOfMonthOfYear(year = year.toString(),month = month.toString(), date = day.toString())// Its come from jump to date

        }else{
            viewModel.getEventsByMonthOfYear(year = year.toString(),month = month.toString())//Its come from direct month fragment or year fragment
        }

    }

    var _eventsOfDateMap: MutableMap<YearKey, MutableMap<MonthKey, MutableMap<DayKey, EventValue>>> = mutableMapOf()

    @SuppressLint("NotifyDataSetChanged")
    private suspend fun observeViewModelState() {
        Log.i(TAG, "observeViewModelState: ")
        // Collecting the StateFlow
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.yearList.collectLatest{
                Log.e(TAG, "observeViewModelState: $it", )
                yearList = it
            }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED){
                viewModel.firstDayOfTheWeek.collectLatest { firstDay->
                    pageAdapter.updateFirstDayOfTheWeek(firstDay)
                    when(firstDay){
                        "Sunday" -> eventAdapter.updateFirstDayOfWeek(Calendar.SUNDAY)
                        "Monday" -> eventAdapter.updateFirstDayOfWeek(Calendar.MONDAY)
                        "Saturday" -> eventAdapter.updateFirstDayOfWeek(Calendar.SATURDAY)
                    }

                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.yearMonthPairList.collectLatest{
                Log.e(TAG, "observeViewModelState: $it", )
                yearMonthPairList = it
                val currentMonthPosition = findIndexOfYearMonth(yearMonthPairList, targetYear = year, targetMonth = month)
                launch(Dispatchers.Main) {
                    pageAdapter.updateYearMonthPairList(it)
                    viewPager.setCurrentItem(currentMonthPosition, false)
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.yearState.collectLatest{
                    Log.e(TAG, "observeViewModelState: $it", )
                    updateToolbarTitle("$it")
                    year = it
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.allEventsDateInMapState.collectLatest { data: MutableMap<YearKey, MutableMap<MonthKey, MutableMap<DayKey, EventValue>>> ->
                launch(Dispatchers.Main) {
                    _eventsOfDateMap = data
                    pageAdapter.updateEventsOfDate(_eventsOfDateMap)
                }
                //Log.v(TAG, "observeViewModelState: $_eventsOfDateMap")
            }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.monthlyEventsState.collectLatest { dataState ->
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


    private var selectedDate: String? = null
    private suspend fun setupViewPager() {
        Log.d(TAG, "setupViewPager: ")
        yield()

        // When swipe happens, update the year in your adapter based on the position
        val currentMonthPosition = findIndexOfYearMonth(yearMonthPairList, targetYear = year, targetMonth = month)
        var previousPosition = currentMonthPosition // todo: this is necessary to give previous position (which are you want)

        //viewPager = binding.viewPagerCalendarMonth
        //viewPager.offscreenPageLimit = 1//yearMonthPairList.size

        viewPager.adapter = pageAdapter
        pageAdapter.updateYearMonthPairList(yearMonthPairList)
        pageAdapter.updateEventsOfDate(_eventsOfDateMap)
        pageAdapter.setSelectedDate(selectedDate)
        //Todo: Start in the middle for infinite scrolling and set to the current month
        viewPager.setCurrentItem(previousPosition, false)

        //viewModel.updateYear(year)

        pageAdapter.configureCustomView {customViewMonth ->
            val monthName = customViewMonth.currentMonthName
            //binding.tvMonthTitle.text = monthName
            //customViewMonth.selectedDate = "2024-11-25"
            customViewMonth.getMonthNameClickListener{ year: YearKey, month: MonthKey ->
                viewModel.getEventsByMonthOfYear(year = year, month = month) }

            customViewMonth.getDateClickListener{triple: Triple<Rect, Canvas, String> ->
                Log.v(TAG, "setupViewPager: ${triple}", )
                val date: Triple<String, String, String> = stringToDateTriple(triple.third, isZeroBased = false)
                Log.v(TAG, "setupViewPager: A _selectedDate:${selectedDate}", )

                val clickedDate = triple.third
                // Update selected date
                //selectedDate = if (selectedDate == clickedDate) null else clickedDate
                lifecycleScope.launch {
                    viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED){
                        viewModel.selectedDate.collectLatest {
                            customViewMonth.selectedDate = it
                            selectedDate = it
                            Log.d(TAG, "setupViewPager: selectedDate: $selectedDate")
                        }
                    }
                }
                if (selectedDate == clickedDate) {
                    viewModel.getEventsByMonthOfYear(year = date.first, month = date.second)
                    viewModel.updateSelectedDate(null)
                    viewModel.updateYear(year= date.first.toInt())
                } else {
                    viewModel.getEventsByDateOfMonthOfYear(year = date.first, month = date.second, date = date.third)
                    viewModel.updateSelectedDate(clickedDate)
                    viewModel.updateYear(year= date.first.toInt())
                }
                Log.v(TAG, "setupViewPager: A _selectedDate:${selectedDate}", )

                return@getDateClickListener selectedDate
            }


        }

        // Register a callback to handle swipe events
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                // Retrieve year and month directly from yearMonthPairList
                val (year, month) = yearMonthPairList[position]
                binding.tvMonthTitle.text = DateFormatSymbols().months[month]+" " + year
                viewModel.updateYear(year)

                //val (firstDayOfMonth, lastDayOfMonth) = getFirstAndLastDateOfMonth(year = year, month = month +1)
                //viewModel.getMonthlyEvents(startOfMonth = firstDayOfMonth, endOfMonth = lastDayOfMonth)
                //pageAdapter.apply { configureCustomView { it.selectedDate = selectedDate} }
                viewModel.updateSelectedDate(selectedDate)
                pageAdapter.setSelectedDate(selectedDate)
                selectedDate?.let {it:String ->

                    val date: Triple<String, String, String> = stringToDateTriple(it, isZeroBased = false)

                    if (year.toString() == date.first && month.toString() == date.second)
                        viewModel.getEventsByDateOfMonthOfYear(year = date.first, month = date.second, date = date.third)
                    else viewModel.getEventsByMonthOfYear(year = year.toString(), month = month.toString() )

                } ?: viewModel.getEventsByMonthOfYear(year = year.toString(), month = month.toString() )


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

    private val toolbar: Toolbar? by lazy {
        requireActivity().findViewById<Toolbar>(R.id.toolbar)
    }
    private fun updateToolbarTitle(title: String) {
        toolbar?.title = resources.getString(R.string.app_name)//title
    }

}