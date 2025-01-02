package com.hardik.calendarapp.presentation.ui.country

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants
import com.hardik.calendarapp.databinding.FragmentCountryBinding
import com.hardik.calendarapp.presentation.MainViewModel
import com.hardik.calendarapp.presentation.ui.MainActivity
import com.hardik.calendarapp.presentation.ui.calendar_month.adapter.CountryAdapter
import com.hardik.calendarapp.presentation.ui.calendar_month.adapter.HORIZONTAL
import com.hardik.calendarapp.presentation.ui.calendar_month.adapter.VERTICAL
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class CountryFragment : Fragment(R.layout.fragment_country) {
    private final val TAG = Constants.BASE_TAG + CountryFragment::class.java.simpleName

    private val binding get() = _binding!!
    private var _binding: FragmentCountryBinding? = null

    private val viewModel: MainViewModel by activityViewModels()

    private val countryAdapter = CountryAdapter ( { selectedCountry, isChecked ->
        onCountrySelected(selectedCountry, isChecked)
    } , viewType = VERTICAL)
    private val selectedCountryAdapter = CountryAdapter ( { selectedCountry, isChecked ->
        onCountrySelected(selectedCountry, isChecked)
    } , viewType = HORIZONTAL)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { }
    }

//    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? { // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_country, container, false) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCountryBinding.bind(view)

        // Step 1: Retrieve saved countries from SharedPreferences
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val savedCountries = sharedPreferences.getStringSet("countries", setOf("indian")) ?: setOf("indian")


        // Step 3: Get country names and country codes from resources
        val countries = resources.getStringArray(R.array.country_entries)
        val countryCodes = resources.getStringArray(R.array.country_values)

        // Step 4: Map country data to CountryItem, setting isSelected based on selectedCountries
        viewModel.initializeCountries(savedCountries, countries, countryCodes)

        // Step 5: Observe countryItems and update RecyclerView
        lifecycleScope.launch {
            viewModel.countryItems.collectLatest { countryItems ->
                //countryAdapter.submitList(countryItems) // Use Default item of list if needed
                countryAdapter.submitFullList(countryItems) // Use the filtered list
                val selectedCountryItems = countryItems.filter { it.isSelected } // Filter only selected items
                //Log.i(TAG, "onViewCreated: selectedCountry $selectedItems", )
                //selectedCountryAdapter.submitList(selectedCountryItems) // Use Default item of list if needed
                selectedCountryAdapter.submitFullList(selectedCountryItems) // Use the filtered list

                delay(400)
                //countryAdapter.filter.filter("ind")
            }
        }

        // Step 6: Set up the RecyclerView
        binding.apply {
            countryRecView.apply {
                layoutManager = LinearLayoutManager(context)
                setHasFixedSize(true)
                adapter = countryAdapter

            }
            countryRecViewHorizontal.apply {
                // Set the LayoutManager with horizontal orientation
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                setHasFixedSize(true)

                val margin = resources.getDimension(com.intuit.sdp.R.dimen._6sdp).toInt()

                addItemDecoration(object: RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(
                        outRect: Rect,
                        view: View,
                        parent: RecyclerView,
                        state: RecyclerView.State
                    ) {
                        val position = parent.getChildAdapterPosition(view) // Get the position of the item
                        val itemCount = parent.adapter?.itemCount ?: 0

                        if (position == RecyclerView.NO_POSITION) return

                        // Apply margin adjustments
                        when (position) {
                            0 -> { // First item
                                outRect.left = 0
                                outRect.right = margin
                            }
                            itemCount - 1 -> { // Last item
                                outRect.left = margin
                                outRect.right = 0 // margin.times(margin) // 0
                            }
                            else -> { // Middle items
                                outRect.left = margin
                                outRect.right = margin
                            }
                        }
                    }
                })

                // Ensure proper padding to prevent clipping of the last item
                //setPadding(0, 0, margin, 0) // Add padding to the right
                //clipToPadding = false // Allow the RecyclerView to scroll beyond the padding

                // Attach the SnapHelper to the RecyclerView
                //val snapHelper = LinearSnapHelper()
                //snapHelper.attachToRecyclerView(this)


                // Set the adapter
                adapter = selectedCountryAdapter
            }
        }


        /** Save Selected Country */
        (activity as MainActivity).binding.saveSelectLanguageIcon.setOnClickListener {
            if (isAdded){
                lifecycleScope.launch {
                    viewModel.saveSelectedCountries(
                        viewModel.countryItems.value
                            .filter { it.isSelected } // Filter only selected items
                            .map { it.code }          // Map to country codes
                            .toSet()                  // Convert to a Set
                    )
                    if (findNavController().currentDestination?.id == R.id.nav_select_country) {
                        findNavController().popBackStack(R.id.nav_select_country, inclusive = true)// Pop back two fragments by specifying the fragment ID you want to retain
                    } else {
                        Log.e(TAG,"NavigationError ->: Destination not in back stack")
                    }
                }
            }
        }

        (activity as MainActivity).binding.searchView.apply {
            if (isAdded){
                // Ensure SearchView stays expanded and doesn't collapse
                this.isIconified = false
                this.setIconifiedByDefault(false)

                // Ensure it doesn't collapse when focus is lost
                this.setOnQueryTextFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        // Refocus the SearchView if it loses focus
                        this.requestFocus()
                    }
                }

                // Set query text listener for filtering and search actions
                this.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        // Handle query submission
                        if (!query.isNullOrBlank()) {
                            // Perform search or filtering based on the query
                            countryAdapter.filter.filter(query)
                        }
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        // Handle query text changes
                        countryAdapter.filter.filter(newText ?: "")
                        return true
                    }
                })

                // Handle the close action of SearchView
                this.setOnCloseListener {
                    // Reset filter when the SearchView is closed
                    countryAdapter.filter.filter("")
                    false // Return false if the event hasn't been consumed yet
                }
            }
        }
    }


//    private fun saveSelectedCountries(selectedCountries: Set<String>) {
//        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
//        sharedPreferences.edit()
//            .putStringSet("countries", selectedCountries)
//            .apply()
//    }

    private fun onCountrySelected(countryCode: String, isSelected: Boolean) {
        viewModel.toggleCountrySelection(countryCode)

        // Optional: Update ViewModel or trigger side effects
        //viewModel.getHolidayCalendarData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
/*
class StartSnapHelper : LinearSnapHelper() {
    override fun findSnapView(layoutManager: RecyclerView.LayoutManager): View? {
        if (layoutManager is LinearLayoutManager) {
            // Always snap to the first visible item
            return layoutManager.findViewByPosition(0)
        }
        return super.findSnapView(layoutManager)
    }
    override fun calculateDistanceToFinalSnap(
        layoutManager: RecyclerView.LayoutManager,
        targetView: View
    ): IntArray? {
        val distances = super.calculateDistanceToFinalSnap(layoutManager, targetView)
        if (layoutManager is LinearLayoutManager) {
            val position = layoutManager.getPosition(targetView)
            val itemCount = layoutManager.itemCount

            if (distances != null) {
                when (position) {
                    0 -> {
                        // Adjust snapping for the first item
                        distances[0] = 50 // Adjust horizontal offset (left padding)
                    }
                    itemCount - 1 -> {
                        // Adjust snapping for the last item
                        distances[0] = 50 // Adjust horizontal offset (right padding)
                    }
                }
            }
        }
        return distances
    }
}*/
