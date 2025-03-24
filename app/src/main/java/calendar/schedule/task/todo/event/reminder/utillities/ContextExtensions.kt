package calendar.schedule.task.todo.event.reminder.utillities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

fun Context.requestBatteryOptimizationPermission(){
    startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${packageName}")))
}
fun Context.isBatteryOptimizationPermissionGranted(): Boolean{
    val pkg = packageName;
    val pm = getSystemService(PowerManager::class.java)
    return pm.isIgnoringBatteryOptimizations(pkg)
}