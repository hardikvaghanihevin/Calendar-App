package calendar.schedule.task.todo.event.reminder.utillities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.preference.PreferenceManager
import android.provider.Settings
import android.view.ViewGroup
import android.view.Window
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import calendar.schedule.task.todo.event.reminder.R
import calendar.schedule.task.todo.event.reminder.common.Constants.BASE_TAG
import calendar.schedule.task.todo.event.reminder.databinding.DialogRequiredPermissionBinding

object PermissionHandler {
    private const val TAG = BASE_TAG + "PermissionHandler"

    private lateinit var multiplePermissionsLauncher: ActivityResultLauncher<Array<String>>

    private lateinit var sharedPreferences: SharedPreferences

    private var permissionCallback: ((Map<String, Boolean>, Boolean) -> Unit)? = null

    fun requestAllPermissions(activity: AppCompatActivity, callback: (Map<String, Boolean>, Boolean) -> Unit) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        permissionCallback = callback
        //setupPermissionLaunchers(activity)
        requestEssentialPermissions(activity)
    }

    private val permissionResults = mutableMapOf<String, Boolean>()

    fun setupPermissionLaunchers(activity: AppCompatActivity) {
        multiplePermissionsLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                permissions.forEach { (permission, granted) ->
                    permissionResults[permission] = granted
                    //Log.e(TAG, "setupPermissionLaunchers: $granted", )
                }
                //Log.e(TAG, "setupPermissionLaunchers: A $permissions", )
                // todo: complete this method result and then move forward to next work here 'good go'.
            }
    }

    private fun requestEssentialPermissions(activity: AppCompatActivity) {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !checkPermission(activity, Manifest.permission.POST_NOTIFICATIONS))
            //ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionsToRequest.isNotEmpty()) {
            multiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            // todo: if permissions are empty then move forward to next work here 'good to go'
        }
    }

    /**
     * Checks if the given permission is granted.
     * @param permission The permission to check.
     * @return True if the permission is granted, false otherwise.
     */
    fun checkPermission(activity: AppCompatActivity, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
    }
    fun checkPermission(activity: Activity, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun showAppSettingsDialog(activity: AppCompatActivity) {
        activity.run {
            // Inflate the custom layout
            val dialogView = layoutInflater.inflate(R.layout.dialog_required_permission, null)
            val dialogBinding = DialogRequiredPermissionBinding.bind(dialogView)

            val dialog = AlertDialog.Builder(this)
                .setView(dialogView) // Set custom view
                .setCancelable(false) // Prevent dismissing outside
                .create()

            // Set background to transparent if needed
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // Set background to transparent

            // Set width & height to wrap content when shown
            dialog.setOnShowListener {
                dialog.window?.setLayout(
                    (activity.resources.displayMetrics.widthPixels * 0.85).toInt(), // 85% of screen width // ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            // Show the dialog
            dialog.show()

            // Handle button clicks inside the custom dialog
            dialogBinding.btnGoToNext.setOnClickListener {
                activity.openAppSettings()
                dialog.dismiss()
            }
        }
    }

    //----------------------------------------------------------------todo:BatteryOptimization

    //----------------------------------------------------------------todo:AutoStart

    fun Activity.openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    //----------------------------------------------------------------todo:AutoStart
}
