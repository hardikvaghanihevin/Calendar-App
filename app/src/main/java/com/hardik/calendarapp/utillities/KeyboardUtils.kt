package com.hardik.calendarapp.utillities

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

object KeyboardUtils {

    /**
     * Hides the keyboard for the currently focused view or a specific view.
     *
     * @param activity The activity where the keyboard is currently shown.
     * @param view Optional specific view to hide the keyboard from.
     *
     * [KeyboardUtils.hideKeyboard(this)] // Hides the keyboard from the currently focused view
     *
     * // OR for a specific view:
     *
     * [KeyboardUtils.hideKeyboard(this, binding.appBarMain.includedAppBarMainCustomToolbar.searchView)]
     */
    fun hideKeyboard(activity: Activity, view: View? = activity.currentFocus) {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        view?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }
}