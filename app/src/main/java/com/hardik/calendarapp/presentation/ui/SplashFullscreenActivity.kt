package com.hardik.calendarapp.presentation.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants
import com.hardik.calendarapp.databinding.ActivitySplashFullscreenBinding
import com.hardik.calendarapp.presentation.MainViewModel
import com.hardik.calendarapp.utillities.DateUtil
import com.hardik.calendarapp.utillities.DisplayUtil
import dagger.hilt.android.AndroidEntryPoint

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar)
 */

@AndroidEntryPoint
class SplashFullscreenActivity : AppCompatActivity() {
    private final val TAG = Constants.BASE_TAG + SplashFullscreenActivity::class.java.simpleName

    // Use activityViewModels() to share the ViewModel with MainActivity
    val mainViewModel: MainViewModel by viewModels()//by activityViewModels()
    private lateinit var binding: ActivitySplashFullscreenBinding
    val date = DateUtil.getCurrentDate()
    val day = DateUtil.getDayName(DateUtil.getCurrentDate(pattern = DateUtil.DATE_FORMAT_yyyy_MM_dd), isShort = false)

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the binding and set the content view
        binding = ActivitySplashFullscreenBinding.inflate(layoutInflater)


        enableEdgeToEdge()
        // Fullscreen with immersive flags
//        DisplayUtil.enableFullScreen(binding.root, supportActionBar)
        // Check if the device has API level 30 or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30 or higher (Android 11)
            window.decorView.windowInsetsController?.let { controller ->
                controller.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                //controller.systemGestureInsets = android.graphics.Rect(0, 0, 0, 0)
            }

            // Set custom status bar color for API 30+
            window.statusBarColor = ContextCompat.getColor(this, R.color.status_bar_color) // Replace with your color resource
        } else {
            // For older versions (below API 30)
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

            // Set custom status bar color for API < 30
            window.statusBarColor = ContextCompat.getColor(this, R.color.status_bar_color) // Replace with your color resource
        }
//        getWindow().setFlags(
//            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
//            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
//        )
//        //requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().setFlags(
//            WindowManager.LayoutParams.FLAG_FULLSCREEN,
//            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContentView(binding.root)

        binding.apply {
            tvDay.apply { text = day }
            tvDate.apply { text = date.toString() }
        }

        mainViewModel.getHolidayCalendarData() //todo: 2 getting api data after getting locale calendar data
        Handler(Looper.getMainLooper()).postDelayed(
            {
                DisplayUtil.disableFullScreen(binding.root, supportActionBar)

                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)

                finish()
            }, 3000
        )

    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // Initial hide after the activity is created
    }


}
