package com.hardik.calendarapp.presentation.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.preference.PreferenceManager
import com.hardik.calendarapp.common.Constants
import com.hardik.calendarapp.databinding.ActivitySplashBinding
import com.hardik.calendarapp.presentation.ui.language.LanguageActivity
import com.hardik.calendarapp.utillities.DateUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    private val TAG = Constants.BASE_TAG + SplashActivity::class.java.simpleName

    private lateinit var binding: ActivitySplashBinding
    val date = DateUtil.getCurrentDate()
    val day = DateUtil.getCurrentDay(isShort = false)

    companion object {
        var splashScrn: SplashScreen? = null
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT > 31) {
            splashScrn = installSplashScreen()
            splashScrn?.setKeepOnScreenCondition { false }
        }
        super.onCreate(savedInstanceState)

        // Inflate the binding and set the content view
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            tvDay.apply { text = day }
            tvDate.apply { text = date.toString() }
        }

        // Launch the next screen asynchronously
        CoroutineScope(Dispatchers.Main).launch {
            delay(100) // Short delay to mimic splash duration
            navigateToNextScreen()
        }
    }

    private suspend fun navigateToNextScreen() {
        withContext(Dispatchers.IO) {
            // Check if it's the first launch using SharedPreferences
            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this@SplashActivity)
            val isFirstLaunch = sharedPrefs.getBoolean("isFirstLaunch", true)

            val nextActivity = if (isFirstLaunch) {
                LanguageActivity::class.java
            } else {
                MainActivity::class.java
            }

            withContext(Dispatchers.Main) {
                val intent = Intent(this@SplashActivity, nextActivity)
                startActivity(intent)
                finish()
            }
        }
    }
}