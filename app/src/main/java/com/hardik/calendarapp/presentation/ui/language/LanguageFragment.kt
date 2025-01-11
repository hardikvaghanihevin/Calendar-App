package com.hardik.calendarapp.presentation.ui.language

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants
import com.hardik.calendarapp.databinding.FragmentLanguageBinding
import com.hardik.calendarapp.presentation.MainViewModel
import com.hardik.calendarapp.presentation.adapter.LanguageAdapter
import com.hardik.calendarapp.presentation.adapter.LanguageItem
import com.hardik.calendarapp.presentation.ui.MainActivity
import com.hardik.calendarapp.utillities.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class LanguageFragment : Fragment(R.layout.fragment_language) {
    private final val TAG = Constants.BASE_TAG + LanguageFragment::class.java.simpleName

    private var param1: String? = null

    private val binding get() = _binding!!
    private var _binding: FragmentLanguageBinding? = null

    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var languageEntries: Array<String>
    private lateinit var languageValues: Array<String>
    private var selectedLanguage: String? = null

    // Adapter and items
    private lateinit var languageAdapter: LanguageAdapter
    private lateinit var languageItems: List<LanguageItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString("")
        }
    }

//    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? { // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_language, container, false) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLanguageBinding.bind(view)

        // Load string arrays
        languageEntries = resources.getStringArray(R.array.language_entries)
        languageValues = resources.getStringArray(R.array.language_values)

        // Prepare language items
        val languageItems = languageEntries.mapIndexed { index, language ->
            LanguageItem(language, languageValues[index] == getCurrentLanguage())
        }

        // Set up RecyclerView
        languageAdapter = LanguageAdapter(requireContext(), languageItems) { position ->
            selectedLanguage = languageValues[position]
            // Notify adapter of the selected language
            //saveLanguage(selectedLanguage!!)
        }

        binding.languageRecView.layoutManager = LinearLayoutManager(requireContext())
        binding.languageRecView.adapter = languageAdapter

        /** Save Selected Language */
        (activity as MainActivity).binding.appBarMain.saveSelectLanguageIcon.setOnClickListener {
            if (isAdded){
                lifecycleScope.launch {
                    selectedLanguage?.let { saveLanguage(it) }
                    findNavController().popBackStack(R.id.nav_select_language, inclusive = true)// Pop back two fragments by specifying the fragment ID you want to retain
                }
            }
        }
    }

    private fun getCurrentLanguage(): String {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        return sharedPreferences.getString("language", Locale.getDefault().language) ?: "en"
    }

    private fun saveLanguage(languageCode: String) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        sharedPreferences.edit().putString("language", languageCode).apply()

        // Update the app language
        setAppLanguage(languageCode)

        // Update the ViewModel
        viewModel.updateLanguageCode(languageCode)

        // Notify the user and restart the activity
        Toast.makeText(
            requireContext(),
            "Language updated to ${languageEntries[languageValues.indexOf(languageCode)]}",
            Toast.LENGTH_SHORT
        ).show()
    }

    /*//todo: Here Activity recreate() and blink flicker/Blink. note do not use this method
    private fun setAppLanguage(languageCode: String) {
        LocaleHelper.setLocale(requireContext(), languageCode)
        requireActivity().recreate()
    }*/

    //todo: here Avoid calling recreate() to prevent flicker/Blink.
    private fun setAppLanguage(languageCode: String) {
        // Set the locale using LocaleHelper (or your own helper method)
        LocaleHelper.setLocale(requireContext(), languageCode)

        // Update the configuration of the context
        val resources = requireContext().resources
        val configuration = resources.configuration
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        configuration.setLocale(locale)

        // Create a new context with the updated configuration
        val context = requireContext().createConfigurationContext(configuration)

        // Apply the configuration to the resources
        //resources.updateConfiguration(configuration, resources.displayMetrics)
        //todo: OR
        resources.displayMetrics.setTo(resources.displayMetrics)
        resources.configuration.setTo(configuration)

        // Optionally, update the UI manually or notify the ViewModel if needed
        viewModel.updateLanguageCode(languageCode)

        // Notify the user of the language change
        //Toast.makeText(requireContext(), "Language updated to ${languageEntries[languageValues.indexOf(languageCode)]}", Toast.LENGTH_SHORT).show()
        Log.e(TAG, "setAppLanguage: Language updated to ${languageEntries[languageValues.indexOf(languageCode)]}", )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}