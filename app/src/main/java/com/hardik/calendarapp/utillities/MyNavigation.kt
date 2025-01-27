package com.hardik.calendarapp.utillities

import androidx.navigation.NavOptions
import com.hardik.calendarapp.R

object MyNavigation {
    val navOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_in_right)  // Custom enter animation (slide in from the right)
        .setExitAnim(R.anim.slide_out_left)   // Custom exit animation (slide out to the left)
        .setPopEnterAnim(R.anim.slide_in_left) // Animation when popping back (slide in from the left)
        .setPopExitAnim(R.anim.slide_out_right) // Animation when popping back (slide out to the right)
        .build()
}