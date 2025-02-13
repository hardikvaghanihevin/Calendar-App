package com.hardik.calendarapp.presentation.ui.calendar_month_1

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.viewpager2.widget.ViewPager2
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.common.Constants.KEY_EVENT
import com.hardik.calendarapp.common.DataListState
import com.hardik.calendarapp.data.database.entity.DayKey
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.data.database.entity.EventValue
import com.hardik.calendarapp.data.database.entity.MonthKey
import com.hardik.calendarapp.data.database.entity.SourceType
import com.hardik.calendarapp.data.database.entity.YearKey
import com.hardik.calendarapp.databinding.FragmentCalendarMonth1Binding
import com.hardik.calendarapp.presentation.MainViewModel
import com.hardik.calendarapp.presentation.adapter.EventAdapter
import com.hardik.calendarapp.presentation.ui.MainActivity
import com.hardik.calendarapp.presentation.ui.calendar_month_1.adapter.*
import com.hardik.calendarapp.utillities.DateUtil
import com.hardik.calendarapp.utillities.DateUtil.reverseYearMonth
import com.hardik.calendarapp.utillities.DateUtil.stringToDateTriple
import com.hardik.calendarapp.utillities.DisplayUtil.dpToPx
import com.hardik.calendarapp.utillities.DisplayUtil.hideViewWithAnimation
import com.hardik.calendarapp.utillities.DisplayUtil.showViewWithAnimation
import com.hardik.calendarapp.utillities.MyNavigation.navOptions
import com.hardik.calendarapp.utillities.findIndexOfYearMonth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.util.Calendar


@AndroidEntryPoint
class CalendarMonth1Fragment : Fragment(R.layout.fragment_calendar_month1) {
    private val TAG = BASE_TAG + CalendarMonth1Fragment::class.simpleName

    private var _binding: FragmentCalendarMonth1Binding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private val eventAdapter by lazy { EventAdapter() }
    private var yearMonthPairList: List<Pair<Int, Int>> = emptyList()
    //private var yearList: Map<Int, Map<Int, List<Int>>> = emptyMap()
    private val pageAdapter by lazy { CalendarMonthPageAdapter() }

    var year: Int = Calendar.getInstance().get(Calendar.YEAR)
    var month: Int = Calendar.getInstance().get(Calendar.MONTH)
    var day: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

    var bundle: Bundle? = null

    private lateinit var viewPager: ViewPager2

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        arguments?.let {
//            year = it.getInt(Constants.KEY_YEAR)
//            month = it.getInt(Constants.KEY_MONTH)//0 base month 0-11 (jan-dec0
//            day = it.getInt(Constants.KEY_DAY)
//            selectedDate = if (day == 0) null else "$year-$month-$day"//todo: when not get full date like [2024-0-'0'] set null
//        }
//    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCalendarMonth1Binding.inflate(inflater, container,false)
        return binding.root
    }
    @SuppressLint("NotifyDataSetChanged")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = binding.viewPagerCalendarMonth
        viewPager.adapter = pageAdapter
        binding.rvEvent.adapter = eventAdapter

        CoroutineScope(Dispatchers.Main).launch {
            // Observe and update the event list
            observeViewModelState()

            delay(100)
            setupViewPager()

            setupEventRecycler()

        }


        /** Back to current month */
        //(activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.backToDateIcon.setOnClickListener {
        (activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.includedMonthView.includedBackToDate.root.setOnClickListener {
            lifecycleScope.launch {
                CoroutineScope(Dispatchers.Main).launch {
                    viewModel.findMonthViewPos.collectLatest{
                        if (::viewPager.isInitialized) {
                            viewPager.setCurrentItem(it, true) // Navigate to the desired position
                        }
                    }
                }
            }


        }

        /** Go to newEvent */
        (activity as MainActivity).binding.appBarMain.fab.setOnClickListener { view ->

            findNavController().navigate(R.id.newEventFragment, null, navOptions)
        }

        /** Click on month title */
        binding.tvMonthTitle.apply {
            setOnClickListener {
                val d: CharSequence = binding.tvMonthTitle.text
                if (d.isNotEmpty()) {
                    val (y, m) = reverseYearMonth(d.toString()) ?: Pair(-1, -1)
                    viewModel.getEventsByMonthOfYear( year = y.toString() , month = m.toString() )
                }
            }
        }

        /** Click on prev button */
        binding.btnPrevMonth.apply {
            setOnClickListener { navigateToMonth(-1) }
        }

        /** Click on next button */
        binding.btnNextMonth.apply {
            setOnClickListener { navigateToMonth(1) }
        }


    }

