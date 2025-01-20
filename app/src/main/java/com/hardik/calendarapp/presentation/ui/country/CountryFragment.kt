package com.hardik.calendarapp.presentation.ui.country

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
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
import com.hardik.calendarapp.presentation.adapter.CountryAdapter
import com.hardik.calendarapp.presentation.adapter.CountryItem
import com.hardik.calendarapp.presentation.adapter.HORIZONTAL
import com.hardik.calendarapp.presentation.adapter.VERTICAL
import com.hardik.calendarapp.presentation.ui.MainActivity
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

    // Variable to store the current query for search
    private var currentQuery: String? = null

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
        //val countries = resources.getStringArray(R.array.country_entries)
        //val countryCodes = resources.getStringArray(R.array.country_values)
        val countryList = getCountryList()

        // Step 4: Map country data to CountryItem, setting isSelected based on selectedCountries
        //viewModel.initializeCountries(savedCountries, countries, countryCodes)
        viewModel.initializeCountries(savedCountries, countryList)

        // Step 5: Observe countryItems and update RecyclerView
        lifecycleScope.launch {
            viewModel.countryItems.collectLatest { countryItems ->

                //countryAdapter.submitList(countryItems) // Use Default item of list if needed
                countryAdapter.apply {
                    submitFullList(countryItems) // Use the filtered list
                    //currentQuery?.let { this.filter.filter(it) } //IndexOutOfBoundsException: Index: 9, Size: 9
                }

                val selectedCountryItems = countryItems.filter { it.isSelected } // Filter only selected items
                //Log.i(TAG, "onViewCreated: selectedCountry $selectedItems", )
                //selectedCountryAdapter.submitList(selectedCountryItems) // Use Default item of list if needed
                selectedCountryAdapter.submitFullList(selectedCountryItems) // Use the filtered list

                delay(100)
                // Reapply the current query after updating the data for search
                //currentQuery?.let { countryAdapter.filter.filter(it) }

            }
        }

        // Step 6: Set up the RecyclerView
        binding.apply {
            countryRecView.apply {
                layoutManager = LinearLayoutManager(context)
                setHasFixedSize(true)

                val margin = resources.getDimension(com.intuit.sdp.R.dimen._1sdp).toInt()

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
                                outRect.top = 0
                                outRect.bottom = margin
                            }
                            itemCount - 1 -> { // Last item
                                outRect.top = margin
                                outRect.bottom = 0
                            }
                            else -> { // Middle items
                                outRect.top = margin
                                outRect.bottom = margin
                            }
                        }
                    }
                })

                adapter = countryAdapter

            }
            countryRecViewHorizontal.apply {
                // Set the LayoutManager with horizontal orientation
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                setHasFixedSize(true)

                val margin = resources.getDimension(com.intuit.sdp.R.dimen._4sdp).toInt()

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
        (activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.saveSelectLanguageIcon.setOnClickListener {
            if (isAdded){
                lifecycleScope.launch {
                    viewModel.saveSelectedCountries(
                        viewModel.countryItems.value
                            .filter { it.isSelected } // Filter only selected items
                            .map { it.code }          // Map to country codes
                            .toSet()                  // Convert to a Set
                    )

                    // Optional: Update ViewModel or trigger side effects
                    viewModel.getHolidayCalendarData()
                    
                    if (findNavController().currentDestination?.id == R.id.nav_select_country) {
                        findNavController().popBackStack(R.id.nav_select_country, inclusive = true)// Pop back two fragments by specifying the fragment ID you want to retain
                    } else {
                        Log.e(TAG,"NavigationError ->: Destination not in back stack")
                    }
                }
            }
        }

        /** Search view for Country */
        (activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.searchView.apply {
            if (isAdded){
                // Set inactive background (null)
                this.setBackgroundResource(0) // 0 removes any background
                // Ensure SearchView stays expanded and doesn't collapse
                //this.isIconified = false
                //this.setIconifiedByDefault(false)
                this.queryHint = getString(R.string.search_country)

                /*this.setPadding(resources.getDimension(com.intuit.sdp.R.dimen._24sdp).toInt(), 0, 0, 0 )*/
                // Adjust margins dynamically
                val params = (this.layoutParams as ViewGroup.MarginLayoutParams).apply {
                    setMargins(
                        resources.getDimension(com.intuit.sdp.R.dimen._36sdp).toInt(), // Start margin
                        0,  // Top margin
                        0,//resources.getDimension(com.intuit.sdp.R.dimen._12sdp).toInt(), // End margin
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

                        //this.setBackgroundResource(R.drawable.item_background) // 0 removes any background

                    }

                }

                // Set query text listener for filtering and search actions
                this.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        // Handle query submission
                        if (!query.isNullOrBlank()) {
                            // Perform search or filtering based on the query
                            currentQuery = query // Save the query
                            countryAdapter.filter.filter(query)
                        }
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        // Handle query text changes
                        currentQuery = newText // Save the query
                        countryAdapter.filter.filter(newText ?: "")
                        return true
                    }
                })

                // Handle the close action of SearchView
                this.setOnCloseListener {
                    // Reset filter when the SearchView is closed
                    this.setBackgroundResource(0) // 0 removes any background
                    currentQuery = null // Clear the query
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

    private fun getCountryList(): List<CountryItem> {
        return listOf(
            CountryItem(flag = R.drawable.ic_country_flag_afghanistan, name = "Afghanistan", code = "af", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_albania, name = "Albania", code = "al", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_algeria, name = "Algeria", code = "dz", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "American Samoa", code = "as", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Andorra", code = "ad", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_angola, name = "Angola", code = "ao", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Anguilla", code = "ai", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Antigua & Barbuda", code = "ag", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_argentina, name = "Argentina", code = "ar", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Armenia", code = "am", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Aruba", code = "aw", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_australia, name = "Australia", code = "australian", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_austria, name = "Austria", code = "austrian", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Azerbaijan", code = "az", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Bahamas", code = "bs", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_bahrain, name = "Bahrain", code = "bh", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_bangladesh, name = "Bangladesh", code = "bd", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Barbados", code = "bb", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_belarus, name = "Belarus", code = "by", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_belgium, name = "Belgium", code = "be", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Belize", code = "bz", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Benin", code = "bj", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Bermuda", code = "bm", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Bhutan", code = "bt", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Bolivia", code = "bo", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_bosnia_and_herzegovina, name = "Bosnia & Herzegovina", code = "ba", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Botswana", code = "bw", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_brazil, name = "Brazil", code = "brazilian", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "British Virgin Islands", code = "vg", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Brunei", code = "bn", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_bulgaria, name = "Bulgaria", code = "bulgarian", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Burkina Faso", code = "bf", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Burundi", code = "bi", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_cambodia, name = "Cambodia", code = "kh", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_cameroon, name = "Cameroon", code = "cm", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_canada, name = "Canada", code = "canadian", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Cape Verde", code = "cv", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Cayman Islands", code = "ky", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Central African Republic", code = "cf", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Chad", code = "td", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_chile, name = "Chile", code = "cl", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "China", code = "china", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_colombia, name = "Colombia", code = "co", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Comoros", code = "km", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_congo, name = "Congo - Brazzaville", code = "cg", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Congo - Kinshasa", code = "cd", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Cook Islands", code = "ck", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_costa_rica, name = "Costa Rica", code = "cr", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_ivory_coast, name = "Côte d’Ivoire", code = "ci", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_croatia, name = "Croatia", code = "croatian", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Cuba", code = "cu", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Curaçao", code = "cw", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Cyprus", code = "cy", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_czechia, name = "Czechia", code = "czech", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_denmark, name = "Denmark", code = "danish", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Djibouti", code = "dj", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Dominica", code = "dm", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Dominican Republic", code = "do", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_ecuador, name = "Ecuador", code = "ec", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_egypt, name = "Egypt", code = "eg", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "El Salvador", code = "sv", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_equatorial_guinea, name = "Equatorial Guinea", code = "gq", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Eritrea", code = "er", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_estonia, name = "Estonia", code = "ee", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Eswatini", code = "sz", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_ethiopia, name = "Ethiopia", code = "et", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Falkland Islands (Islas Malvinas)", code = "fk", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Faroe Islands", code = "fo", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_fiji, name = "Fiji", code = "fj", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Finland", code = "finnish", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_france, name = "France", code = "french", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "French Guiana", code = "gf", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "French Polynesia", code = "pf", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Gabon", code = "ga", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Gambia", code = "gm", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Georgia", code = "ge", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_germany, name = "Germany", code = "german", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_ghana, name = "Ghana", code = "gh", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Gibraltar", code = "gi", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_greece, name = "Greece", code = "greek", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Greenland", code = "gl", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Grenada", code = "gd", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Guadeloupe", code = "gp", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Guam", code = "gu", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Guatemala", code = "gt", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Guernsey", code = "gg", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_guinea, name = "Guinea", code = "gn", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Guinea-Bissau", code = "gw", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Guyana", code = "gy", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Haiti", code = "ht", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Honduras", code = "hn", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Hong Kong", code = "hong_kong", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_hungary, name = "Hungary", code = "hungarian", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Iceland", code = "is", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_india, name = "India", code = "indian", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Indonesia", code = "indonesian", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_iran, name = "Iran", code = "ir", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_iraq, name = "Iraq", code = "iq", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_ireland, name = "Ireland", code = "irish", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Isle of Man", code = "im", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Israel", code = "jewish", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_italy, name = "Italy", code = "italian", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Jamaica", code = "jm", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_japan, name = "Japan", code = "japanese", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Jersey", code = "je", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Jordan", code = "jo", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_kazakhstan, name = "Kazakhstan", code = "kz", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_kenya, name = "Kenya", code = "ke", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Kiribati", code = "ki", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Kosovo", code = "xk", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_kuwait, name = "Kuwait", code = "kw", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Kyrgyzstan", code = "kg", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_laos, name = "Laos", code = "la", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_latvia, name = "Latvia", code = "latvian", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_lebanon, name = "Lebanon", code = "lb", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Lesotho", code = "ls", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Liberia", code = "lr", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_libya, name = "Libya", code = "ly", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_liechtenstein, name = "Liechtenstein", code = "li", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_lithuania, name = "Lithuania", code = "lithuanian", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_luxembourg, name = "Luxembourg", code = "lu", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_macao, name = "Macao", code = "mo", isSelected = false),//54
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Madagascar", code = "mg", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Malawi", code = "mw", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_malaysia, name = "Malaysia", code = "malaysia", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Maldives", code = "mv", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_mali, name = "Mali", code = "ml", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_malta, name = "Malta", code = "mt", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Marshall Islands", code = "mh", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Martinique", code = "mq", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Mauritania", code = "mr", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_mauritius, name = "Mauritius", code = "mu", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Mayotte", code = "yt", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_mexico, name = "Mexico", code = "mexican", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Micronesia", code = "fm", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_moldova, name = "Moldova", code = "md", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Monaco", code = "mc", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Mongolia", code = "mn", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_montenegro, name = "Montenegro", code = "me", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Montserrat", code = "ms", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Morocco", code = "ma", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Mozambique", code = "mz", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_myanmar, name = "Myanmar (Burma)", code = "mm", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Namibia", code = "na", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Nauru", code = "nr", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_nepal, name = "Nepal", code = "np", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_netherlands, name = "Netherlands", code = "dutch", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "New Caledonia", code = "nc", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "New Zealand", code = "new_zealand", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_nicaragua, name = "Nicaragua", code = "ni", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Niger", code = "ne", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_nigeria, name = "Nigeria", code = "ng", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Northern Mariana Islands", code = "mp", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_north_korea, name = "North Korea", code = "kp", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_macedonia, name = "North Macedonia", code = "mk", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_norway, name = "Norway", code = "norwegian", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_oman, name = "Oman", code = "om", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_pakistan, name = "Pakistan", code = "pk", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Palau", code = "pw", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_panama, name = "Panama", code = "pa", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Papua New Guinea", code = "pg", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_paraguay, name = "Paraguay", code = "py", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_peru, name = "Peru", code = "pe", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_philippines, name = "Philippines", code = "philippines", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_poland, name = "Poland", code = "polish", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_portugal, name = "Portugal", code = "portuguese", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Puerto Rico", code = "pr", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_qatar, name = "Qatar", code = "qa", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_reunion, name = "Réunion", code = "re", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_romania, name = "Romania", code = "romanian", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_russia, name = "Russia", code = "russian", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_rwanda, name = "Rwanda", code = "rw", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Samoa", code = "ws", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "San Marino", code = "sm", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "São Tomé & Príncipe", code = "st", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Saudi Arabia", code = "saudiarabian", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_senegal, name = "Senegal", code = "sn", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_serbia, name = "Serbia", code = "rs", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Seychelles", code = "sc", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Sierra Leone", code = "sl", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_singapore, name = "Singapore", code = "singapore", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Sint Maarten", code = "sx", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Slovakia", code = "slovak", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_slovenia, name = "Slovenia", code = "slovenian", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Solomon Islands", code = "sb", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Somalia", code = "so", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_south_africa, name = "South Africa", code = "sa", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_south_korea, name = "South Korea", code = "south_korea", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "South Sudan", code = "ss", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_spain, name = "Spain", code = "spain", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_sri_lanka, name = "Sri Lanka", code = "lk", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "St. Barthélemy", code = "bl", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "St. Helena", code = "sh", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "St. Kitts & Nevis", code = "kn", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "St. Lucia", code = "lc", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "St. Martin", code = "mf", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "St. Pierre & Miquelon", code = "pm", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "St. Vincent & Grenadines", code = "vc", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Sudan", code = "sd", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Suriname", code = "sr", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Sweden", code = "swedish", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_switzerland, name = "Switzerland", code = "ch", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Syria", code = "sy", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_taiwan, name = "Taiwan", code = "taiwan", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Tajikistan", code = "tj", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_tanzania, name = "Tanzania", code = "tz", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_thailand, name = "Thailand", code = "th", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Timor-Leste", code = "tl", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Togo", code = "tg", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Tonga", code = "to", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_trinidad_and_tobago, name = "Trinidad & Tobago", code = "tt", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Tunisia", code = "tn", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Turkey", code = "turkish", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Turkmenistan", code = "tm", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Turks & Caicos Islands", code = "tc", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_tuvalu, name = "Tuvalu", code = "tv", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "U.S. Virgin Islands", code = "vi", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_uganda, name = "Uganda", code = "ug", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Ukraine", code = "ukrainian", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_united_arab_emirates, name = "United Arab Emirates", code = "ae", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_united_kingdom, name = "United Kingdom", code = "uk", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "United States", code = "usa", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_uruguay, name = "Uruguay", code = "uy", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_uzbekistan, name = "Uzbekistan", code = "uz", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Vanuatu", code = "vu", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Vatican City", code = "va", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_venezuela, name = "Venezuela", code = "ve", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Vietnam", code = "vietnamese", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Wallis & Futuna", code = "wf", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Yemen", code = "ye", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_zambia, name = "Zambia", code = "zm", isSelected = false),
            //CountryItem(flag = R.drawable.ic_country_flag_, name = "Zimbabwe", code = "zw", isSelected = false),
        )
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
