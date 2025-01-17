package com.hardik.calendarapp.presentation.ui.language

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants
import com.hardik.calendarapp.databinding.ActivityLanguageBinding
import com.hardik.calendarapp.presentation.MainViewModel
import com.hardik.calendarapp.presentation.adapter.LanguageAdapter
import com.hardik.calendarapp.presentation.adapter.LanguageItem
import com.hardik.calendarapp.presentation.adapter.getDrawableFromAttribute
import com.hardik.calendarapp.presentation.ui.MainActivity
import com.hardik.calendarapp.utillities.DisplayUtil
import com.hardik.calendarapp.utillities.DisplayUtil.showViewWithAnimation
import com.hardik.calendarapp.utillities.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class LanguageActivity : AppCompatActivity() {

    private val TAG = Constants.BASE_TAG + LanguageActivity::class.java.simpleName

    private lateinit var binding: ActivityLanguageBinding
    private val viewModel: MainViewModel by viewModels()

    private lateinit var languageEntries: Array<String>
    private lateinit var languageValues: Array<String>
    private var selectedLanguage: String? = null

    private lateinit var languageAdapter: LanguageAdapter
    private lateinit var languageItems: List<LanguageItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate: ")
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        loadLanguages()
        setupRecyclerView()
        setupSaveButton()
    }

    private fun setupToolbar() {
        updateToolbarTitle(resources.getString(R.string.select_language))
        hideAllViewsWithAnimation()
        showViewWithAnimation(binding.includedLanguageActivityCustomToolbar.llToolbarMenuIcon3, duration = 0)
        showViewWithAnimation(binding.includedLanguageActivityCustomToolbar.saveSelectLanguageIcon)

        val iconDrawableBackArrow = getDrawableFromAttribute(this, R.drawable.back_arrow_icon)
        binding.includedLanguageActivityCustomToolbar.sivNavigationIcon.apply {
            setImageDrawable(iconDrawableBackArrow)
            setOnClickListener { navigateToMainActivity() }
        }

        lifecycleScope.launch {
            viewModel.toolbarTitle.collectLatest { title ->
                binding.includedLanguageActivityCustomToolbar.toolbarTitle.text = title
            }
        }
    }

    private fun loadLanguages() {
        languageEntries = resources.getStringArray(R.array.language_entries)
        languageValues = resources.getStringArray(R.array.language_values)

        languageItems = languageEntries.mapIndexed { index, language ->
            LanguageItem(language, languageValues[index] == getCurrentLanguage())
        }
    }

    private fun setupRecyclerView() {
        languageAdapter = LanguageAdapter(this, languageItems) { position ->
            selectedLanguage = languageValues[position]
        }

        binding.languageRecView.layoutManager = LinearLayoutManager(this)
        binding.languageRecView.adapter = languageAdapter
    }

    private fun setupSaveButton() {
        binding.includedLanguageActivityCustomToolbar.saveSelectLanguageIcon.setOnClickListener {
            selectedLanguage?.let {
                lifecycleScope.launch {
                    saveLanguage(it)
                }
            }
            navigateToMainActivity()
        }
    }

    private fun navigateToMainActivity() {
        val i = Intent(this@LanguageActivity, MainActivity::class.java)
        startActivity(i)
        finish()
    }

    private fun getCurrentLanguage(): String {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        return sharedPreferences.getString("language", Locale.getDefault().language) ?: "en"
    }

    private fun saveLanguage(languageCode: String) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.edit().putString("language", languageCode).apply()

        setAppLanguage(languageCode)

        viewModel.updateLanguageCode(languageCode)

        Toast.makeText(
            this,
            "Language updated to ${languageEntries[languageValues.indexOf(languageCode)]}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun setAppLanguage(languageCode: String) {
        LocaleHelper.setLocale(this, languageCode)

        viewModel.updateLanguageCode(languageCode)
        viewModel.updateToolbarTitle(viewModel.toolbarTitle.value)

        Log.e(TAG, "setAppLanguage: Language updated to ${languageEntries[languageValues.indexOf(languageCode)]}")

        //recreate()
    }


    private fun hideAllViewsWithAnimation() {
        val viewList = listOf(
            binding.includedLanguageActivityCustomToolbar.llToolbarMenuIcon1,
            binding.includedLanguageActivityCustomToolbar.searchIcon,
            binding.includedLanguageActivityCustomToolbar.backToDateIcon,
            binding.includedLanguageActivityCustomToolbar.llToolbarMenuIcon2,
            binding.includedLanguageActivityCustomToolbar.deleteEventIcon,
            binding.includedLanguageActivityCustomToolbar.saveEventIcon,
            binding.includedLanguageActivityCustomToolbar.llToolbarMenuIcon3,
            binding.includedLanguageActivityCustomToolbar.searchView,
            binding.includedLanguageActivityCustomToolbar.saveSelectLanguageIcon
        )
        viewList.forEach { DisplayUtil.hideViewWithAnimation(it) }
    }

    private fun updateToolbarTitle(title: String) {
        // Dynamically update NavDestination label
        //findNavController().currentDestination?.label = title
        viewModel.updateToolbarTitle(title ?: resources.getString(R.string.app_name))
    }
}
