package com.hardik.calendarapp.presentation.ui.setting

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.databinding.FragmentSettingBinding
import com.hardik.calendarapp.presentation.MainViewModel
import com.hardik.calendarapp.presentation.ui.MainActivity
import com.hardik.calendarapp.presentation.ui.new_event.NewEventViewModel
import com.hardik.calendarapp.utillities.LocaleHelper
import com.hardik.calendarapp.utillities.MyNavigation


class SettingFragment : Fragment(R.layout.fragment_setting) {
    private val TAG = BASE_TAG + SettingFragment::class.java.simpleName

    private val viewModel: NewEventViewModel by activityViewModels()
    private val mainViewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate: ")
        arguments?.let {}
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        applyThemeAndLocale()
        return inflater.inflate(R.layout.fragment_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.i(TAG, "onViewCreated: ")
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingBinding.bind(view)

        setupSettingsUI()
    }

    private fun applyThemeAndLocale() {
        // Step 1: Retrieve saved language preference
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val languageCode = sharedPreferences.getString("language", "en") ?: "en"
        val countryCode = sharedPreferences.getStringSet("countries", setOf("indian")) ?: setOf("indian")
        val firstDayOfTheWeek = sharedPreferences.getString("firstDayOfWeek", "Sunday") ?: "Sunday"
        val appTheme = sharedPreferences.getString("app_theme", "system") ?: "system"

        // Step 2: Set the theme before locale
        when (appTheme) {
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        // Step 3: Update the locale after setting the theme and before inflating the UI
        LocaleHelper.setLocale(requireContext(), languageCode)
    }
    private fun setupSettingsUI() {
        binding.apply {
            if (isAdded){

                //todo: personalization:
                includedItemAppTheme.apply {
                    this.imgSettingIcon.apply { setImageResource(R.drawable.setting_app_theme_icon) }
                    this.tvSettingItemTitle.apply { text = getString(R.string.app_theme) }
                    this.imgSettingMoveArrowIcon.apply { setImageResource(R.drawable.setting_move_arrow_icon) }
                    this.constLayItemSetting.setOnClickListener {  (activity as MainActivity).showAppThemeDialog()  }
                }
                includedItemAppLanguage.apply {
                    this.imgSettingIcon.apply { setImageResource(R.drawable.setting_app_language_icon) }
                    this.tvSettingItemTitle.apply { text = getString(R.string.app_language) }
                    this.imgSettingMoveArrowIcon.apply { setImageResource(R.drawable.setting_move_arrow_icon) }
                    this.constLayItemSetting.setOnClickListener {
                        findNavController().navigate(R.id.nav_select_language, null, navOptions = MyNavigation.navOptions, null)
                    }
                }
                includedItemFirstDayOfTheWeek.apply {
                    this.imgSettingIcon.apply { setImageResource(R.drawable.setting_first_day_of_the_week_icon) }
                    this.tvSettingItemTitle.apply { text = getString(R.string.first_day_of_the_week) }
                    this.imgSettingMoveArrowIcon.apply { setImageResource(R.drawable.setting_move_arrow_icon) }
                    this.constLayItemSetting.setOnClickListener {   (activity as MainActivity).showFirstDayOfTheWeek() }
                }
                includedItemJumpToDate.apply {
                    this.imgSettingIcon.apply { setImageResource(R.drawable.setting_jump_to_date_icon) }
                    this.tvSettingItemTitle.apply { text = getString(R.string.jump_to_date) }
                    this.imgSettingMoveArrowIcon.apply { setImageResource(R.drawable.setting_move_arrow_icon) }
                    this.constLayItemSetting.setOnClickListener {(activity as MainActivity).showJumpToDateDialog() }
                }
                includedItemTimeFormat.apply {
                    this.imgSettingIcon.apply { setImageResource(R.drawable.setting_time_format_icon) }
                    this.tvSettingItemTitle.apply { text = getString(R.string.time_format) }
                    this.imgSettingMoveArrowIcon.apply { setImageResource(R.drawable.setting_move_arrow_icon) }
                        this.constLayItemSetting.setOnClickListener { Toast.makeText(requireContext(), "Time Format!",Toast.LENGTH_SHORT).show() }
                }

                //todo: About:
                includedItemPrivacyPolicy.apply {
                    this.imgSettingIcon.apply { setImageResource(R.drawable.setting_privacy_policy_icon) }
                    this.tvSettingItemTitle.apply { text = getString(R.string.privacy_policy) }
                    this.imgSettingMoveArrowIcon.apply { setImageResource(R.drawable.setting_move_arrow_icon) }
                    this.constLayItemSetting.setOnClickListener { (activity as MainActivity).privacyPolicy() }
                }
                includedItemRateOnGooglePlay.apply {
                    this.imgSettingIcon.apply { setImageResource(R.drawable.setting_rate_on_google_play_icon) }
                    this.tvSettingItemTitle.apply { text = getString(R.string.rate_on_google_play) }
                    this.imgSettingMoveArrowIcon.apply { setImageResource(R.drawable.setting_move_arrow_icon) }
                    this.constLayItemSetting.setOnClickListener { (activity as MainActivity).rateApp() }
                }
                includedItemShareApp.apply {
                    this.imgSettingIcon.apply { setImageResource(R.drawable.setting_share_app_icon) }
                    this.tvSettingItemTitle.apply { text = getString(R.string.share_app) }
                    this.imgSettingMoveArrowIcon.apply { setImageResource(R.drawable.setting_move_arrow_icon) }
                    this.constLayItemSetting.setOnClickListener { Toast.makeText(requireContext(), "Share App!",Toast.LENGTH_SHORT).show()}
                }
                includedItemFeedBack.apply {
                    this.imgSettingIcon.apply { setImageResource(R.drawable.setting_feedback_icon) }
                    this.tvSettingItemTitle.apply { text = getString(R.string.feedback) }
                    this.imgSettingMoveArrowIcon.apply { setImageResource(R.drawable.setting_move_arrow_icon) }
                    this.constLayItemSetting.setOnClickListener { Toast.makeText(requireContext(), "Feedback!",Toast.LENGTH_SHORT).show() }
                }
                includedItemDeviceInfo.apply {
                    this.imgSettingIcon.apply { setImageResource(R.drawable.setting_device_info_icon) }
                    this.tvSettingItemTitle.apply { text = getString(R.string.device_info) }
                    this.imgSettingMoveArrowIcon.apply { setImageResource(R.drawable.setting_move_arrow_icon) }
                    this.constLayItemSetting.setOnClickListener { (activity as MainActivity).showDeviceInfoDialog() }
                }
                includedItemVersion.apply {
                    this.imgSettingIcon.apply { setImageResource(R.drawable.setting_version_icon) }
                    this.tvSettingItemTitle.apply { text = getString(R.string.version) }
                    this.imgSettingMoveArrowIcon.apply { setImageResource(R.drawable.setting_move_arrow_icon) }
                    this.constLayItemSetting.setOnClickListener { Toast.makeText(requireContext(), "Version",Toast.LENGTH_SHORT).show() }
                }
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
//            includedItemAppTheme
//            includedItemAppLanguage
//            includedItemFirstDayOfTheWeek
//            includedItemJumpToDate
//            includedItemTimeFormat
//
//            includedItemPrivacyPolicy
//            includedItemRateOnGooglePlay
//            includedItemShareApp
//            includedItemFeedBack
//            includedItemDeviceInfo
//            includedItemVersion