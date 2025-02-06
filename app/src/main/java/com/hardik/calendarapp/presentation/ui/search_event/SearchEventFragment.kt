package com.hardik.calendarapp.presentation.ui.search_event

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.databinding.FragmentSearchEventBinding
import com.hardik.calendarapp.presentation.MainViewModel
import com.hardik.calendarapp.presentation.adapter.EventAdapter
import com.hardik.calendarapp.presentation.ui.MainActivity
import com.hardik.calendarapp.utillities.DisplayUtil
import com.hardik.calendarapp.utillities.DisplayUtil.hideViewWithAnimation
import com.hardik.calendarapp.utillities.DisplayUtil.showViewWithAnimation
import com.hardik.calendarapp.utillities.KeyboardUtils
import com.hardik.calendarapp.utillities.MyNavigation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar


class SearchEventFragment : Fragment(R.layout.fragment_search_event) {
    private val TAG = BASE_TAG + SearchEventFragment::class.simpleName

    private val binding get() = _binding ?: throw IllegalStateException("Binding is only valid between onCreateView and onDestroyView")
    private var _binding: FragmentSearchEventBinding? = null

    private val viewModel: MainViewModel by activityViewModels()
    private val eventAdapter: EventAdapter = EventAdapter()
    private var currentQuery: String? = null // Variable to store the current query for search

    var bundle: Bundle? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? { return inflater.inflate(R.layout.fragment_search_event, container, false) }

    @SuppressLint("NotifyDataSetChanged")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSearchEventBinding.bind(view)

        viewModel.getAllEvents()

        setupUI()

