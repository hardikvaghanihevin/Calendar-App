package com.hardik.calendarapp.utillities

import android.content.res.Resources

object DisplayUtil {
    //Utility for Converting dp to Pixels
    fun Int.dpToPx(): Int {
        return (this * Resources.getSystem().displayMetrics.density).toInt()
    }
}