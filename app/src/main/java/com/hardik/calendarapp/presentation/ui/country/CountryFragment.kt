package com.hardik.calendarapp.presentation.ui.country

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants
import com.hardik.calendarapp.databinding.FragmentCountryBinding
import com.hardik.calendarapp.presentation.MainViewModel
import com.hardik.calendarapp.presentation.ui.MainActivity
import com.hardik.calendarapp.presentation.ui.calendar_month.adapter.CountryAdapter
import kotlinx.coroutines.launch


class CountryFragment : Fragment(R.layout.fragment_country) {
    private final val TAG = Constants.BASE_TAG + CountryFragment::class.java.simpleName

    private val binding get() = _binding!!
    private var _binding: FragmentCountryBinding? = null

    private val viewModel: MainViewModel by activityViewModels()

    private val countryAdapter = CountryAdapter { selectedCountry, isChecked ->
        onCountrySelected(selectedCountry, isChecked)
    }
    // To keep track of selected countries dynamically
    private var selectedCountries = mutableSetOf<String>()
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

        // Step 2: Convert to MutableSet
        selectedCountries = savedCountries.toMutableSet()

        // Step 3: Get country names and country codes from resources
        val countries = resources.getStringArray(R.array.country_entries)
        val countryCodes = resources.getStringArray(R.array.country_values)

        // Step 4: Map country data to CountryItem, setting isSelected based on selectedCountries
        viewModel.initializeCountries(savedCountries, countries, countryCodes)
//        val countryItems = countries.mapIndexed { index, name ->
//            CountryItem(
//                name = name,
//                code = countryCodes[index],
//                isSelected = selectedCountries.contains(countryCodes[index]) // Check if the country is pre-selected
//            )
//        }

        // Step 5: Observe countryItems and update RecyclerView
        lifecycleScope.launch {
            viewModel.countryItems.collect { countryItems ->
                countryAdapter.submitList(countryItems)
            }
        }
        // Step 5: Submit the list to the adapter to update the UI
        //countryAdapter.submitList(countryItems)

        // Step 6: Set up the RecyclerView
        binding.countryRecView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = countryAdapter
        }


        /** Save Selected Country */
        (activity as MainActivity).binding.saveSelectLanguageIcon.setOnClickListener {
            if (isAdded){
                lifecycleScope.launch {
                    saveSelectedCountries(
                        viewModel.countryItems.value
                            .filter { it.isSelected } // Filter only selected items
                            .map { it.code }          // Map to country codes
                            .toSet()                  // Convert to a Set
                    )
                    //saveSelectedCountries(selectedCountries)
                    findNavController().popBackStack(R.id.nav_select_country, inclusive = true)// Pop back two fragments by specifying the fragment ID you want to retain
                }
            }
        }
    }


    private fun saveSelectedCountries(selectedCountries: Set<String>) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        sharedPreferences.edit()
            .putStringSet("countries", selectedCountries)
            .apply()
    }

    private fun onCountrySelected(countryCode: String, isSelected: Boolean) {
        viewModel.toggleCountrySelection(countryCode)
//        if (isSelected) {
//            selectedCountries.add(countryCode) // Add to the selected list
//        } else {
//            selectedCountries.remove(countryCode) // Remove from the selected list
//        }

        // Optional: Update ViewModel or trigger side effects
        //viewModel.getHolidayCalendarData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}