    override fun onResume() {
        super.onResume()
        requireActivity().invalidateOptionsMenu()
    }

    override fun onDestroy() {
        lifecycleScope.coroutineContext.cancelChildren()
        super.onDestroy()

        //Log.e(TAG, "onDestroy: ", )
        // reset date for drawer navigation option 'month'
        val resetDate = "${DateUtil.getCurrentYear()}-${DateUtil.getCurrentMonth()}-${0}"
        viewModel.updateMonthViewDate(monthViewDate = resetDate)

    }
    override fun onDestroyView() {
        super.onDestroyView()
        val (y, m) = reverseYearMonth(viewModel.tvMonthTitle.value) ?: Pair(-1, -1)
        if (y != -1 && m != -1){
            val resetDate = "${y}-${m}-${0}"
            //Log.i(TAG, "onDestroyView: $y- $m", )
            viewModel.updateMonthViewDate(monthViewDate = resetDate)
        }
        _binding = null
    }

    @SuppressLint("SetTextI18n")
    private fun setupEventRecycler(){
        viewModel.updateTvMonthTitle(tvMTitle = DateFormatSymbols().months[month]+" " + year)
        binding.apply {

            rvEvent.layoutManager = LinearLayoutManager(requireContext())
            rvEvent.setHasFixedSize(true)
            (rvEvent.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

            val margin = resources.getDimension(R.dimen.itemCountryVerticalSpacing_dev2).toInt()

            // Add a custom ItemDecoration to handle padding/margin
            rvEvent.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val position = parent.getChildAdapterPosition(view)
                    val itemCount = state.itemCount

                    if(position == RecyclerView.NO_POSITION) return

                    when(position){
                        0 -> { // First item
                            outRect.top = margin
                            outRect.bottom = margin
                        }
                        itemCount - 1 -> { // Last item
                            outRect.top = margin
                            outRect.bottom = 80.dpToPx() //0
                        }
                        else -> { // Middle items
                            outRect.top = margin
                            outRect.bottom = margin
                        }
                    }
                }
            })

            observeViewModelState1()

