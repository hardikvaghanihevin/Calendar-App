package com.hardik.calendarapp.presentation.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.preference.PreferenceManager
import com.hardik.calendarapp.common.Constants
import com.hardik.calendarapp.databinding.ActivitySplashBinding
import com.hardik.calendarapp.presentation.MainViewModel
import com.hardik.calendarapp.presentation.ui.language.LanguageActivity
import com.hardik.calendarapp.utillities.DateUtil
import dagger.hilt.android.AndroidEntryPoint


@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    private final val TAG = Constants.BASE_TAG + SplashActivity::class.java.simpleName

    // Use activityViewModels() to share the ViewModel with MainActivity
    private val mainViewModel: MainViewModel by viewModels()//by activityViewModels()
    private lateinit var binding: ActivitySplashBinding
    val date = DateUtil.getCurrentDate()
    val day = DateUtil.getCurrentDay(isShort = false)
    //val day = DateUtil.getDayName(DateUtil.getCurrentDate(pattern = DateUtil.DATE_FORMAT_yyyy_MM_dd), isShort = false)

    companion object {
        var splashScrn: SplashScreen? = null
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT > 31) {
            splashScrn = installSplashScreen()
            splashScrn?.setKeepOnScreenCondition { false }
        }
        super.onCreate(savedInstanceState)
        Handler(Looper.getMainLooper()).postDelayed(
            {
//                splashScrn?.setKeepOnScreenCondition{ false}
                // Check if it's the first launch using SharedPreferences
                val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
                val isFirstLaunch = sharedPrefs.getBoolean("isFirstLaunch", true)

                val nextActivity = if (isFirstLaunch) {
                    LanguageActivity::class.java
                } else {
                    MainActivity::class.java
                }

                // If it's the first launch, update the SharedPreferences
                if (isFirstLaunch) {
                    sharedPrefs.edit().putBoolean("isFirstLaunch", false).apply()
                }

                // Start the appropriate activity based on the first launch check
                val intent = Intent(this, nextActivity)
                startActivity(intent)
                finish()
            }, 5000
        )

        // Inflate the binding and set the content view
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root /*R.layout.activity_splash*/ )

        binding.apply {
            tvDay.apply { text = day }
            tvDate.apply { text = date.toString() }
        }

        mainViewModel.getHolidayCalendarData() //todo: 2 getting api data after getting locale calendar data


    }
}