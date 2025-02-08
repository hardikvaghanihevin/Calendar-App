package com.hardik.calendarapp.presentation.ui.calendar_month_1

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.View
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
import com.hardik.calendarapp.data.database.entity.DayKey
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.data.database.entity.EventValue
import com.hardik.calendarapp.data.database.entity.MonthKey
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
import com.hardik.calendarapp.utillities.MyNavigation.navOptions
import com.hardik.calendarapp.utillities.findIndexOfYearMonth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.util.Calendar


@Suppress("NAME_SHADOWING")
@AndroidEntryPoint
class CalendarMonth1Fragment : Fragment(R.layout.fragment_calendar_month1) {
    private val TAG = BASE_TAG + CalendarMonth1Fragment::class.simpleName

    private val binding get() = _binding ?: throw IllegalStateException("Binding is only valid between onCreateView and onDestroyView")
    private var _binding: FragmentCalendarMonth1Binding? = null

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var eventAdapter: EventAdapter
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
    }

    @SuppressLint("NotifyDataSetChanged")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentCalendarMonth1Binding.bind(view)
        viewPager = binding.viewPagerCalendarMonth
        viewPager.adapter = pageAdapter

        CoroutineScope(Dispatchers.Main).launch {
            // Observe and update the event list
            observeViewModelState()

            //setupViewPager()

            setupUI()

        }


        /** Back to current month */
        (activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.backToDateIcon.setOnClickListener {
            lifecycleScope.launch {
                CoroutineScope(Dispatchers.Main).launch {
                    viewModel.findMonthViewPos.collect{
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
    }

    override fun onResume() {
        super.onResume()
        CoroutineScope(Dispatchers.Main).launch {
            setupViewPager()
        }
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
        binding.rvEvent.adapter = null
        _binding = null
    }

    @SuppressLint("SetTextI18n")
    private fun setupUI(){
        viewModel.updateTvMonthTitle(tvMTitle = DateFormatSymbols().months[month]+" " + year)
        binding.apply {
            tvMonthTitle.apply {
                setOnClickListener {
                    val d: CharSequence = tvMonthTitle.text
                    if (d.isNotEmpty()) {
                        val (y, m) = reverseYearMonth(d.toString()) ?: Pair(-1, -1)
                        viewModel.getEventsByMonthOfYear( year = y.toString() , month = m.toString() )
                    }
                }
            }

            btnPrevMonth.apply {
                setOnClickListener { navigateToMonth(-1) }
            }

            btnNextMonth.apply {
                setOnClickListener { navigateToMonth(1) }
            }

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
            eventAdapter = EventAdapter()
            binding.rvEvent.adapter = eventAdapter
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
            findNavController().navigate(R.id.viewEventFragment, bundle, navOptions)
        }
    }

    var _eventsOfDateMap: MutableMap<YearKey, MutableMap<MonthKey, MutableMap<DayKey, EventValue>>> = mutableMapOf()

    @SuppressLint("NotifyDataSetChanged")
    private suspend fun observeViewModelState() {
        // Collecting the StateFlow
        lifecycleScope.launch {
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

        lifecycleScope.launch{
            viewModel.monthViewDate.collectLatest {monthDate ->
                val date: Triple<String, String, String> = stringToDateTriple(monthDate, isZeroBased = false)

                if (monthDate != "2000-0-0"){
                    year = date.first.toInt()
                    month = date.second.toInt()
                    day = date.third.toInt()
                }

            }
        }

        lifecycleScope.launch {
            viewModel.tvMonthTitle.collectLatest { tvMonthTitle ->
                binding.tvMonthTitle.text = tvMonthTitle
            }
        }


        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.yearList.collectLatest{
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
                yearMonthPairList = it
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.allEventsDateInMapState.collectLatest { data: MutableMap<YearKey, MutableMap<MonthKey, MutableMap<DayKey, EventValue>>> ->
                launch(Dispatchers.Main) {
                    _eventsOfDateMap = data
                    pageAdapter.updateEventsOfDate(_eventsOfDateMap)
                }
            }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            delay(100)
            viewModel.monthlyEventsState.collectLatest { dataState ->
                val safeBinding = _binding // Safely reference the binding
                if (safeBinding != null) {

                    if (dataState.isLoading) {
                        // Show loading indicator
                        safeBinding.includedProgressLayout.progressBar.visibility = View.VISIBLE
                        safeBinding.rvEvent.visibility = View.VISIBLE
                        safeBinding.tvNotify.visibility = View.GONE

                    } else if (dataState.error.isNotEmpty()) {
                        // Show error message
                        Toast.makeText(requireContext(), dataState.error, Toast.LENGTH_SHORT).show()
                        safeBinding.includedProgressLayout.progressBar.visibility = View.GONE
                        safeBinding.rvEvent.visibility = View.VISIBLE
                        safeBinding.tvNotify.apply {
                            text = dataState.error
                            visibility = View.VISIBLE
                        }


                    } else {
                        // Update UI with the user list
                        val data = dataState.data
                        safeBinding.rvEvent.visibility = if (data.isEmpty()) View.GONE else View.VISIBLE
                        safeBinding.tvNotify.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE

                        viewModel.firstEventOfEachWeek.collectLatest {

                            eventAdapter.apply {
                                updateData(data, it)
                                this.notifyDataSetChanged()
                            }
                            safeBinding.includedProgressLayout.progressBar.visibility = View.GONE
                        }
                    }
                }else {
                    // Binding is null, skipping UI update.
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

        viewPager.adapter = pageAdapter
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
            customViewMonth.getMonthNameClickListener{ year: YearKey, month: MonthKey ->
                viewModel.getEventsByMonthOfYear(year = year, month = month)
            }

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
                val (year, month) = yearMonthPairList[position]

                val tvMonthTitle = DateFormatSymbols().months[month]+" " + year
                viewModel.updateTvMonthTitle(tvMTitle = tvMonthTitle)

                viewModel.updateSelectedDate(selectedDate!!)
                viewModel.updateMonthViewDate("$year-$month-${0}")

                selectedDate?.let {it:String ->
                    val date: Triple<String, String, String> = stringToDateTriple(it, isZeroBased = false)
                    if (year.toString() == date.first && month.toString() == date.second){
                        val findDateDataB1 = "$year-$month-${date.third}"
                        viewModel.fetchEventsForMonthView(findDateDataB1)
                    } else {
                        viewModel.getEventsByMonthOfYear(year = year.toString(), month = month.toString() )
                    }
                } ?: viewModel.getEventsByMonthOfYear(year = year.toString(), month = month.toString() )
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
