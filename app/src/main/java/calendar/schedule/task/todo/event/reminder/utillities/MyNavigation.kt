package calendar.schedule.task.todo.event.reminder.utillities

import androidx.navigation.NavOptions
import calendar.schedule.task.todo.event.reminder.R

object MyNavigation {
    val navOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_in_right)  // Custom enter animation (slide in from the right)
        .setExitAnim(R.anim.slide_out_left)   // Custom exit animation (slide out to the left)
        .setPopEnterAnim(R.anim.slide_in_left) // Animation when popping back (slide in from the left)
        .setPopExitAnim(R.anim.slide_out_right) // Animation when popping back (slide out to the right)
        .build()

    val navOptionsForYear = NavOptions.Builder() //todo: How many stack in the navigation but when you use this to navigate new fragment and then press back you can navigate directly this below fragment
        .setEnterAnim(R.anim.slide_in_right)  // Custom enter animation (slide in from the right)
        .setExitAnim(R.anim.slide_out_left)   // Custom exit animation (slide out to the left)
        .setPopEnterAnim(R.anim.slide_in_left) // Animation when popping back (slide in from the left)
        .setPopExitAnim(R.anim.slide_out_right) // Animation when popping back (slide out to the right)
        .setPopUpTo(R.id.nav_year, false) // Removes 2,3,4 but keeps frag1
        .build()
}