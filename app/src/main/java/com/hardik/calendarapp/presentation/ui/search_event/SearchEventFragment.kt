package com.hardik.calendarapp.presentation.ui.search_event

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.widget.EditText
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
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.common.Constants.KEY_EVENT_JSON
import com.hardik.calendarapp.common.DataListState
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.data.database.entity.SourceType
import com.hardik.calendarapp.databinding.FragmentSearchEventBinding
import com.hardik.calendarapp.presentation.MainViewModel
import com.hardik.calendarapp.presentation.adapter.SearchEventAdapter
import com.hardik.calendarapp.presentation.ui.MainActivity
import com.hardik.calendarapp.utillities.DisplayUtil
import com.hardik.calendarapp.utillities.DisplayUtil.hideViewWithAnimation
import com.hardik.calendarapp.utillities.DisplayUtil.showViewWithAnimation
import com.hardik.calendarapp.utillities.GsonUtil
import com.hardik.calendarapp.utillities.KeyboardUtils.hideKeyboard
import com.hardik.calendarapp.utillities.MyNavigation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchEventFragment : Fragment() {
    private val TAG = BASE_TAG + SearchEventFragment::class.simpleName

    private var _binding: FragmentSearchEventBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private val eventAdapter by lazy { SearchEventAdapter(requireContext()) }
    private var currentQuery: String? = null // Variable to store the current query for search
    private var isFirstTimeFlag = true

    var bundle: Bundle? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentSearchEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvEvent.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
            adapter = eventAdapter
        }

        setupEventRecycler()

        /** Search view for Event */
        (activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.includedSchedule.includedSearchView.root.apply {
            if (isAdded){
                // Set inactive background (null)
                this.setBackgroundResource(0) // 0 removes any background
                this.queryHint = getString(R.string.search_event)

                // Ensure it doesn't collapse when focus is lost
                this.setOnQueryTextFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {

                        // hide keyboard when empty searchView and pressed doneAction
                        val editText = this.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
                        editText.setOnEditorActionListener { _, actionId, _ ->
                            if (actionId == EditorInfo.IME_ACTION_DONE) {
                                hideKeyboard(requireActivity())
                                true
                            } else {
                                false
                            }
                        }

                        showHideBeckToCurrentEventIcon(wantToShow = false)
                        // Set active background
                        this.setBackgroundResource(R.drawable.item_background)
                        (this.layoutParams as ViewGroup.MarginLayoutParams).apply {
                            setMargins(resources.getDimension(R.dimen.menuItemHorizontalSpacing).toInt(), 0, resources.getDimension(com.intuit.sdp.R.dimen._minus3sdp).toInt(), 0 )
                            width = ViewGroup.LayoutParams.MATCH_PARENT
                            height = ViewGroup.LayoutParams.MATCH_PARENT
                        }

                    } else {

                        // Refocus the SearchView if it loses focus
                        (this.layoutParams as ViewGroup.MarginLayoutParams).apply {
                            setMargins(0, 0, 0, 0)
                            width = ViewGroup.LayoutParams.WRAP_CONTENT
                            height = ViewGroup.LayoutParams.WRAP_CONTENT
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
                            viewModel.getAllEvents(query)

                            hideKeyboard(this@SearchEventFragment.requireActivity())
                        }
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        // Handle query text changes
                        currentQuery = newText // Save the query
                        viewModel.getAllEvents(newText)
                        return true
                    }
                })

                // Handle the close action of SearchView
                val closeButton = this.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
                closeButton?.setOnClickListener {
                    resetSearchView()
                }
            }
        }

        /** Back to current Event */
        (activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.includedSchedule.includedBackToDate.root.setOnClickListener {
            isFirstTimeFlag = true
            val position = viewModel.currentEventPos.value
            scrollEventIndexAtJumpToCurrentDate(position = position)
        }
    }

    private fun setupEventRecycler() {
        binding.apply {
            //region Event handlers

            val margin = resources.getDimension(R.dimen.itemCountryVerticalSpacing_dev2).toInt()

            rvEvent.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                    when (newState) {
                        RecyclerView.SCROLL_STATE_IDLE -> {
                            // RecyclerView is NOT scrolling
                        }
                        RecyclerView.SCROLL_STATE_DRAGGING -> {
                            // User is actively dragging the list
                            hideKeyboard(requireActivity())
                        }
                        RecyclerView.SCROLL_STATE_SETTLING -> {
                            // RecyclerView is settling after fling
                            hideKeyboard(requireActivity())
                        }
                    }
                }
            })

            //addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            // Add a custom ItemDecoration to handle padding/margin
            rvEvent.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
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

            observeViewModelState()

            val itemCount = binding.rvEvent.adapter?.itemCount ?: 0
            if(itemCount != 0){
                // show RecyclerView
                rvEvent.visibility = View.VISIBLE
                tvNotify.visibility = View.GONE
                includedProgressLayout.progressBar.visibility = View.GONE
            }

            eventAdapter.setConfigureEventCallback {event: Event ->
                // got event update
                navigateToViewEventFrag(event = event)
            }

            //endregion
        }
    }

    @OptIn(FlowPreview::class)
    @SuppressLint("NotifyDataSetChanged")
    private fun observeViewModelState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED){
                //combine(viewModel.allEventsState.debounce(300)) { dataState: Array<DataListState<Event>> -> dataState.get(0) }
                viewModel.allEventsState.debounce(300).collectLatest { dataState -> handleDataState(dataState) }
            }
        }
    }

    private fun handleDataState(dataState: DataListState<Event>) {

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
            viewModel.findPositionOfEvent(data)

            // Scroll to position after data is loaded
            eventAdapter.apply {
               //updateData(data, it)
                submitList(data)

                scrollEventIndexAtCurrentDate()

                binding.includedProgressLayout.progressBar.visibility = View.GONE
                binding.tvNotify.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun navigateToViewEventFrag(event: Event) {
        lifecycleScope.launch {
            // Make sure the navigation happens on the main thread
            val eventJson = GsonUtil.toJson(event)
            bundle = (bundle ?: Bundle()).apply {
                putString(KEY_EVENT_JSON, eventJson)// Pass the event object
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

            findNavController().navigate(R.id.viewEventFragment, bundle, MyNavigation.navOptions)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        resetSearchView()
    }

    private fun resetSearchView() {
        currentQuery = null // Clear the query
        viewModel.getAllEvents(null)
        val searchView = (activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.includedSchedule.includedSearchView.root
        (activity as MainActivity).resetSearchView(searchView)
        showHideBeckToCurrentEventIcon(wantToShow = true)
    }

    // Show or Hide jump to current date event icon
    private fun showHideBeckToCurrentEventIcon(wantToShow: Boolean) {
        (activity as MainActivity).apply {
            if(wantToShow){
                (activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.llToolbarMenu.apply {
                    (this.layoutParams as ViewGroup.MarginLayoutParams).apply {
                        width = ViewGroup.LayoutParams.WRAP_CONTENT
                        height = ViewGroup.LayoutParams.MATCH_PARENT
                    }
                }
                showViewWithAnimation(this.binding.appBarMain.includedAppBarMainCustomToolbar.includedSchedule.includedBackToDate.root, duration = 0)
                showViewWithAnimation(binding.appBarMain.includedAppBarMainCustomToolbar.toolbarTitle, duration = 0)
            }else{
                hideViewWithAnimation(this.binding.appBarMain.includedAppBarMainCustomToolbar.includedSchedule.includedBackToDate.root, duration = 0)
                hideViewWithAnimation(binding.appBarMain.includedAppBarMainCustomToolbar.toolbarTitle, duration = 0)
                (activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.llToolbarMenu.apply {
                    (this.layoutParams as ViewGroup.MarginLayoutParams).apply {
                        width = ViewGroup.LayoutParams.MATCH_PARENT
                        height = ViewGroup.LayoutParams.MATCH_PARENT
                    }
                }
            }
        }
    }

    // Reach out the current date's/month's event
    @SuppressLint("NotifyDataSetChanged")
    private fun scrollEventIndexAtCurrentDate() {
        viewModel.currentEventPos.value.let { position ->
            //Log.e(TAG, "scrollEventIndexAtCurrentDate: $position", )
            if (isFirstTimeFlag){
                isFirstTimeFlag = false
                binding.rvEvent.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        binding.rvEvent.viewTreeObserver.removeOnPreDrawListener(this)
                        (binding.rvEvent.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(position, 0)
                        return true
                    }
                })
            }
        }
        binding.rvEvent.visibility = View.VISIBLE
    }
    private fun scrollEventIndexAtJumpToCurrentDate(position: Int = -1) {
        isFirstTimeFlag = false
        binding.rvEvent.post {
            val layoutManager = binding.rvEvent.layoutManager as? LinearLayoutManager
            if (position != -1) {
                val smoothScroller = object : LinearSmoothScroller(binding.rvEvent.context) {
                    override fun getVerticalSnapPreference(): Int {
                        return SNAP_TO_START
                    }
                }
                smoothScroller.targetPosition = position
                layoutManager?.startSmoothScroll(smoothScroller)
            }
        }
    }
}