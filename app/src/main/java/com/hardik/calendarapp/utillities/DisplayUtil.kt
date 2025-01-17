package com.hardik.calendarapp.utillities

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.ActionBar
import androidx.core.view.doOnDetach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.suspendCoroutine

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

    /**
    isKeyboardVisible(this) { isVisible ->
    if (isVisible) {}}
    */
    fun isKeyboardVisible(context: Context, onKeyboardVisibilityChanged: (isVisible: Boolean) -> Unit) {
        val rootView = (context as? android.app.Activity)?.findViewById<View>(android.R.id.content)
            ?: return // Return if rootView is not found

        val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
            private var wasKeyboardVisible = false

            override fun onGlobalLayout() {
                val heightDiff = rootView.rootView.height - rootView.height
                val isKeyboardVisible = heightDiff > rootView.height * 0.25 // Adjust threshold if needed

                if (isKeyboardVisible != wasKeyboardVisible) {
                    if (isKeyboardVisible) {
                        enableAdjustResize(context) // Enable adjustResize when keyboard is visible
                    } else {
                        resetAdjustNothing(context) // Reset to adjustNothing when keyboard is hidden
                    }
                    onKeyboardVisibilityChanged(isKeyboardVisible)
                    wasKeyboardVisible = isKeyboardVisible
                }
            }
        }

        rootView.viewTreeObserver.addOnGlobalLayoutListener(listener)

        // Ensure the listener is removed when the root view is detached
        rootView.doOnDetach {
            rootView.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }


    /**
    val rootView = findViewById<View>(android.R.id.content)

    isKeyboardVisible(rootView) { isVisible ->
    if (isVisible) {}}
    */
    fun isKeyboardVisible(rootView: View, onKeyboardVisibilityChanged: (isVisible: Boolean) -> Unit) {
        val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
            private var wasKeyboardVisible = false

            override fun onGlobalLayout() {
                val heightDiff = rootView.rootView.height - rootView.height
                val isKeyboardVisible = heightDiff > rootView.height * 0.25 // Adjust threshold if needed

                if (isKeyboardVisible != wasKeyboardVisible) {
                    onKeyboardVisibilityChanged(isKeyboardVisible)
                    wasKeyboardVisible = isKeyboardVisible
                }
            }
        }

        rootView.viewTreeObserver.addOnGlobalLayoutListener(listener)

        // Remove the listener when necessary
        rootView.doOnDetach {
            rootView.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }

    suspend fun suspendKeyboardCheck(rootView: View, onKeyboardVisibilityChecked: suspend (isVisible: Boolean) -> Unit) {
        return suspendCoroutine { continuation ->
            val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
                private var wasKeyboardVisible = false

                override fun onGlobalLayout() {
                    val heightDiff = rootView.rootView.height - rootView.height
                    val isKeyboardVisible = heightDiff > rootView.height * 0.25 // Adjust threshold if needed

                    if (isKeyboardVisible != wasKeyboardVisible) {
                        wasKeyboardVisible = isKeyboardVisible
                        continuation.resumeWith(Result.success(Unit))
                    }
                }
            }

            rootView.viewTreeObserver.addOnGlobalLayoutListener(listener)

            // Cleanup the listener when the view is detached
            rootView.doOnDetach {
                rootView.viewTreeObserver.removeOnGlobalLayoutListener(listener)
            }
        }
    }

    /**
    Display Screen adjust
     */
    private fun enableAdjustResize(context: Context) {
        (context as? android.app.Activity)?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }
    /**
    Display Screen adjust
     */
    private fun resetAdjustNothing(context: Context) {
        (context as? android.app.Activity)?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
    }

    // Function to show VIEW with animation
    fun showViewWithAnimation(fab: View, duration: Long = 300) {
        CoroutineScope(Dispatchers.Main).launch {
            fab.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(duration) // animation duration in milliseconds
                .withStartAction { fab.visibility = View.VISIBLE }
                .start()
        }
    }

    // Function to hide VIEW with animation
    fun hideViewWithAnimation(fab: View, duration: Long = 300) {
        CoroutineScope(Dispatchers.Main).launch {
            fab.animate()
                .alpha(0f)
                .scaleX(0f)
                .scaleY(0f)
                .setDuration(duration) // animation duration in milliseconds
                .withEndAction { fab.visibility = View.GONE }
                .start()
        }
    }
}