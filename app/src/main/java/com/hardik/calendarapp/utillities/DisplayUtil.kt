package com.hardik.calendarapp.utillities

import android.content.res.Resources
import android.os.Build
import android.view.View
import android.view.WindowInsets
import androidx.appcompat.app.ActionBar

object DisplayUtil {
    //Utility for Converting dp to Pixels
    fun Int.dpToPx(): Int {
        return (this * Resources.getSystem().displayMetrics.density).toInt()
    }

    private var isFullscreen: Boolean = true
    //private fun toggle() { if (isFullscreen) { disableFullScreen() } else { enableFullScreen() } }// Toggle between full-screen and normal view
    fun enableFullScreen(view: View, supportActionBar: ActionBar?) {
        // Hide UI elements and enable full-screen mode
        supportActionBar?.hide()
        isFullscreen = true

        if (Build.VERSION.SDK_INT >= 30) {
            view.windowInsetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            // Hide status bar and navigation bar

        } else {
            view.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            view.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        }
    }

    fun disableFullScreen(view: View, supportActionBar: ActionBar?) {
        // Show UI elements and disable full-screen mode
        supportActionBar?.show()
        isFullscreen = false

        if (Build.VERSION.SDK_INT >= 30) {
            view.windowInsetsController?.run {
                //show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                //systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            view.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }
    }
}