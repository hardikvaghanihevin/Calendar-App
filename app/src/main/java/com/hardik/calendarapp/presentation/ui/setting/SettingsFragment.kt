package com.hardik.calendarapp.presentation.ui.setting

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.activityViewModels
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.presentation.MainViewModel
import com.hardik.calendarapp.utillities.LocaleHelper

class SettingsFragment : PreferenceFragmentCompat() {
    private val TAG = BASE_TAG + SettingsFragment::class.java.simpleName

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var listPreferenceLanguage: ListPreference
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        setListPreferenceLanguage()

        setListPreferenceCountry()
    }

    private fun setListPreferenceCountry() {
        val listPreferenceCountry = findPreference<MultiSelectListPreference>("countries")!!

        // Check if the preference is empty or not initialized
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val savedCountries = sharedPreferences.getStringSet("countries", null)

        val defaultCountries = if (savedCountries.isNullOrEmpty()) {
            setOf("indian") // Default value
        } else {
            savedCountries
        }

        // Set the default value in the preference
        listPreferenceCountry.values = defaultCountries
        updateCountrySummary(listPreferenceCountry, defaultCountries)

        listPreferenceCountry.setOnPreferenceChangeListener { preference, newValue ->
            if (newValue is Set<*>) {
                val selectedCountries = newValue as Set<String>

                // Check if the user is trying to uncheck all items
                if (selectedCountries.isEmpty()) {
                    // Prevent the change and keep the default or current value
                    Log.e(TAG, "At least one country must remain selected!")
                    return@setOnPreferenceChangeListener false // Reject the change
                }

                // Save the new value in shared preferences
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
                val editor = sharedPreferences.edit()
                editor.putStringSet("countries", selectedCountries) // Save the new value
                editor.apply() // Commit changes

                // Update the summary for the preference
                updateCountrySummary(preference as MultiSelectListPreference, selectedCountries)

                Log.e(TAG, "Selected countries: $selectedCountries")

                // Call the ViewModel with the updated values
                viewModel.getHolidayCalendarData()

                true // Allow the change to persist
            } else {
                false // Reject the change if newValue is not a Set
            }
        }
    }
    private fun updateCountrySummary(preference: MultiSelectListPreference, selectedValues: Set<String>? = null) {
        val selectedCountries = selectedValues ?: preference.values
        if (selectedCountries.isEmpty()) {
            preference.summary = "No countries selected"
        } else {
            preference.summary = selectedCountries.joinToString(", ") { value ->
                // Convert entry values to display entries
                val index = preference.entryValues.indexOf(value)
                if (index >= 0) preference.entries[index].toString() else value
            }
        }
    }

    private fun setListPreferenceLanguage() {
        // Find the ListPreference by key
        listPreferenceLanguage  = findPreference<ListPreference>("language")!!
        // Set a listener for preference changes
        listPreferenceLanguage.setOnPreferenceChangeListener { preference, newValue ->
            val selectedValue = newValue as String
            Log.d(TAG, "Language preference changed to: $selectedValue")

            /*
            // Save manually to custom SharedPreferences
            val sharedPreferences = requireContext().getSharedPreferences("my_preferences", Context.MODE_PRIVATE)
            sharedPreferences.edit().putString("language", selectedValue).apply()
            */
            // Update the locale
            LocaleHelper.setLocale(requireContext(), selectedValue)//todo: selectedLanguageCode is selectedValue

            viewModel.updateLanguageCode( languageCode = selectedValue )
            // Recreate the activity to apply the language change
            requireActivity().recreate()
//            viewModel.getHolidayCalendarData()
            true // Return true to persist the change
        }
    }
}