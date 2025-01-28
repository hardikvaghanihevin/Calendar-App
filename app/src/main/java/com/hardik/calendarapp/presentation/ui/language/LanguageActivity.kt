package com.hardik.calendarapp.presentation.ui.language

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

    private var selectedLanguage: String? = null

    private lateinit var languageAdapter: LanguageAdapter
    private lateinit var languageItems: List<LanguageItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isFirstLaunch = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("isFirstLaunch", true)

        // If it's the first launch, update the SharedPreferences
        if (isFirstLaunch) {
            binding.includedLanguageActivityCustomToolbar.sivNavigationIcon.visibility = View.GONE
            binding.includedLanguageActivityCustomToolbar.toolbarTitle.apply {
                setPadding(resources.getDimensionPixelSize(R.dimen.itemLayoutHorizontalSpacing), 0, 0, 0)
            }
        }

        setupToolbar()
        loadLanguages()
        setupRecyclerView()
        setupSaveButton()
    }

    private fun setupToolbar() {
        updateToolbarTitle(resources.getString(R.string.select_language))
        hideAllViewsWithAnimation()
        showViewWithAnimation(binding.includedLanguageActivityCustomToolbar.llToolbarMenuIcon3, duration = 0)
        showViewWithAnimation(binding.includedLanguageActivityCustomToolbar.saveSelectionIcon)

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
        languageItems = getLanguageList().map { languageItem ->
            languageItem.copy(isSelected = languageItem.code == getCurrentLanguage())
        }
    }

    private fun setupRecyclerView() {
        languageAdapter = LanguageAdapter(this, languageItems) { position ->
            selectedLanguage = languageItems.get(position).code
        }

        binding.languageRecView.apply {
                layoutManager = LinearLayoutManager(context)
                setHasFixedSize(true)

                val margin = resources.getDimension(R.dimen.itemLanguageVerticalSpacing_dev2).toInt()

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
                                outRect.top = margin
                                outRect.bottom = margin
                            }
                            itemCount - 1 -> { // Last item
                                outRect.top = margin
                                outRect.bottom = margin * 2  //0
                            }
                            else -> { // Middle items
                                outRect.top = margin
                                outRect.bottom = margin
                            }
                        }
                    }
                })

                adapter = languageAdapter
            }

    }

    private fun setupSaveButton() {
        binding.includedLanguageActivityCustomToolbar.saveSelectionIcon.setOnClickListener {
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("isFirstLaunch", false).apply()
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

    private fun getCurrentLanguage(): String? {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        return sharedPreferences.getString("language", Locale.getDefault().language)
    }

    private fun saveLanguage(languageCode: String) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.edit().putString("language", languageCode).apply()

        setAppLanguage(languageCode)

        viewModel.updateLanguageCode(languageCode)

        val selectedLanguageName = languageItems.find { it.code == languageCode }?.name
        Toast.makeText(this, "Language updated to $selectedLanguageName", Toast.LENGTH_SHORT).show()
    }

    private fun setAppLanguage(languageCode: String) {
        LocaleHelper.setLocale(this, languageCode)

        viewModel.updateLanguageCode(languageCode)
        viewModel.updateToolbarTitle(viewModel.toolbarTitle.value)
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
            binding.includedLanguageActivityCustomToolbar.saveSelectionIcon
        )
        viewList.forEach { DisplayUtil.hideViewWithAnimation(it) }
    }

    private fun updateToolbarTitle(title: String) {
        viewModel.updateToolbarTitle(title ?: resources.getString(R.string.app_name))
    }

    private fun getLanguageList(): List<LanguageItem> {
        return listOf(
            LanguageItem(name = "English", code = "en", isSelected = false), //English
            LanguageItem(name = "Français", code = "fr", isSelected = false), //French
            LanguageItem(name = "Deutsch", code = "de", isSelected = false), //German
            LanguageItem(name = "हिन्दी", code = "hi", isSelected = false), //Hindi
            LanguageItem(name = "Italiano", code = "it", isSelected = false), //Italian
            LanguageItem(name = "한국어", code = "ko", isSelected = false), //Korean
            LanguageItem(name = "Português", code = "pt", isSelected = false), //Portuguese
            LanguageItem(name = "Русский", code = "ru", isSelected = false), //Russian
            LanguageItem(name = "Español", code = "es", isSelected = false), //Spanish
            LanguageItem(name = "Українська", code = "uk", isSelected = false), //Ukrainian
        )
    }
}