            eventAdapter.updateFirstDayOfWeek()
            eventAdapter.setConfigureEventCallback {event:Event->
                // got event update
                navigateToViewEventFrag(event = event)
            }
        }
    }
    private fun navigateToViewEventFrag(event: Event) {
        lifecycleScope.launch {
            // Make sure the navigation happens on the main thread
            bundle = (bundle ?: Bundle()).apply {
                putParcelable(KEY_EVENT, event)// Pass the event object
            }

            //region Todo : this is for title and menu items for ViewEventsFragment
            val visibility = if (event.sourceType in listOf(SourceType.CURSOR, SourceType.REMOTE)) View.GONE else View.VISIBLE
            if (visibility == View.GONE) {
                hideViewWithAnimation((activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.llToolbarMenu, duration = 0)
            } else {
                showViewWithAnimation((activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.llToolbarMenu, duration = 0)
                showViewWithAnimation((activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.includedViewEvent.root, duration = 0)
                showViewWithAnimation((activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.includedViewEvent.manuItemViewEvent, duration = 0)
                showViewWithAnimation((activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.includedViewEvent.includedSave.root, duration = 0)
                showViewWithAnimation((activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.includedViewEvent.includedDelete.root, duration = 0)
            }
            //endregion

            findNavController().navigate(R.id.viewEventFragment, bundle, navOptions)
        }
    }

    var _eventsOfDateMap: MutableMap<YearKey, MutableMap<MonthKey, MutableMap<DayKey, EventValue>>> = mutableMapOf()

    @SuppressLint("NotifyDataSetChanged")
    private fun observeViewModelState1() {
        // Collecting the StateFlow
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                /*launch() {
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

                launch() {
                    viewModel.monthlyEventsState.collectLatest { dataState ->

                        if (dataState.isLoading) {
                            // Show loading indicator
                            binding.includedProgressLayout.progressBar.visibility = View.VISIBLE
                            binding.rvEvent.visibility = View.VISIBLE
                            binding.tvNotify.visibility = View.GONE

                        } else if (dataState.error.isNotEmpty()) {
                            // Show error message
                            Toast.makeText(requireContext(), dataState.error, Toast.LENGTH_SHORT).show()
                            binding.includedProgressLayout.progressBar.visibility = View.GONE
                            binding.rvEvent.visibility = View.VISIBLE
                            binding.tvNotify.apply {
                                text = dataState.error
                                visibility = View.VISIBLE
                            }


                        } else {
                            // Update UI with the user list
                            val data = dataState.data
                            binding.rvEvent.visibility = if (data.isEmpty()) View.GONE else View.VISIBLE
                            binding.tvNotify.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE

                            viewModel.firstEventOfEachWeek.collectLatest {

                                eventAdapter.apply {
                                    updateData(data, it)
                                    this.notifyDataSetChanged()
                                }
                                binding.includedProgressLayout.progressBar.visibility = View.GONE
                            }
                        }
                    }
                }*/
                combine(viewModel.firstDayOfTheWeek, viewModel.monthlyEventsState) { firstDay, dataState ->
                    Pair(firstDay, dataState)
                }.collectLatest { (firstDay, dataState) ->
                    when (firstDay) {
                        "Sunday" -> eventAdapter.updateFirstDayOfWeek(Calendar.SUNDAY)
                        "Monday" -> eventAdapter.updateFirstDayOfWeek(Calendar.MONDAY)
                        "Saturday" -> eventAdapter.updateFirstDayOfWeek(Calendar.SATURDAY)
                    }
                    handleDataState(dataState)
                }
            }
        }
    }

    private suspend fun handleDataState(dataState: DataListState<Event>) {
        if (dataState.isLoading) {
            // Show loading indicator
            binding.includedProgressLayout.progressBar.visibility = View.VISIBLE
            binding.tvNotify.visibility = View.GONE

        } else if (dataState.error.isNotEmpty()) {
            // Show error message
            Toast.makeText(requireContext(), dataState.error, Toast.LENGTH_SHORT).show()
            binding.includedProgressLayout.progressBar.visibility = View.GONE
            binding.tvNotify.apply {
                text = dataState.error
                visibility = View.VISIBLE
            }

        } else {
            // Update UI with the user list
            val data = dataState.data

            //binding.rvEvent.visibility = if (data.isEmpty()) View.GONE else View.VISIBLE
            binding.tvNotify.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE

            viewModel.firstEventOfEachWeek.collectLatest {

                eventAdapter.apply { updateData(data, it) }


                binding.includedProgressLayout.progressBar.visibility = View.GONE
            }
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun observeViewModelState() {
        // Collecting the StateFlow
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED){

                launch {
                    viewModel.selectedDate.collectLatest { selectedDate = it
                        viewModel.fetchEventsForMonthView(it)

                        val date: Triple<String, String, String> = stringToDateTriple(it, isZeroBased = false)
                        if (it.last() == '0' && !it.endsWith("10") && !it.endsWith("20") && !it.endsWith("30")){//0,10,20,30
                            viewModel.getEventsByMonthOfYear(year = date.first, month = date.second)
                        }else{
                            viewModel.getEventsByDateOfMonthOfYear(year = date.first, month = date.second, date = date.third)
                        }
                        pageAdapter.setSelectedDate(selectedDate)

                    }
                }

                launch{
                    viewModel.monthViewDate.collectLatest {monthDate ->
                        val date: Triple<String, String, String> = stringToDateTriple(monthDate, isZeroBased = false)
                        year = date.first.toInt()
                        month = date.second.toInt()
                        day = date.third.toInt()
                        //Log.i(TAG, "observeViewModelState: 1 $monthDate")
                    }
                }

                launch {
                    viewModel.tvMonthTitle.collectLatest { tvMonthTitle ->
                        binding.tvMonthTitle.text = tvMonthTitle
                    }
                }

//                launch(Dispatchers.IO) {
//                    viewModel.yearList.collectLatest{
//                        yearList = it
//                    }
//                }
                launch() {
                    viewModel.firstDayOfTheWeek.collectLatest { firstDay->
                        pageAdapter.updateFirstDayOfTheWeek(firstDay)
                        when(firstDay){
                            "Sunday" -> eventAdapter.updateFirstDayOfWeek(Calendar.SUNDAY)
                            "Monday" -> eventAdapter.updateFirstDayOfWeek(Calendar.MONDAY)
                            "Saturday" -> eventAdapter.updateFirstDayOfWeek(Calendar.SATURDAY)
                        }
                    }
                }

                launch() {
                    viewModel.yearMonthPairList.collectLatest{
                        yearMonthPairList = it
                    }
                }

                launch(Dispatchers.IO) {
                    viewModel.allEventsDateInMapState.collectLatest { data: MutableMap<YearKey, MutableMap<MonthKey, MutableMap<DayKey, EventValue>>> ->
                        launch(Dispatchers.Main) {
                            _eventsOfDateMap = data
                            pageAdapter.updateEventsOfDate(_eventsOfDateMap)
                        }
                    }
                }

            }
        }

    }


    private var selectedDate: String? = null
    private suspend fun setupViewPager() {

        lifecycleScope.launch(Dispatchers.Main){
            viewModel.yearMonthPairList.collectLatest {
                // When swipe happens, update the year in your adapter based on the position
                val currentMonthPosition = findIndexOfYearMonth(it, targetYear = year, targetMonth = month)

                //Todo: Start in the middle for infinite scrolling and set to the current month
                if (::viewPager.isInitialized){
                    viewPager.setCurrentItem(currentMonthPosition,false)
                }
            }
        }

        //viewPager.adapter = pageAdapter
        pageAdapter.updateYearMonthPairList(yearMonthPairList)
        pageAdapter.updateEventsOfDate(_eventsOfDateMap)
        pageAdapter.setSelectedDate(selectedDate)

        val findDateDataA = viewModel.selectedDate.value.let {it:String ->
            val date: Triple<String, String, String> = stringToDateTriple(it, isZeroBased = false)
            if (year.toString() == date.first && month.toString() == date.second){
                "$year-$month-${date.third}"
            } else {
                "$year-$month-${0}"
            }
        }
        viewModel.fetchEventsForMonthView(findDateDataA)

        pageAdapter.configureCustomView {customViewMonth ->
            customViewMonth.getDateClickListener { day:String ->
                viewModel.updateSelectedDate(day)
                return@getDateClickListener viewModel.selectedDate.value
            }
        }

        // Register a callback to handle swipe events
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                setNextPrevBtnColor()

                // Retrieve year and month directly from yearMonthPairList
                val (yr, mn) = yearMonthPairList[position]

                //val tvMonthTitle = DateFormatSymbols().months[mn]+" " + yr
                val tvMonthTitle = resources.getStringArray(R.array.months)[mn]+" " + yr

                Log.w(TAG, "onPageSelected: $tvMonthTitle", )
                viewModel.updateTvMonthTitle(tvMTitle = tvMonthTitle)

                viewModel.updateSelectedDate(selectedDate!!)
                //viewModel.updateMonthViewDate("$year-$month-${0}")

                selectedDate?.let {it:String ->
                    val date: Triple<String, String, String> = stringToDateTriple(it, isZeroBased = false)
                    if (yr.toString() == date.first && mn.toString() == date.second){
                        val findDateDataB1 = "$yr-$mn-${date.third}"
                        viewModel.fetchEventsForMonthView(findDateDataB1)
                    } else {
                        viewModel.getEventsByMonthOfYear(year = yr.toString(), month = mn.toString() )
                    }
                } ?: viewModel.getEventsByMonthOfYear(year = yr.toString(), month = mn.toString() )
            }
        })
    }

    //Set button color while reach last and first item of month
    private fun setNextPrevBtnColor() {
        val currentItem = binding.viewPagerCalendarMonth.currentItem
        val itemCount = binding.viewPagerCalendarMonth.adapter?.itemCount ?: 0
        val imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.text_secondary))

        binding.btnPrevMonth.imageTintList = imageTintList.takeIf { currentItem == 0 } ?: ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.text_primary))
        binding.btnNextMonth.imageTintList = imageTintList.takeIf { currentItem == itemCount -1 } ?: ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.text_primary))
    }

    private fun navigateToMonth(direction: Int) {
        // Update ViewPager position and display the new month and year
        val newPosition = binding.viewPagerCalendarMonth.currentItem + direction
        binding.viewPagerCalendarMonth.setCurrentItem(newPosition, true)
    }
}