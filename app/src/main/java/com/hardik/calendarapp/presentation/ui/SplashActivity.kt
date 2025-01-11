package com.hardik.calendarapp.presentation.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.hardik.calendarapp.common.Constants
import com.hardik.calendarapp.databinding.ActivitySplashBinding
import com.hardik.calendarapp.presentation.MainViewModel
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the binding and set the content view
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root /*R.layout.activity_splash*/ )

        binding.apply {
            tvDay.apply { text = day }
            tvDate.apply { text = date.toString() }
        }

        mainViewModel.getHolidayCalendarData() //todo: 2 getting api data after getting locale calendar data
        Handler(Looper.getMainLooper()).postDelayed(
            {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }, 3000
        )

    }
}