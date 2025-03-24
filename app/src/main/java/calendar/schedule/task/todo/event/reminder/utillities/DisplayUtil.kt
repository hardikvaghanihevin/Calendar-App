package calendar.schedule.task.todo.event.reminder.utillities

import android.content.Context
import android.content.res.Resources
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.core.view.doOnDetach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DisplayUtil {
    //Utility for Converting dp to Pixels
    fun Int.dpToPx(): Int {
        return (this * Resources.getSystem().displayMetrics.density).toInt()
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
    fun hideViewWithAnimation(fab: View, duration: Long = 0) {
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