        /** Search view for Event */
        (activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.searchView.apply {
            if (isAdded){
                // Set inactive background (null)
                this.setBackgroundResource(0) // 0 removes any background
                this.queryHint = getString(R.string.search_event)

                // Ensure it doesn't collapse when focus is lost
                this.setOnQueryTextFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {

                        showHideBeckToCurrentEventIcon(wantToShow = false)
                        // Set active background
                        this.setBackgroundResource(R.drawable.item_background)
                        (this.layoutParams as ViewGroup.MarginLayoutParams).apply {
                            setMargins(
                                0, // Start margin
                                0, // Top margin
                                resources.getDimension(com.intuit.sdp.R.dimen._6sdp).toInt(), // End margin
                                0 // Bottom margin
                            )
                        }

                    } else {

                        // Refocus the SearchView if it loses focus
                        (this.layoutParams as ViewGroup.MarginLayoutParams).apply {
                            setMargins(
                                0, // Start margin
                                0, // Top margin
                                0, // End margin
                                0  // Bottom margin
                            )
                        }

                        DisplayUtil.isKeyboardVisible(requireContext()) { isVisible ->
                            if (isVisible) {
                                showHideBeckToCurrentEventIcon(wantToShow = false)
                            } else {
                                showHideBeckToCurrentEventIcon(wantToShow = true)
                            }
                        }
                    }

                }

                // Set query text listener for filtering and search actions
                this.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        // Handle query submission
                        if (!query.isNullOrBlank()) {
                            // Perform search or filtering based on the query
                            currentQuery = query // Save the query
                            eventAdapter.filter.filter(query)

                            KeyboardUtils.hideKeyboard(this@SearchEventFragment.requireActivity())
                        }
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        // Handle query text changes
                        currentQuery = newText // Save the query
                        eventAdapter.filter.filter(newText ?: "")
                        return true
                    }
                })

                // Handle the close action of SearchView
                //this.setOnCloseListener {}
                val closeButton = this.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
                closeButton?.setOnClickListener {
                   resetSearchView()
                }
            }
        }

        /** Back to current Event */
        (activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.backToCurrentEventIcon.setOnClickListener { scrollEventIndexAtCurrentDate() }
    }

    private fun setupUI() {
        binding.apply {
            //region Event handlers
            rvEvent.layoutManager = LinearLayoutManager(requireContext())
            rvEvent.setHasFixedSize(true)

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
                            outRect.bottom = margin * 2 //0
                        }
                        else -> { // Middle items
                            outRect.top = margin
                            outRect.bottom = margin
                        }
                    }

                }
            })

            rvEvent.adapter = eventAdapter

            collectDataForAdapter()

            eventAdapter.updateFirstDayOfWeek()
            eventAdapter.setConfigureEventCallback {event: Event ->
                // got event update
                navigateToViewEventFrag(event = event)
            }

            eventAdapter.setNoDataCallback {hasData ->
                rvEvent.visibility = View.GONE.takeUnless { hasData } ?: View.VISIBLE
                tvNotify.apply {
                    text = resources.getString(R.string.no_data)
                    visibility = View.GONE.takeIf { hasData } ?: View.VISIBLE
                }
                includedProgressLayout.progressBar.visibility = View.GONE
            }
            //endregion
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun collectDataForAdapter() {
        lifecycleScope.launch(Dispatchers.Main) {
            lifecycleScope.launch(Dispatchers.Main) {
                viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED){
                    viewModel.firstDayOfTheWeek.collectLatest { firstDay->
                        when(firstDay){
                            "Sunday" -> eventAdapter.updateFirstDayOfWeek(Calendar.SUNDAY)
                            "Monday" -> eventAdapter.updateFirstDayOfWeek(Calendar.MONDAY)
                            "Saturday" -> eventAdapter.updateFirstDayOfWeek(Calendar.SATURDAY)
                        }

                    }
                }
            }

            delay(100)
            viewModel.allEventsState.collectLatest {dataState ->
                    val safeBinding = _binding // Safely reference the binding
                    if (safeBinding != null) {
                        if (dataState.isLoading) {
                            // Show loading indicator
                            safeBinding.includedProgressLayout.progressBar.visibility = View.VISIBLE
                            safeBinding.tvNotify.visibility = View.GONE

                        } else if (dataState.error.isNotEmpty()) {
                            // Show error message
                            Toast.makeText(requireContext(), dataState.error, Toast.LENGTH_SHORT).show()
                            safeBinding.includedProgressLayout.progressBar.visibility = View.GONE
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
                                viewModel.findPositionOfEvent(data)
                                // Scroll to position after data is loaded
                                scrollEventIndexAtCurrentDate()

                                safeBinding.includedProgressLayout.progressBar.visibility = View.GONE
                            }

                        }
                    }else {
                        // observeViewModelState: Binding is null, skipping UI update.
                    }
                }
        }
    }

    private fun navigateToViewEventFrag(event: Event) {
        lifecycleScope.launch {
            // Make sure the navigation happens on the main thread
            bundle = (bundle ?: Bundle()).apply {
                putParcelable(Constants.KEY_EVENT, event)// Pass the event object
            }
            findNavController().navigate(R.id.viewEventFragment, bundle, MyNavigation.navOptions)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        resetSearchView()
        binding.rvEvent.adapter = null
        _binding = null
    }

    private fun resetSearchView() {
        currentQuery = null // Clear the query
        eventAdapter.filter.filter("") // Reset the filter
        (activity as MainActivity).resetSearchView()
        showHideBeckToCurrentEventIcon(wantToShow = true)
    }

    // Show or Hide jump to current date event icon
    private fun showHideBeckToCurrentEventIcon(wantToShow: Boolean) {
        (activity as MainActivity).apply {
            if(wantToShow){
                showViewWithAnimation(this.binding.appBarMain.includedAppBarMainCustomToolbar.backToCurrentEventIcon, duration = 0)
            }else{
                hideViewWithAnimation(this.binding.appBarMain.includedAppBarMainCustomToolbar.backToCurrentEventIcon, duration = 0)
            }
        }
    }

    // Reach out the current date's/month's event
    @SuppressLint("NotifyDataSetChanged")
    private fun scrollEventIndexAtCurrentDate() {
        binding.rvEvent.post {
            val layoutManager = binding.rvEvent.layoutManager as? LinearLayoutManager
            layoutManager?.scrollToPositionWithOffset(viewModel.currentEventPos.value, 0)
            eventAdapter.notifyDataSetChanged()
            //binding.rvEvent.smoothScrollToPosition(viewModel.currentEventPos.value) // note: you want use also "scrollToPosition(pos)"
        }
    }
}