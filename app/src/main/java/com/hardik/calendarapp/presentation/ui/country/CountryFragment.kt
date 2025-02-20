package com.hardik.calendarapp.presentation.ui.country

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants
import com.hardik.calendarapp.databinding.FragmentCountryBinding
import com.hardik.calendarapp.presentation.MainViewModel
import com.hardik.calendarapp.presentation.adapter.CountryAdapter
import com.hardik.calendarapp.presentation.adapter.CountryItem
import com.hardik.calendarapp.presentation.adapter.HORIZONTAL
import com.hardik.calendarapp.presentation.adapter.VERTICAL
import com.hardik.calendarapp.presentation.ui.MainActivity
import com.hardik.calendarapp.utillities.DisplayUtil
import com.hardik.calendarapp.utillities.DisplayUtil.hideViewWithAnimation
import com.hardik.calendarapp.utillities.DisplayUtil.showViewWithAnimation
import com.hardik.calendarapp.utillities.KeyboardUtils.hideKeyboard
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCountryBinding.bind(view)

        // Step 1: Retrieve saved countries from SharedPreferences
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val savedCountries = sharedPreferences.getStringSet("countries", setOf("indian")) ?: setOf("indian")


        // Step 3: Get country names and country codes from resources
        val countryList = getCountryList()

        // Step 4: Map country data to CountryItem, setting isSelected based on selectedCountries
        viewModel.initializeCountries(savedCountries, countryList)

        // Step 5: Observe countryItems and update RecyclerView
        lifecycleScope.launch {
            viewModel.countryItems.collectLatest { countryItems ->
                countryAdapter.apply {
                    submitFullList(countryItems, currentQuery) // Update the full list
                }

                val selectedCountryItems = countryItems.filter { it.isSelected } // Filter only selected items
                selectedCountryAdapter.submitFullList(selectedCountryItems) // Use the filtered list
            }
        }

        // Step 6: Set up the RecyclerView
        binding.apply {
            countryRecView.apply {
                layoutManager = LinearLayoutManager(context)
                setHasFixedSize(true)
                (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

                val margin = resources.getDimension(R.dimen.itemCountryVerticalSpacing_dev2).toInt()

                countryRecView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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

                addItemDecoration(object: RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                        val position = parent.getChildAdapterPosition(view) // Get the position of the item
                        val itemCount = parent.adapter?.itemCount ?: 0

                        if (position == RecyclerView.NO_POSITION) return

                        // Apply margin adjustments
                        when (position) {
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

                adapter = countryAdapter

            }
            countryRecViewHorizontal.apply {
                // Set the LayoutManager with horizontal orientation
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                setHasFixedSize(true)
                (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

                val margin = resources.getDimension(com.intuit.sdp.R.dimen._4sdp).toInt()

                addItemDecoration(object: RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
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

                // Set the adapter
                adapter = selectedCountryAdapter
            }
        }


        /** Save Selected Country */
        (activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.includedSelectCountry.includedSaveSelect.root.setOnClickListener {
            if (isAdded){
                lifecycleScope.launch {
                    viewModel.saveSelectedCountries(
                        viewModel.countryItems.value
                            .filter { it.isSelected } // Filter only selected items
                            .map { it.code }          // Map to country codes
                            .toSet()                  // Convert to a Set
                    )

                    // Optional: Update ViewModel or trigger side effects
                    launch {
                        viewModel.getHolidayCalendarData() // todo: 2 getting api data after getting locale calendar data
                    }
                    
                    if (findNavController().currentDestination?.id == R.id.nav_select_country) {
                        findNavController().popBackStack(R.id.nav_select_country, inclusive = true)// Pop back two fragments by specifying the fragment ID you want to retain
                    } else {
                        // Todo: NavigationError ->: Destination not in back stack
                    }
                }
            }
        }

        /** Search view for Country */
        (activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.includedSelectCountry.includedSearchView.root.apply {
            if (isAdded){
                this.setBackgroundResource(0) // 0 removes any background and Set inactive background
                this.queryHint = getString(R.string.search_country)

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

                        showHideSaveSelectionIcon(wantToShow = false)
                        // Set active background
                        this.setBackgroundResource(R.drawable.item_background)
                        (this.layoutParams as ViewGroup.MarginLayoutParams).apply {
                            setMargins(resources.getDimension(R.dimen.menuItemHorizontalSpacing).toInt(), 0, resources.getDimension(com.intuit.sdp.R.dimen._minus3sdp).toInt(), 0)
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

//                        if (!this.isIconified) { this.isIconified = true } // Collapses SearchView

                        DisplayUtil.isKeyboardVisible(requireContext()) { isVisible ->
                            if (isVisible) {
                                //this.isIconified = false  // Keep SearchView expanded
                                showHideSaveSelectionIcon(wantToShow = false)
                            } else {
                                showHideSaveSelectionIcon(wantToShow = true)
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

                            hideKeyboard(this@CountryFragment.requireActivity())
                        }
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        // Handle query text changes
                        currentQuery = newText // Save the query
                        countryAdapter.submitFullList(viewModel.countryItems.value, currentQuery) // Update the full list
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
    }

    /** On click item for Country */
    private fun onCountrySelected(countryCode: String, isSelected: Boolean) {

        val selectedCountryItems: List<CountryItem> = viewModel.countryItems.value.filter { it.isSelected }
        if (selectedCountryItems.size == 1 && selectedCountryItems.get(0).code.contains(countryCode))
            Toast.makeText(requireContext(), getString(R.string.default_selected_1_country), Toast.LENGTH_SHORT).show()

        viewModel.toggleCountrySelection(countryCode)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        resetSearchView()
        _binding = null
    }

    /** Search view reset */
    private fun resetSearchView() {
        currentQuery = null // Clear the query
        val searchView = (activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.includedSelectCountry.includedSearchView.root
        (activity as MainActivity).resetSearchView(searchView)
        showHideSaveSelectionIcon(wantToShow = true)
    }

    private fun showHideSaveSelectionIcon(wantToShow: Boolean) {
        (activity as MainActivity).apply {
            if(wantToShow){
                (activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.llToolbarMenu.apply {
                    (this.layoutParams as ViewGroup.MarginLayoutParams).apply {
                        width = ViewGroup.LayoutParams.WRAP_CONTENT
                        height = ViewGroup.LayoutParams.MATCH_PARENT
                    }
                }
                showViewWithAnimation(this.binding.appBarMain.includedAppBarMainCustomToolbar.includedSelectCountry.includedSaveSelect.root, duration = 0)
                showViewWithAnimation(this.binding.appBarMain.includedAppBarMainCustomToolbar.toolbarTitle, duration = 0)
            }else{
                hideViewWithAnimation(this.binding.appBarMain.includedAppBarMainCustomToolbar.includedSelectCountry.includedSaveSelect.root, duration = 0)
                hideViewWithAnimation(this.binding.appBarMain.includedAppBarMainCustomToolbar.toolbarTitle, duration = 0)
                (activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.llToolbarMenu.apply {
                    (this.layoutParams as ViewGroup.MarginLayoutParams).apply {
                        width = ViewGroup.LayoutParams.MATCH_PARENT
                        height = ViewGroup.LayoutParams.MATCH_PARENT
                    }
                }
            }
        }
    }

    /** Get list of Countries */
    private fun getCountryList(): List<CountryItem> {
        return listOf(
            CountryItem(flag = R.drawable.ic_country_flag_afghanistan, name = "Afghanistan", code = "af", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_albania, name = "Albania", code = "al", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_algeria, name = "Algeria", code = "dz", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_angola, name = "Angola", code = "ao", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_argentina, name = "Argentina", code = "ar", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_australia, name = "Australia", code = "australian", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_austria, name = "Austria", code = "austrian", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_bahrain, name = "Bahrain", code = "bh", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_bangladesh, name = "Bangladesh", code = "bd", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_belarus, name = "Belarus", code = "by", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_belgium, name = "Belgium", code = "be", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_bosnia_and_herzegovina, name = "Bosnia & Herzegovina", code = "ba", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_brazil, name = "Brazil", code = "brazilian", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_bulgaria, name = "Bulgaria", code = "bulgarian", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_cambodia, name = "Cambodia", code = "kh", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_cameroon, name = "Cameroon", code = "cm", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_canada, name = "Canada", code = "canadian", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_chile, name = "Chile", code = "cl", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_colombia, name = "Colombia", code = "co", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_congo, name = "Congo - Brazzaville", code = "cg", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_costa_rica, name = "Costa Rica", code = "cr", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_ivory_coast, name = "Côte d’Ivoire", code = "ci", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_croatia, name = "Croatia", code = "croatian", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_czechia, name = "Czechia", code = "czech", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_denmark, name = "Denmark", code = "danish", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_ecuador, name = "Ecuador", code = "ec", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_egypt, name = "Egypt", code = "eg", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_equatorial_guinea, name = "Equatorial Guinea", code = "gq", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_estonia, name = "Estonia", code = "ee", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_ethiopia, name = "Ethiopia", code = "et", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_fiji, name = "Fiji", code = "fj", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_france, name = "France", code = "french", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_germany, name = "Germany", code = "german", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_ghana, name = "Ghana", code = "gh", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_greece, name = "Greece", code = "greek", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_guinea, name = "Guinea", code = "gn", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_hungary, name = "Hungary", code = "hungarian", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_india, name = "India", code = "indian", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_iran, name = "Iran", code = "ir", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_iraq, name = "Iraq", code = "iq", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_ireland, name = "Ireland", code = "irish", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_italy, name = "Italy", code = "italian", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_japan, name = "Japan", code = "japanese", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_kazakhstan, name = "Kazakhstan", code = "kz", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_kenya, name = "Kenya", code = "ke", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_kuwait, name = "Kuwait", code = "kw", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_laos, name = "Laos", code = "la", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_latvia, name = "Latvia", code = "latvian", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_lebanon, name = "Lebanon", code = "lb", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_libya, name = "Libya", code = "ly", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_liechtenstein, name = "Liechtenstein", code = "li", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_lithuania, name = "Lithuania", code = "lithuanian", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_luxembourg, name = "Luxembourg", code = "lu", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_macao, name = "Macao", code = "mo", isSelected = false),//54
            CountryItem(flag = R.drawable.ic_country_flag_malaysia, name = "Malaysia", code = "malaysia", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_mali, name = "Mali", code = "ml", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_malta, name = "Malta", code = "mt", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_mauritius, name = "Mauritius", code = "mu", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_mexico, name = "Mexico", code = "mexican", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_moldova, name = "Moldova", code = "md", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_montenegro, name = "Montenegro", code = "me", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_myanmar, name = "Myanmar (Burma)", code = "mm", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_nepal, name = "Nepal", code = "np", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_netherlands, name = "Netherlands", code = "dutch", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_nicaragua, name = "Nicaragua", code = "ni", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_nigeria, name = "Nigeria", code = "ng", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_north_korea, name = "North Korea", code = "kp", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_macedonia, name = "North Macedonia", code = "mk", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_norway, name = "Norway", code = "norwegian", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_oman, name = "Oman", code = "om", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_pakistan, name = "Pakistan", code = "pk", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_panama, name = "Panama", code = "pa", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_paraguay, name = "Paraguay", code = "py", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_peru, name = "Peru", code = "pe", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_philippines, name = "Philippines", code = "philippines", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_poland, name = "Poland", code = "polish", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_portugal, name = "Portugal", code = "portuguese", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_qatar, name = "Qatar", code = "qa", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_reunion, name = "Réunion", code = "re", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_romania, name = "Romania", code = "romanian", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_russia, name = "Russia", code = "russian", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_rwanda, name = "Rwanda", code = "rw", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_senegal, name = "Senegal", code = "sn", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_serbia, name = "Serbia", code = "rs", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_singapore, name = "Singapore", code = "singapore", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_slovenia, name = "Slovenia", code = "slovenian", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_south_africa, name = "South Africa", code = "sa", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_south_korea, name = "South Korea", code = "south_korea", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_spain, name = "Spain", code = "spain", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_sri_lanka, name = "Sri Lanka", code = "lk", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_switzerland, name = "Switzerland", code = "ch", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_taiwan, name = "Taiwan", code = "taiwan", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_tanzania, name = "Tanzania", code = "tz", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_thailand, name = "Thailand", code = "th", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_trinidad_and_tobago, name = "Trinidad & Tobago", code = "tt", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_tuvalu, name = "Tuvalu", code = "tv", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_uganda, name = "Uganda", code = "ug", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_united_arab_emirates, name = "United Arab Emirates", code = "ae", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_united_kingdom, name = "United Kingdom", code = "uk", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_uruguay, name = "Uruguay", code = "uy", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_uzbekistan, name = "Uzbekistan", code = "uz", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_venezuela, name = "Venezuela", code = "ve", isSelected = false),
            CountryItem(flag = R.drawable.ic_country_flag_zambia, name = "Zambia", code = "zm", isSelected = false),
        )
    }
}
