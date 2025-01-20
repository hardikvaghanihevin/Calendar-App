package com.hardik.calendarapp.presentation.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.Snackbar
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.databinding.ActivityMainBinding
import com.hardik.calendarapp.databinding.DialogAppThemeBinding
import com.hardik.calendarapp.databinding.DialogDeviceInformationBinding
import com.hardik.calendarapp.databinding.DialogFirstDayOfTheWeekBinding
import com.hardik.calendarapp.databinding.DialogJumpToDateBinding
import com.hardik.calendarapp.databinding.DialogTimeFormatBinding
import com.hardik.calendarapp.presentation.MainViewModel
import com.hardik.calendarapp.presentation.adapter.DrawerMenuAdapter
import com.hardik.calendarapp.presentation.adapter.DrawerMenuItem
import com.hardik.calendarapp.presentation.adapter.getDrawableFromAttribute
import com.hardik.calendarapp.presentation.ui.language.LanguageActivity
import com.hardik.calendarapp.utillities.AlarmScheduler
import com.hardik.calendarapp.utillities.DateUtil
import com.hardik.calendarapp.utillities.DisplayUtil
import com.hardik.calendarapp.utillities.DisplayUtil.hideViewWithAnimation
import com.hardik.calendarapp.utillities.DisplayUtil.showViewWithAnimation
import com.hardik.calendarapp.utillities.KeyboardUtils
import com.hardik.calendarapp.utillities.LocaleHelper
import com.hardik.calendarapp.utillities.MyNavigation.navOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private final val TAG = BASE_TAG + MainActivity::class.java.simpleName

    // Use activityViewModels() to share the ViewModel with SplashFullscreenActivity
    val mainViewModel: MainViewModel by viewModels()//by viewModels()//by activityViewModels()

    private lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var binding: ActivityMainBinding
    lateinit var toolbar: Toolbar
    lateinit var drawerLayout: DrawerLayout
    lateinit var navController: NavController
    var drawerMenuAdapter = DrawerMenuAdapter()

    var bundle: Bundle? = null

    companion object{
        //val yearList: Map<Int, Map<Int, List<Int>>> = createYearData(2000,2100, isZeroBased = true)
        //val yearMonthPairList: List<Pair<Int, Int>> = yearList.flatMap { (year, monthsMap) -> monthsMap.keys.map { month -> year to month } }
        const val REQUEST_CODE_CALENDAR_PERMISSIONS = 1
    }

    private val toolbarTitle: TextView? by lazy { binding.appBarMain.includedAppBarMainCustomToolbar.toolbarTitle }
    private fun updateToolbarTitle(title: String?) {
        //toolbarTitle?.text = title ?: resources.getString(R.string.app_name)
        mainViewModel.updateToolbarTitle(title ?: resources.getString(R.string.app_name)) }//title

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate: ")

        // Step 1: Retrieve saved language preference
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val languageCode = sharedPreferences.getString("language", "en") ?: "en"
        val countryCode = sharedPreferences.getStringSet("countries", setOf("indian")) ?: setOf("indian")
        val firstDayOfTheWeek = sharedPreferences.getString("firstDayOfWeek", "Sunday") ?: "Sunday"
        val appTheme = sharedPreferences.getString("app_theme", "system") ?: "system"
        val is24HourFormat = sharedPreferences.getBoolean("time_format", false)

        // Step 2: Set the theme before locale
        when (appTheme) {
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        // Step 3: Update the locale after setting the theme and before inflating the UI
        LocaleHelper.setLocale(this, languageCode)

        super.onCreate(savedInstanceState)

        // Step 4: Inflate the layout and set the content view
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //binding.drawerLayout.setScrimColor(ContextCompat.getColor(this, R.color.blue))
        //binding.appBarMain.mainContent.foreground = ColorDrawable(ContextCompat.getColor(this@MainActivity, R.color.scrim_color))

        checkAndRequestCalendarPermissions()//todo: 1 get calendar permission and set locale calendar data before API data get

        setupNavigation() //setupToolbar Function: Modularized toolbar configuration and listeners.
        setupToolbar() //setupNavigation Function: Centralized navigation setup, including AppBarConfiguration.
        setupDrawerHeader()
        setupDrawerMenu() //setupDrawerMenu Function: Cleanly handles drawer menu initialization.
        handelBackPressed()

        binding.appBarMain.fab.setOnClickListener { view ->
            Log.i(TAG, "onCreate: clicked fab:")

            bundle = (bundle ?: Bundle()).apply {
                //putParcelable(Constants.KEY_EVENT, dummyEvent.copy( year = "2025", month = "2", date = "0") )
            }

            navController.navigate(R.id.newEventFragment, null, navOptions)
        }

        mainViewModel.getHolidayCalendarData() //todo: 2 getting api data after getting locale calendar data
        // Collecting the StateFlow
        lifecycleScope.launch {

            mainViewModel.toolbarTitle.collectLatest { title->
                binding.appBarMain.includedAppBarMainCustomToolbar.toolbarTitle.text = title
            }

            mainViewModel.holidayApiState.collect { dataState ->
                if (dataState.isLoading) {
                    // Show loading indicator
                    Log.d(TAG, "onCreate: Progressing")
                } else if (dataState.error.isNotEmpty()) {
                    // Show error message
                    Toast.makeText(this@MainActivity, dataState.error, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "onCreate: hide Progressing")
                } else {
                    // Update UI with the user list
                    val users = dataState.data
                    Log.d(TAG, "onCreate: hide Progressing")
//                 userAdapter.differ.submitList(users.toList())
//                 binding.recyclerview.setPadding(0, 0, 0, 0)
                }
            }
        }
        navController.addOnDestinationChangedListener { navCont: NavController, destination: NavDestination, _ ->
            updateToolbarAndViews(navController, destination)
        }
    }

    fun updateToolbarAndViews(navController: NavController, destination: NavDestination) {
        updateToolbarTitle(destination.label.toString().takeIf { !it.isEmpty() } ?: getString(R.string.app_name))
        invalidateOptionsMenu()
        hideAllViewsWithAnimation()

        when (destination.id) {
            // Destinations where FAB/Menu should be hidden
            R.id.newEventFragment -> {
                showViewWithAnimation(binding.appBarMain.includedAppBarMainCustomToolbar.llToolbarMenuIcon2, duration = 0)
                showViewWithAnimation(binding.appBarMain.includedAppBarMainCustomToolbar.saveEventIcon)
            }

            // Destinations where Save Event Icon should be shown
            R.id.viewEventFragment -> {
                showViewWithAnimation(binding.appBarMain.includedAppBarMainCustomToolbar.llToolbarMenuIcon2, duration = 0)
                showViewWithAnimation(binding.appBarMain.includedAppBarMainCustomToolbar.deleteEventIcon)
                showViewWithAnimation(binding.appBarMain.includedAppBarMainCustomToolbar.saveEventIcon)//but it's for edit option not save use
            }

            // Destinations for Year and Month navigation
            R.id.nav_year, R.id.nav_month -> {
                showViewWithAnimation(binding.appBarMain.includedAppBarMainCustomToolbar.llToolbarMenuIcon1, duration = 0)
                showViewWithAnimation(binding.appBarMain.includedAppBarMainCustomToolbar.searchIcon)
                showViewWithAnimation(binding.appBarMain.includedAppBarMainCustomToolbar.backToDateIcon)
                showViewWithAnimation(binding.appBarMain.fab)
            }

            R.id.nav_select_country -> {
                showViewWithAnimation(binding.appBarMain.includedAppBarMainCustomToolbar.llToolbarMenuIcon3, duration = 0)
                showViewWithAnimation(binding.appBarMain.includedAppBarMainCustomToolbar.searchView)
                showViewWithAnimation(binding.appBarMain.includedAppBarMainCustomToolbar.saveSelectLanguageIcon)
            }

            R.id.nav_select_language -> {}

            R.id.nav_setting -> { }// todo: own setting fragment

            R.id.repeatOptionFragment -> {
                showViewWithAnimation(binding.appBarMain.includedAppBarMainCustomToolbar.llToolbarMenuIcon3, duration = 0)
                showViewWithAnimation(binding.appBarMain.includedAppBarMainCustomToolbar.saveSelectLanguageIcon)
            }

            R.id.alertOptionFragment -> {
                showViewWithAnimation(binding.appBarMain.includedAppBarMainCustomToolbar.llToolbarMenuIcon3, duration = 0)
                showViewWithAnimation(binding.appBarMain.includedAppBarMainCustomToolbar.saveSelectLanguageIcon)
            }

            R.id.searchEventFragment -> {
                showViewWithAnimation(binding.appBarMain.includedAppBarMainCustomToolbar.llToolbarMenuIcon3, duration = 0)
                showViewWithAnimation(binding.appBarMain.includedAppBarMainCustomToolbar.searchView)
            }

            // Default case: Hide everything except FAB
            else -> {
                hideAllViewsWithAnimation()
            }
        }
    }

    private var dialogFirstDayOfTheWeekBinding: DialogFirstDayOfTheWeekBinding? = null
    fun showFirstDayOfTheWeek(){
        Log.i(TAG, "showFirstDayOfTheWeek: ")
        val dialogView = layoutInflater.inflate(R.layout.dialog_first_day_of_the_week, null)
        dialogFirstDayOfTheWeekBinding = DialogFirstDayOfTheWeekBinding.bind(dialogView)

        // Create and display the dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Set background to transparent if needed
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        //dialog.window?.setBackgroundDrawableResource(android.R.drawable.screen_background_light_transparent) // Set your background drawable here

        // Ensure the dialog's size wraps the content
        dialog.setOnShowListener {
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT, // Width
                ViewGroup.LayoutParams.WRAP_CONTENT  // Height
            )
        }

        dialog.setCancelable(true)

        dialogFirstDayOfTheWeekBinding?.apply {
            val sundayText = this.dialogFirstDayOfTheWeekSunday
            val mondayText = this.dialogFirstDayOfTheWeekMonday
            val saturdayText = this.dialogFirstDayOfTheWeekSaturday

            //rest all icon and text
            fun resetSelections(){

                sundayText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
                mondayText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
                saturdayText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))

                sundayText.typeface = ResourcesCompat.getFont(this@MainActivity, R.font.post_nord_sans_regular)
                mondayText.typeface = ResourcesCompat.getFont(this@MainActivity, R.font.post_nord_sans_regular)
                saturdayText.typeface = ResourcesCompat.getFont(this@MainActivity, R.font.post_nord_sans_regular)

            }


            // Set the initial selection based on the saved preference
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this@MainActivity)
            val firstDayOfTheWeek = sharedPreferences.getString("firstDayOfWeek", "Sunday")// Default to Sunday

            when (firstDayOfTheWeek) {
                "Sunday" -> {
                    sundayText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent_primary))
                    sundayText.typeface = ResourcesCompat.getFont(this@MainActivity, R.font.post_nord_sans_medium)
                }
                "Monday" -> {
                    mondayText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent_primary))
                    mondayText.typeface = ResourcesCompat.getFont(this@MainActivity, R.font.post_nord_sans_medium)
                }
                "Saturday" -> {
                    saturdayText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent_primary))
                    saturdayText.typeface = ResourcesCompat.getFont(this@MainActivity, R.font.post_nord_sans_medium)
                }
            }

            // Handle Sunday click
            sundayText.setOnClickListener {
                resetSelections()

                sundayText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent_primary))
                sundayText.typeface = ResourcesCompat.getFont(this@MainActivity, R.font.post_nord_sans_medium)

                // Save selection to SharedPreferences
                sharedPreferences.edit().putString("firstDayOfWeek", "Sunday").apply()
            }

            // Handle Monday click
            mondayText.setOnClickListener {
                resetSelections()

                mondayText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent_primary))
                mondayText.typeface = ResourcesCompat.getFont(this@MainActivity, R.font.post_nord_sans_medium)

                // Save selection to SharedPreferences
                sharedPreferences.edit().putString("firstDayOfWeek", "Monday").apply()
            }

            // Handle Saturday click
            saturdayText.setOnClickListener {
                resetSelections()

                saturdayText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent_primary))
                saturdayText.typeface = ResourcesCompat.getFont(this@MainActivity, R.font.post_nord_sans_medium)

                // Save selection to SharedPreferences
                sharedPreferences.edit().putString("firstDayOfWeek", "Saturday").apply()
            }
        }

        dialogFirstDayOfTheWeekBinding?.apply {

            btnDone.setOnClickListener {
                lifecycleScope.launch {
                    // Make sure the navigation happens on the main thread
                    PreferenceManager.getDefaultSharedPreferences(this@MainActivity).getString("firstDayOfWeek", "Sunday")
                        ?.let { it1 -> mainViewModel.updateFirstDayOfTheWeek(refresh = it1) }
                }
                dialog.dismiss()
            }

            btnCancel.setOnClickListener { dialog.dismiss() }
        }

        dialog.show()
    }

    private var dialogJumpToDateBinding: DialogJumpToDateBinding? = null
    fun showJumpToDateDialog() {
        Log.i(TAG, "showJumpToDateDialog: ")
        val dialogView = layoutInflater.inflate(R.layout.dialog_jump_to_date, null)
        dialogJumpToDateBinding = DialogJumpToDateBinding.bind(dialogView)

        // Create and display the dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Set background to transparent if needed
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        //dialog.window?.setBackgroundDrawableResource(android.R.drawable.screen_background_light_transparent) // Set your background drawable here

        // Ensure the dialog's size wraps the content
        dialog.setOnShowListener {
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT, // Width
                ViewGroup.LayoutParams.WRAP_CONTENT  // Height
            )
        }

        dialog.setCancelable(true)

        dialogJumpToDateBinding?.apply {
            yearPicker.apply {
                minValue = 2000
                maxValue = 2100
                //value = Calendar.getInstance().get(Calendar.YEAR)//2025

                lifecycleScope.launch {
                    mainViewModel.yearJTD.collectLatest {
                        value = it
                    }
                }

                this.setOnValueChangedListener { picker, oldVal, newVal ->
                    mainViewModel.updateYearJTD( year = newVal )
                }
            }
            monthPicker.apply {
                minValue = 1
                maxValue = 12
                //value = Calendar.getInstance().get(Calendar.MONTH) + 1 //12

                lifecycleScope.launch {
                    mainViewModel.monthJTD.collectLatest {
                        value = it
                    }
                }

                this.setOnValueChangedListener { picker, oldVal, newVal ->
                    mainViewModel.updateMonthJTD( month = newVal )
                }
            }
            datePicker.apply {
                minValue = 1
                //maxValue = 28
                //value = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

                lifecycleScope.launch {
                    mainViewModel.dateMaxJTD.collectLatest {
                        maxValue = it
                    }
                }

                lifecycleScope.launch {
                    mainViewModel.dateJTD.collectLatest {
                        value = it
                    }
                }

                this.setOnValueChangedListener { picker, oldVal, newVal ->
                    mainViewModel.updateDateJTD( date = newVal )
                }

               /* // Listen for changes in the month picker value
                monthPicker.setOnValueChangedListener { _, _, newMonth ->

                    val maxDays = getMinMaxDays(yearPicker.value, newMonth -1 )

                    datePicker.maxValue = maxDays.second ?: 1
                }*/
            }

            lifecycleScope.launch {
                mainViewModel.fullDateJTD.collectLatest {
                    tvSelectedDate.text = it //todo set fullDate (eg.Monday 1 January 2025)
                }
            }
            btnJump.setOnClickListener {
                val selectedYear = yearPicker.value
                val selectedMonth = monthPicker.value
                val selectedDay = datePicker.value
                // Perform your "Jump to Date" logic here
                Log.d(TAG, "showJumpToDateDialog: $selectedYear - $selectedMonth - $selectedDay")

                lifecycleScope.launch {
                    // Make sure the navigation happens on the main thread
                    Log.e(TAG, "navigateToCalendarMonth:  ${Thread.currentThread().name}")
                    val bundle = Bundle().apply {
                        putInt(Constants.KEY_YEAR, selectedYear)
                        putInt(Constants.KEY_MONTH, selectedMonth -1)
                        putInt(Constants.KEY_DAY, selectedDay)
                    }
                    //navController.popBackStack()//for repeat entry clear
                    navController.navigate(R.id.nav_month, bundle, navOptions)
                }

                dialog.dismiss()
            }
            btnCancel.setOnClickListener { dialog.dismiss() }
        }

        dialog.show()
    }

    private var dialogAppThemeBinding: DialogAppThemeBinding? = null
    fun showAppThemeDialog(){
        Log.i(TAG, "showAppThemeDialog: ")
        val dialogView = layoutInflater.inflate(R.layout.dialog_app_theme, null)
        dialogAppThemeBinding = DialogAppThemeBinding.bind(dialogView)

        // Create and display the dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Set background to transparent if needed
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        //dialog.window?.setBackgroundDrawableResource(android.R.drawable.screen_background_light_transparent) // Set your background drawable here

        // Ensure the dialog's size wraps the content
        dialog.setOnShowListener {
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT, // Width
                ViewGroup.LayoutParams.WRAP_CONTENT  // Height
            )
        }

        dialog.setCancelable(true)

        dialogAppThemeBinding?.apply {
            val darkThemeIcon = this.dialogAppThemeDarkThemeIcon
            val darkThemeText = this.dialogAppThemeDarkTheme
            val lightThemeIcon = this.dialogAppThemeLightThemeIcon
            val lightThemeText = this.dialogAppThemeLightTheme
            val systemThemeIcon = this.dialogAppThemeSystemThemeIcon
            val systemThemeText = this.dialogAppThemeSystemTheme

            //rest all icon and text
            fun resetSelections(){
                darkThemeIcon.setImageResource(R.drawable.icon_unchecked)
                lightThemeIcon.setImageResource(R.drawable.icon_unchecked)
                systemThemeIcon.setImageResource(R.drawable.icon_unchecked)

                darkThemeText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
                lightThemeText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
                systemThemeText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))

                darkThemeText.typeface = ResourcesCompat.getFont(this@MainActivity, R.font.post_nord_sans_regular)
                lightThemeText.typeface = ResourcesCompat.getFont(this@MainActivity, R.font.post_nord_sans_regular)
                systemThemeText.typeface = ResourcesCompat.getFont(this@MainActivity, R.font.post_nord_sans_regular)

            }

            // Set the initial selection based on the saved preference
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this@MainActivity)
            val appTheme = sharedPreferences.getString("app_theme", "system")// Default to system theme

            when(appTheme){
                "dark" -> {
                    darkThemeIcon.setImageResource(R.drawable.icon_checked)
                    darkThemeText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent_primary))
                    darkThemeText.typeface = ResourcesCompat.getFont(this@MainActivity, R.font.post_nord_sans_medium)
                }
                "light" -> {
                    lightThemeIcon.setImageResource(R.drawable.icon_checked)
                    lightThemeText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent_primary))
                    lightThemeText.typeface = ResourcesCompat.getFont(this@MainActivity, R.font.post_nord_sans_medium)
                }
                "system" -> {
                    systemThemeIcon.setImageResource(R.drawable.icon_checked)
                    systemThemeText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent_primary))
                    systemThemeText.typeface = ResourcesCompat.getFont(this@MainActivity, R.font.post_nord_sans_medium)
                }
            }

            // Handle darkTheme click
            darkThemeText.setOnClickListener {
                resetSelections()

                darkThemeIcon.setImageResource(R.drawable.icon_checked)
                darkThemeText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent_primary))
                darkThemeText.typeface = ResourcesCompat.getFont(this@MainActivity, R.font.post_nord_sans_medium)

                //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES) // Dark Theme

                // Save selection to SharedPreferences
                sharedPreferences.edit().putString("app_theme", "dark").apply()

            }

            // Handle lightTheme click
            lightThemeText.setOnClickListener {
                resetSelections()

                lightThemeIcon.setImageResource(R.drawable.icon_checked)
                lightThemeText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent_primary))
                lightThemeText.typeface = ResourcesCompat.getFont(this@MainActivity, R.font.post_nord_sans_medium)

                //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) // Light Theme

                // Save selection to SharedPreferences
                sharedPreferences.edit().putString("app_theme", "light").apply()

            }

            // Handle systemTheme click
            systemThemeText.setOnClickListener {
                resetSelections()

                systemThemeIcon.setImageResource(R.drawable.icon_checked)
                systemThemeText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent_primary))
                systemThemeText.typeface = ResourcesCompat.getFont(this@MainActivity, R.font.post_nord_sans_medium)

                //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) // System Default Theme

                // Save selection to SharedPreferences
                sharedPreferences.edit().putString("app_theme", "system").apply()

            }

            btnDone.setOnClickListener {
                val selectedTheme = sharedPreferences.getString("app_theme", "system") // Get saved preference

                // Apply the selected theme
                when (selectedTheme) {
                    "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES) // Dark Theme
                    "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) // Light Theme
                    "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) // System Default Theme
                }
                dialog.dismiss()
            }

            btnCancel.setOnClickListener { dialog.dismiss() }
        }

        dialog.show()
    }

    private var dialogTimeFormatBinding: DialogTimeFormatBinding? = null
    fun showTimeFormatDialog(){
        Log.i(TAG, "showTimeFormat: ")
        val dialogView = layoutInflater.inflate(R.layout.dialog_time_format, null)
        dialogTimeFormatBinding = DialogTimeFormatBinding.bind(dialogView)

        // Create and display the dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Set background to transparent if needed
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        //dialog.window?.setBackgroundDrawableResource(android.R.drawable.screen_background_light_transparent) // Set your background drawable here

        // Ensure the dialog's size wraps the content
        dialog.setOnShowListener {
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT, // Width
                ViewGroup.LayoutParams.WRAP_CONTENT  // Height
            )
        }

        dialog.setCancelable(true)

        dialogTimeFormatBinding?.apply {
            val timeFormat12hr = this.dialogTimeFormat12HR
            val timeFormat12hrIcon = this.dialogTimeFormat12HRIcon
            val timeFormat24hr = this.dialogTimeFormat24HR
            val timeFormat24hrIcon = this.dialogTimeFormat24HRIcon

            //rest all icon and text
            fun resetSelections(){
                timeFormat12hrIcon.setImageResource(R.drawable.icon_unchecked)
                timeFormat24hrIcon.setImageResource(R.drawable.icon_unchecked)

                timeFormat12hr.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
                timeFormat24hr.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))

                timeFormat12hr.typeface = ResourcesCompat.getFont(this@MainActivity, R.font.post_nord_sans_regular)
                timeFormat24hr.typeface = ResourcesCompat.getFont(this@MainActivity, R.font.post_nord_sans_regular)
            }

            // Set the initial selection based on the saved preference
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this@MainActivity)
            var is24HourFormat = sharedPreferences.getBoolean("time_format", false)  // Default to false (12-hour format)

            if(is24HourFormat){
                timeFormat24hrIcon.setImageResource(R.drawable.icon_checked)
                timeFormat24hr.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent_primary))
                timeFormat24hr.typeface = ResourcesCompat.getFont(this@MainActivity, R.font.post_nord_sans_medium)
            }else {
                timeFormat12hrIcon.setImageResource(R.drawable.icon_checked)
                timeFormat12hr.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent_primary))
                timeFormat12hr.typeface = ResourcesCompat.getFont(this@MainActivity, R.font.post_nord_sans_medium)
            }

            // Handle timeFormat12hr click
            timeFormat12hr.setOnClickListener {
                resetSelections()

                timeFormat12hrIcon.setImageResource(R.drawable.icon_checked)
                timeFormat12hr.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent_primary))
                timeFormat12hr.typeface = ResourcesCompat.getFont(this@MainActivity, R.font.post_nord_sans_medium)

                // Save selection to SharedPreferences
                is24HourFormat = false
            }
            // Handle timeFormat24hr click
            timeFormat24hr.setOnClickListener {
                resetSelections()

                timeFormat24hrIcon.setImageResource(R.drawable.icon_checked)
                timeFormat24hr.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent_primary))
                timeFormat24hr.typeface = ResourcesCompat.getFont(this@MainActivity, R.font.post_nord_sans_medium)

                // Save selection to SharedPreferences
                is24HourFormat = true
            }

            btnDone.setOnClickListener {

                // Apply the selected timeFormat
                // Save the selected time format (true for 24-hour format, false for 12-hour format)
                sharedPreferences.edit().putBoolean("time_format", is24HourFormat).apply()

                dialog.dismiss()
            }

            btnCancel.setOnClickListener { dialog.dismiss() }
        }

        dialog.show()
    }

    private var dialogDeviceInformationBinding: DialogDeviceInformationBinding? = null
    @SuppressLint("SetTextI18n")
    fun showDeviceInfoDialog(){
        Log.i(TAG, "showDeviceInfoDialog: ")
        val dialogView = layoutInflater.inflate(R.layout.dialog_device_information, null)
        dialogDeviceInformationBinding = DialogDeviceInformationBinding.bind(dialogView)

        // Create and display the dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Set background to transparent if needed
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        //dialog.window?.setBackgroundDrawableResource(android.R.drawable.screen_background_light_transparent) // Set your background drawable here

        // Ensure the dialog's size wraps the content
        dialog.setOnShowListener {
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT, // Width
                ViewGroup.LayoutParams.WRAP_CONTENT  // Height
            )
        }

        dialog.setCancelable(true)

        dialogDeviceInformationBinding?.apply {
            val brand = Build.BRAND // Brand of the device
            val device = Build.DEVICE // Device codename
            val os = "Android ${Build.VERSION.RELEASE}" // OS version
            val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown ABI" // ABI info
            //val version = Build.DISPLAY // Build version
            // Sanitize version string by removing 'release-keys' or any sensitive information
            val version = Build.DISPLAY.replace("release-keys", "").trim()

            val appVersion = try {
                val packageInfo: PackageInfo = packageManager.getPackageInfo(packageName, 0)
                packageInfo.versionName // App version name
            } catch (e: PackageManager.NameNotFoundException) {
                "Unknown Version"
            }

            this.dialogDeviceInformationBrand.apply { text = "$text: $brand" }
            this.dialogDeviceInformationDevice.apply { text = "$text: $device" }
            this.dialogDeviceInformationAppVersion.apply { text = "$text: $appVersion" }
            this.dialogDeviceInformationOs.apply { text = "$text: $os" }
            this.dialogDeviceInformationABI.apply { text = "$text: $abi" }
            this.dialogDeviceInformationVersion.apply { text = "$text: $version" }

            this.btnOkay.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    fun rateApp(context: Context = this){
        //Toast.makeText(context,"Rate App", Toast.LENGTH_SHORT).show()
        val appPackageName = context.packageName // Get the current app's package name
        //val appPackageName = "com.dts.freefiremax" // Todo: dummy Get the current app's package name
        try {
            // Try to open Play Store app
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // If Play Store app is not available, open in the browser
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
    fun privacyPolicy(context: Context = this) {
        //Toast.makeText(this, "Privacy Policy", Toast.LENGTH_SHORT).show()
        val url = "https://gist.githubusercontent.com/hardikvaghanihevin/d45b7376a72f832d2e80573a46628a4c/raw/e79d8fd9bd36d38b31e619bdb41251a7417999a4/privacy_policy.html" // Replace with your URL
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    fun shareApp(){
        val appPackageName = this.packageName // Get the current app's package name
        val appLink = "https://play.google.com/store/apps/details?id=$appPackageName"

        // Create an intent to share the link
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Check out this app!") // Optional subject
            putExtra(Intent.EXTRA_TEXT, "Hey, check out this app: $appLink") // The link to share
        }

        // Start the sharing activity
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }
    @SuppressLint("QueryPermissionsNeeded")
    fun feedback(){
        val emailAddress = "feedback@company.com"// Replace with your company's email address
        val subject = "Feedback for Your App"
        val body = "Please provide your feedback here."

        // Encode the subject and body in the mailto URI
        /*val uri = Uri.parse("mailto:$emailAddress")// Set email address
            .buildUpon()
            .appendQueryParameter("subject", subject)// Pre-fill subject
            .appendQueryParameter("body", body)// Pre-fill body
            .build()*/

        // Intent for sending email
        var emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$emailAddress") // Set email address
            putExtra(Intent.EXTRA_SUBJECT, subject) // Pre-fill subject
            putExtra(Intent.EXTRA_TEXT, body) // Pre-fill body
        }

        // Try to explicitly target Gmail if installed
        val gmailPackage = "com.google.android.gm"
        val packageManager = this.packageManager
        val isGmailInstalled = packageManager.getLaunchIntentForPackage(gmailPackage) != null

        if (isGmailInstalled) {
            // Gmail is installed; explicitly open Gmail
            emailIntent.setPackage(gmailPackage)
        }

        // Verify that there is an app to handle the intent
        if (emailIntent.resolveActivity(packageManager) != null) {
            // Start the intent (Gmail if installed, else show chooser)
            startActivity(Intent.createChooser(emailIntent, "Choose an email client"))
        } else {
            // No email client found
            Toast.makeText(this, "Please install an email client to send feedback.", Toast.LENGTH_SHORT).show()

            // If no email client is found, we create an alternative fallback using ACTION_SEND
            emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822" // Ensures email apps handle this
                putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress)) // Email address
                putExtra(Intent.EXTRA_SUBJECT, subject) // Pre-fill subject
                putExtra(Intent.EXTRA_TEXT, body) // Pre-fill body
            }

            // Show chooser to select an email client
            startActivity(Intent.createChooser(emailIntent, "Choose an email client"))
        }

    }

    /**
     * Sets up the navigation component and the app bar configuration.
     */
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_year), // Top-level destinations
            binding.drawerLayout
        )

        // Listen for destination changes to update the navigation icon
        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateSelectedDrawerItem(destination.id)
            val navIcon = binding.appBarMain.includedAppBarMainCustomToolbar.customToolbar.findViewById<ShapeableImageView>(R.id.siv_navigation_icon)
            if (appBarConfiguration.topLevelDestinations.contains(destination.id)) {
                //navIcon.setImageResource(R.drawable.hamburger_icon)
                val iconDrawable = getDrawableFromAttribute(this, R.drawable.hamburger_icon)
                navIcon.setImageDrawable(iconDrawable)
                navIcon.contentDescription = getString(R.string.open_drawer)
            } else {
                //navIcon.setImageResource(R.drawable.back_arrow)
                val iconDrawableBackArrow = getDrawableFromAttribute(this, R.drawable.back_arrow_icon)
                navIcon.setImageDrawable(iconDrawableBackArrow)
                navIcon.contentDescription = getString(R.string.navigate_up)
            }
        }
    }

    private fun updateSelectedDrawerItem(selectedId: Int) {
        val adapter = binding.drawerRecyclerView.adapter
        if (adapter is DrawerMenuAdapter) {
            adapter.updateSelectedItem(selectedId)
        } else {
            Log.e("MainActivity", "Adapter is not of type DrawerMenuAdapter")
        }
    }

    /**
     * Configures the custom toolbar and sets up click listeners for navigation and actions.
     */
    private fun setupToolbar() {
        val navIcon = binding.appBarMain.includedAppBarMainCustomToolbar.customToolbar.findViewById<ShapeableImageView>(R.id.siv_navigation_icon)
        navIcon.setOnClickListener {
            DisplayUtil.isKeyboardVisible(this) { isVisible -> if (isVisible) { KeyboardUtils.hideKeyboard(this,binding.root) } }
            handleNavigationIconClick()  }

        binding.appBarMain.includedAppBarMainCustomToolbar.searchIcon.setOnClickListener {
            //Toast.makeText(this, "Search clicked", Toast.LENGTH_SHORT).show()

            // todo: navigate to show all events
            navController.navigate(R.id.searchEventFragment, null, navOptions = navOptions, null)

            if (navController.currentDestination?.id == R.id.nav_select_country) {
                showViewWithAnimation(binding.appBarMain.includedAppBarMainCustomToolbar.searchView)
                // Expand SearchView
                binding.appBarMain.includedAppBarMainCustomToolbar.searchView.setIconified(false)

                // Request focus to display keyboard
                binding.appBarMain.includedAppBarMainCustomToolbar.searchView.requestFocus()
            }
            else if (navController.currentDestination?.id == R.id.nav_select_country) {
                showViewWithAnimation(binding.appBarMain.includedAppBarMainCustomToolbar.searchView)
            }
            else {
                Log.e(TAG,"NavigationError ->: Destination on SearchView")
            }
        }

        binding.appBarMain.includedAppBarMainCustomToolbar.backToDateIcon.apply {
            text = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString()
        }

        binding.appBarMain.includedAppBarMainCustomToolbar.saveEventIcon.apply {}

    }

    @SuppressLint("SetTextI18n")
    private fun setupDrawerHeader(){
        val currentDate: Int = DateUtil.getCurrentDate()
        val currentDay: String = DateUtil.getCurrentDay()
        val currentMonth: Any = DateUtil.getCurrentMonth(isString = true)
        val currentYear: Int = DateUtil.getCurrentYear()

        binding.includedActivityMainHeader.apply {
            tvNavHeaderDate.text = currentDate.toString()
            tvNavHeaderDay.text = currentDay
            tvNavHeaderMonthNameYear.text = "$currentMonth $currentYear"
        }
    }

    /**
     * Configures the drawer menu items and their click listeners.
     */
    fun setupDrawerMenu() {
        val drawerMenuItems = listOf(
            DrawerMenuItem(R.drawable.year_icon, getString(R.string.year), R.id.nav_year,true),
            DrawerMenuItem(R.drawable.month_icon, getString(R.string.month), R.id.nav_month),
            DrawerMenuItem(R.drawable.select_country_icon, getString(R.string.select_country), R.id.nav_select_country),
            DrawerMenuItem(R.drawable.select_language_icon, getString(R.string.select_language), R.id.nav_select_language),
            DrawerMenuItem(R.drawable.first_day_of_the_week_icon, getString(R.string.first_day_of_the_week), R.id.nav_first_day_of_week),
            DrawerMenuItem(R.drawable.jump_to_date_icon, getString(R.string.jump_to_date), R.id.nav_jump_to_date),
            //DrawerMenuItem(R.attr.iconTheme, getString(R.string.app_theme), R.id.nav_app_theme),
            //DrawerMenuItem(R.attr.iconRateApp, getString(R.string.rate_our_app), R.id.nav_rate_app),
            DrawerMenuItem(R.drawable.privacy_policy_icon, getString(R.string.privacy_policy), R.id.nav_privacy_policy),
            //DrawerMenuItem(R.attr.iconDeviceInfo, getString(R.string.device_information), R.id.nav_device_info),
            //DrawerMenuItem(R.attr.iconSetting, "Settings", R.id.nav_settings),
            DrawerMenuItem(R.drawable.setting_icon, getString(R.string.setting), R.id.nav_setting)
        )

        // Initialize the adapter
//        val drawerMenuAdapter = DrawerMenuAdapter(drawerMenuItems) { menuItem ->
//            // Handle item click
//            handleMenuClick(menuItem)
//        }
        drawerMenuAdapter.setItems(drawerMenuItems)
        drawerMenuAdapter.setOnClickListener{ menuItem, pos -> handleMenuClick(menuItem) }


        // Set the adapter to the RecyclerView
        binding.drawerRecyclerView.adapter = drawerMenuAdapter
        binding.drawerRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    /**
     * Handles click events on drawer menu items.
     */
    private fun handleMenuClick(item: DrawerMenuItem) {
        when (item.id) {
            R.id.nav_year -> navController.navigate(R.id.nav_year)
            R.id.nav_month -> navController.navigate(R.id.nav_month)
            R.id.nav_select_country -> navController.navigate(R.id.nav_select_country, null, navOptions = navOptions, null)
            R.id.nav_select_language -> {
                val intent = Intent(this@MainActivity, LanguageActivity::class.java)
                startActivity(intent)
                finish()
            }
            R.id.nav_first_day_of_week -> showFirstDayOfTheWeek()
            R.id.nav_jump_to_date -> showJumpToDateDialog()
            R.id.nav_privacy_policy -> { privacyPolicy() }
            R.id.nav_setting -> navController.navigate(R.id.nav_setting)
        }

        // Update selected item in the drawer
        updateSelectedDrawerItem(item.id)

        toggleDrawer()
    }

    /**
     * Handles navigation icon click events (hamburger/back icon).
     */
    private fun handleNavigationIconClick() {
        val currentDestination = navController.currentDestination
        if (currentDestination != null && appBarConfiguration.topLevelDestinations.contains(currentDestination.id)) {
            toggleDrawer()
        } else {
            navController.navigateUp(appBarConfiguration)
        }
    }

    /**
     * Toggles the drawer state (open/close).
     */
    private fun toggleDrawer() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }


    private fun hideAllViewsWithAnimation() {
        val viewList = listOf(
            binding.appBarMain.includedAppBarMainCustomToolbar.llToolbarMenuIcon1,
            binding.appBarMain.fab,
            binding.appBarMain.includedAppBarMainCustomToolbar.searchIcon,
            binding.appBarMain.includedAppBarMainCustomToolbar.backToDateIcon,
            binding.appBarMain.includedAppBarMainCustomToolbar.llToolbarMenuIcon2,
            binding.appBarMain.includedAppBarMainCustomToolbar.deleteEventIcon,
            binding.appBarMain.includedAppBarMainCustomToolbar.saveEventIcon,
            binding.appBarMain.includedAppBarMainCustomToolbar.llToolbarMenuIcon3,
            binding.appBarMain.includedAppBarMainCustomToolbar.searchView,
            binding.appBarMain.includedAppBarMainCustomToolbar.saveSelectLanguageIcon
        )
        viewList.forEach { hideViewWithAnimation(it) }
    }

    // Function to close the SearchView and hide the keyboard
    private fun hideKeyboard() {
        // Hide SearchView
        val searchView = binding.appBarMain.includedAppBarMainCustomToolbar.searchView
        searchView.clearFocus() // Clear focus from SearchView
        searchView.isIconified = true // Collapse the SearchView

        // Hide keyboard
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocusView = currentFocus

        if (currentFocusView != null) {
            imm.hideSoftInputFromWindow(currentFocusView.windowToken, 0)
            imm.hideSoftInputFromWindow(searchView.windowToken, 0)//specific view to hide
        }
    }

    // region Call this function to request permissions as needed
    private fun checkAndRequestCalendarPermissions() {
        val permissions = mutableListOf<String>()
        // Check READ_CALENDAR permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_CALENDAR)
        }
        // Check WRITE_CALENDAR permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_CALENDAR)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), REQUEST_CODE_CALENDAR_PERMISSIONS)
        } else {
            // Permissions already granted
            initializeViewModelIfNeeded()
        }
    }

    private fun areCalendarPermissionsGranted(): Boolean {
        val readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR)
        val writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR)
        val postNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) } else { PackageManager.PERMISSION_GRANTED }
        return readPermission == PackageManager.PERMISSION_GRANTED && writePermission == PackageManager.PERMISSION_GRANTED && postNotificationPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun initializeViewModelIfNeeded() {
        if (areCalendarPermissionsGranted()) {
            mainViewModel.initializeViewModel() // Call your ViewModel initialization function
        }
    }

    // Handle the result of permission requests
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_CALENDAR_PERMISSIONS) {
            //val allPermissionsGranted = permissions.indices.all { grantResults[it] == PackageManager.PERMISSION_GRANTED }
            //if (allPermissionsGranted) {
            if (areCalendarPermissionsGranted()){
                initializeViewModelIfNeeded()
            } else {
                //Toast.makeText(this, "Calendar permissions are required for the app to function.", Toast.LENGTH_SHORT).show()
                // Show a Snackbar with a Settings action
                Snackbar.make(
                    findViewById(android.R.id.content),
                    getString(R.string.deny_permission_msg_calendar),
                    Snackbar.LENGTH_LONG
                ).setAction(getString(R.string.setting)) {
                    // Open app settings
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                    startActivity(intent)
                }.show()
            }
        }
        if (AlarmScheduler.handlePermissionResult(requestCode = requestCode, permissions = permissions, grantResults = grantResults)) {
            if (grantResults.isNotEmpty() && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "onRequestPermissionsResult: Permission granted, schedule the alarm")
            }else{
                // Permission denied, show a message to the user
                //Toast.makeText(this, "Permission required to show notifications", Toast.LENGTH_SHORT).show()
                // Show Snackbar for notification permission
                Snackbar.make(
                    findViewById(android.R.id.content),
                    getString(R.string.deny_permission_msg_notification),
                    Snackbar.LENGTH_LONG
                ).setAction(getString(R.string.setting)) {
                    // Open app settings
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                    startActivity(intent)
                }.show()
            }
        }
        else { super.onRequestPermissionsResult(requestCode, permissions, grantResults) }
        //if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) { when (requestCode) {}}

    }
    //endregion

    fun handelBackPressed(){
        // Use OnBackPressedDispatcher for API 12+ (and fallback for older versions)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START) // Close the drawer first
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Optional: Handle OnBackInvokedCallback for Android 14+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                OnBackInvokedCallback {
                    if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        binding.drawerLayout.closeDrawer(GravityCompat.START)
                    } else {
                        finish()
                    }
                }
            )
        }
    }
}