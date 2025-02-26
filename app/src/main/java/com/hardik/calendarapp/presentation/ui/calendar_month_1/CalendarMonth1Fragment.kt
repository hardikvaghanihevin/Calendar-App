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
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.flow.debounce
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
    private val pageAdapter by lazy { CalendarMonthPageAdapter() }

    var year: Int = Calendar.getInstance().get(Calendar.YEAR)
    var month: Int = Calendar.getInstance().get(Calendar.MONTH)
    var day: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

    var bundle: Bundle? = null

    private lateinit var viewPager: ViewPager2

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

        savedInstanceState?.let {
            year = it.getInt("YEAR", Calendar.getInstance().get(Calendar.YEAR))
            month = it.getInt("MONTH", Calendar.getInstance().get(Calendar.MONTH))
            day = it.getInt("DAY", Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
            selectedDate = it.getString("SELECTED_DATE")

            //Log.v(TAG, "onViewStateRestored: $year $month $month - $selectedDate", )

            val monthViewDate = "$year-$month-${0}"
            viewModel.updateMonthViewDate(monthViewDate)

            selectedDate?.let { date ->
                viewModel.updateSelectedDate(date)
            }

        }

        
        CoroutineScope(Dispatchers.Main).launch {
            // Observe and update the event list
            observeViewModelState()

            delay(100)
            setupViewPager()

            setupEventRecycler()

        }

        /** Back to current month */
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
                    //viewModel.getEventsByMonthOfYear( year = y.toString() , month = m.toString() )
                    val sDate = "$y-$m-${0}"
                    viewModel.fetchEventsForMonthView(sDate = sDate)
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
//        pageAdapter.configureCustomView {
//            val d = it.selectedDate
//            val y = it.currentYear
//            val m = it.currentMonth
//
//            val date = d.takeIf { "2000-0-0" != it } ?: "$y-$m-${0}"
//            viewModel.fetchEventsForMonthView(date, "resume")
//            it.getDateClickListener { day:String ->
//                viewModel.updateSelectedDate(day)
//                return@getDateClickListener viewModel.selectedDate.value
//            }
//        }
        //Log.e(TAG, "onResume: ", )
        requireActivity().invalidateOptionsMenu()
    }

    override fun onDestroy() {
        lifecycleScope.coroutineContext.cancelChildren()
        super.onDestroy()
        // reset date for drawer navigation option 'month'
        val resetDate = "${DateUtil.getCurrentYear()}-${DateUtil.getCurrentMonth()}-${0}"
        viewModel.updateMonthViewDate(monthViewDate = resetDate)

    }
    override fun onDestroyView() {
        super.onDestroyView()
        val (y, m) = reverseYearMonth(viewModel.tvMonthTitle.value) ?: Pair(-1, -1)
        if (y != -1 && m != -1){
            val resetDate = "${y}-${m}-${0}"
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

            val itemCount = binding.rvEvent.adapter?.itemCount ?: 0
            if(itemCount != 0){
                // show RecyclerView
                rvEvent.visibility = View.VISIBLE
                tvNotify.visibility = View.GONE
                includedProgressLayout.progressBar.visibility = View.GONE
            }

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
            val visibility = if (event.sourceType in listOf(/*SourceType.CURSOR, */SourceType.REMOTE)) View.GONE else View.VISIBLE
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
            //viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                combine(viewModel.firstDayOfTheWeek, viewModel.monthlyEventsState.debounce(300)) { firstDay, dataState ->
                    Pair(firstDay, dataState)
                }.collectLatest { (firstDay, dataState) ->
                    when (firstDay) {
                        "Sunday" -> eventAdapter.updateFirstDayOfWeek(Calendar.SUNDAY)
                        "Monday" -> eventAdapter.updateFirstDayOfWeek(Calendar.MONDAY)
                        "Saturday" -> eventAdapter.updateFirstDayOfWeek(Calendar.SATURDAY)
                    }
                    handleDataState(dataState)
                }
            //}
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

            viewModel.firstEventOfEachWeek.collectLatest {

                delay(100)
                eventAdapter.apply {
                    updateData(data, it)
                    binding.includedProgressLayout.progressBar.visibility = View.GONE
                    binding.tvNotify.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun observeViewModelState() {
        //Log.e(TAG, "observeViewModelState: ", )
        // Collecting the StateFlow
        viewLifecycleOwner.lifecycleScope.launch {
            //viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED){

                launch {
                    viewModel.selectedDate.collectLatest {
                        selectedDate = it
                        Log.e(TAG, "observeViewModelState: date-($year-$month) = $it", )
                        pageAdapter.setSelectedDate(selectedDate)
                    }
                }

                launch{
                    viewModel.monthViewDate.collectLatest {monthDate ->
                        val date: Triple<String, String, String> = stringToDateTriple(monthDate, isZeroBased = false)
                        year = date.first.toInt()
                        month = date.second.toInt()
                        day = date.third.toInt()
                    }
                }

                launch {
                    viewModel.tvMonthTitle.collectLatest { tvMonthTitle ->
                        binding.tvMonthTitle.text = tvMonthTitle
                    }
                }

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
                        //Log.e(TAG, "observeViewModelState: $it", )
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

            //}
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

        pageAdapter.updateYearMonthPairList(yearMonthPairList)
        pageAdapter.updateEventsOfDate(_eventsOfDateMap)

        pageAdapter.configureCustomView {customViewMonth ->
            customViewMonth.getDateClickListener { day:String ->
                viewModel.updateSelectedDate(day)
                viewModel.fetchEventsForMonthView(day )
                return@getDateClickListener viewModel.selectedDate.value
            }
        }

        // Register a callback to handle swipe events
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                setNextPrevBtnColor()
                updateDataEventsAndMonthTitle(position)

            }
        })
    }
    private var isDateSetFromJump = false
    private fun updateDataEventsAndMonthTitle(position: Int) {
        val currentMonthPosition = selectedDate?.let {
            val date: Triple<String, String, String> = stringToDateTriple(it, isZeroBased = false)
            findIndexOfYearMonth(yearMonthPairList, targetYear = date.first.toInt(), targetMonth = date.second.toInt())
        }
        viewPager.postDelayed({
            val recyclerView = viewPager.getChildAt(0) as? RecyclerView
            val viewHolder = recyclerView?.findViewHolderForAdapterPosition(position) as? CalendarMonthPageAdapter.MonthViewHolder

            if (viewHolder != null) {
                //Log.d(TAG, "✅ Found ViewHolder for position: $position")
                viewHolder.binding.also {vh ->

                    year = vh.customView.currentYear
                    month = vh.customView.currentMonth
                    selectedDate = vh.customView.selectedDate
                    //Log.e(TAG, "updateDataEventsAndMonthTitle: $position $currentMonthPosition", )
                    if (viewModel.isFromJump.value){
                        if (position == currentMonthPosition){
                            vh.customView.selectedDate = selectedDate//null
                        }else{
                            viewModel.updateSelectedDate("1999-0-0")
                        }
                    }else{
                        viewModel.updateSelectedDate("1999-0-0")
                    }

                    val tvMonthTitle = resources.getStringArray(R.array.months)[month]+" " + year

                    viewModel.updateTvMonthTitle(tvMTitle = tvMonthTitle)

                    //viewModel.updateSelectedDate(selectedDate!!)
                    
                    val sdt = vh.customView.selectedDate
                    val ymdt = "$year-$month-${0}"
                    //Log.e(TAG, "updateDataEventsAndMonthTitle: $sdt | $ymdt", )

                    val date: Triple<String, String, String> = stringToDateTriple(sdt!!, isZeroBased = false)
                    val finalDate = if (date.first.toInt() == year && date.second.toInt() == month){ sdt }else{ ymdt }
                    Log.i(TAG, "updateDataEventsAndMonthTitle:fNL: $finalDate ------------------>", )
                    viewModel.fetchEventsForMonthView(finalDate )

                }
                // Access and modify views inside the ViewHolder here
            } else {
                //Log.e(TAG, "❌ ViewHolder not found for position: $position, will retry.")
            }
        }, 200) // Delay of 200ms to allow ViewPager2 to create the ViewHolder

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

    private val savedBundle: Bundle? = null
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.e(TAG, "onSaveInstanceState: ", )
        outState.putInt("YEAR", year)
        outState.putInt("MONTH", month)
        outState.putInt("DAY", day)
        selectedDate?.let { outState.putString("SELECTED_DATE", it) }
    }
}