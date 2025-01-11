package com.hardik.calendarapp.presentation.ui.search_event

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
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
import com.hardik.calendarapp.utillities.DisplayUtil.dpToPx
import com.hardik.calendarapp.utillities.MyNavigation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class SearchEventFragment : Fragment(R.layout.fragment_search_event) {
    private val TAG = BASE_TAG + SearchEventFragment::class.simpleName

    private val binding get() = _binding ?: throw IllegalStateException("Binding is only valid between onCreateView and onDestroyView")
    private var _binding: FragmentSearchEventBinding? = null

    private val viewModel: MainViewModel by activityViewModels()
    private val eventAdapter: EventAdapter = EventAdapter(arrayListOf())
    private var currentQuery: String? = null // Variable to store the current query for search

    var bundle: Bundle? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate: ")
        arguments?.let { }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? { Log.i(TAG, "onCreateView: ") ; return inflater.inflate(R.layout.fragment_search_event, container, false) }

    @SuppressLint("NotifyDataSetChanged")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "onViewCreated: ")
        _binding = FragmentSearchEventBinding.bind(view)

        viewModel.getAllEvents()

        setupUI()

        /** Search view for Country */
        (activity as MainActivity).binding.searchView.apply {
            if (isAdded){
                // Set inactive background (null)
                this.setBackgroundResource(0) // 0 removes any background
                // Ensure SearchView stays expanded and doesn't collapse
                //this.isIconified = false
                //this.setIconifiedByDefault(false)

                /*this.setPadding(resources.getDimension(com.intuit.sdp.R.dimen._24sdp).toInt(), 0, 0, 0 )*/
                // Adjust margins dynamically
                val params = (this.layoutParams as ViewGroup.MarginLayoutParams).apply {
                    setMargins(
                        resources.getDimension(com.intuit.sdp.R.dimen._36sdp).toInt(), // Start margin
                        0,  // Top margin
                        resources.getDimension(com.intuit.sdp.R.dimen._12sdp).toInt(), // End margin
                        0   // Bottom margin
                    )
                }

                // Ensure it doesn't collapse when focus is lost
                this.setOnQueryTextFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        // Set active background
                        this.setBackgroundResource(R.drawable.item_background)

                    } else {
                        // Refocus the SearchView if it loses focus
                        //this.requestFocus()

                        // Set inactive background (null)
                        //this.setBackgroundResource(0) // 0 removes any background
                        //this.setBackgroundResource(R.drawable.item_background)
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
                this.setOnCloseListener {
                    // Reset filter when the SearchView is closed
                    this.setBackgroundResource(0)
                    currentQuery = null // Clear the query
                    eventAdapter.filter.filter("")
                    false // Return false if the event hasn't been consumed yet
                }
            }
        }
    }

    private fun setupUI() {
        binding.apply {
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

            // Add custom ItemDecoration for divider (34dp space)
            //binding.rvEvent.addItemDecoration(MonthDividerDecoration())

            binding.rvEvent.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    for (i in 0 until recyclerView.childCount) {
                        val child = recyclerView.getChildAt(i)
                        val viewHolder = recyclerView.getChildViewHolder(child) as EventAdapter.ViewHolder //ParallaxAdapter.ParallaxViewHolder

                        // Calculate the offset for parallax scrolling
                        val offset = calculateParallaxOffset(recyclerView, child)
                        //viewHolder.updateParallaxOffset(offset)
                    }
                }

                private fun calculateParallaxOffset(recyclerView: RecyclerView, view: View): Int {
                    val recyclerViewHeight = recyclerView.height
                    val itemCenter = (view.top + view.bottom) / 2
                    val recyclerViewCenter = recyclerViewHeight / 2

                    // Calculate the distance from the item center to the RecyclerView center
                    val distanceFromCenter = recyclerViewCenter - itemCenter

                    // Adjust the parallax intensity (higher values for stronger effect)
                    val parallaxIntensity = 0.2//0.5f

                    return (distanceFromCenter * parallaxIntensity).toInt()
                }
            })

            //todo: or use this

            /*binding.rvEvent.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    for (i in 0 until recyclerView.childCount) {
                        val child = recyclerView.getChildAt(i)
                        val viewHolder = recyclerView.getChildViewHolder(child) as EventAdapter.ViewHolder //ParallaxAdapter.ParallaxViewHolder

                        // Get item details
                        val imageView = child.findViewById<ImageView>(R.id.img_itemEventLay_monthTransitionImage)
                        val imageHeight = imageView.drawable?.intrinsicHeight ?: 0
                        val viewTop = child.top
                        val viewBottom = child.bottom

                        // Update parallax offset
                        viewHolder.updateParallaxOffset(
                            imageHeight = imageHeight,
                            viewTop = viewTop,
                            viewBottom = viewBottom,
                            recyclerViewHeight = recyclerView.height
                        )
                    }
                }
            })*/

            binding.rvEvent.adapter = eventAdapter

            collectDataForAdapter()

            eventAdapter.updateFirstDayOfWeek()
            eventAdapter.setConfigureEventCallback {event: Event ->
                // got event update
                navigateToViewEventFrag(event = event)
            }
        }
    }

    private fun collectDataForAdapter() {
        CoroutineScope(Dispatchers.Main).launch {
            lifecycleScope.launch(Dispatchers.Main) {
                viewModel.allEventsState.collectLatest {dataState ->
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

                            //eventAdapter.updateData(data)
                            eventAdapter.apply { updateData(data) }

                            // Scroll to position after data is loaded
                            safeBinding.rvEvent.post {
                                safeBinding.rvEvent.scrollToPosition(120) // Scroll to position 12 after the data is set
                            }

                            //binding.recyclerview.setPadding(0, 0, 0, 0)  // To remove the extra space on top and bottom of the RecyclerVie
                            safeBinding.includedProgressLayout.progressBar.visibility = View.GONE
                        }
                    }else {
                        Log.w(TAG, "observeViewModelState: Binding is null, skipping UI update.")
                    }
                }

            }
        }
    }

    private fun navigateToViewEventFrag(event: Event) {
        Log.i(TAG, "navigateToViewEventFrag: ")
        lifecycleScope.launch {
            // Make sure the navigation happens on the main thread
            Log.e(TAG, "navigateToViewEventFrag:  ${Thread.currentThread().name}",)
            bundle = (bundle ?: Bundle()).apply {
                putParcelable(Constants.KEY_EVENT, event)// Pass the event object
            }
            findNavController().navigate(R.id.viewEventFragment, bundle, MyNavigation.navOptions)
            //findNavController().navigate(R.id.newEventFragment, bundle)
        }
    }

    // Custom ItemDecoration to add a divider with 34dp height between items
    class MonthDividerDecoration : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            // Apply 34dp space (converted to pixels) as the bottom margin for each item
            outRect.bottom = 34.dpToPx()  // 34dp space between items
        }
    }

}