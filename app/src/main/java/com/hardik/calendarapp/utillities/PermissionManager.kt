package com.hardik.calendarapp.utillities

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

class PermissionManager1(private val context: Context) {

    private var requestPermissionLauncher: ActivityResultLauncher<String>? = null
    private var permissionCallback: ((Boolean) -> Unit)? = null

    /**
     * Checks if the given permission is granted.
     * @param permission The permission to check.
     * @return True if the permission is granted, false otherwise.
     */
    fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Requests the given permission from an Activity.
     * @param activity The Activity to request the permission from.
     * @param permission The permission to request.
     * @param callback A callback function to receive the permission result.
     */
    fun requestPermission(activity: FragmentActivity, permission: String, callback: (Boolean) -> Unit) {
        permissionCallback = callback

        if (checkPermission(permission)) {
            callback(true) // Permission already granted
            return
        }

        requestPermissionLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            permissionCallback?.invoke(isGranted)
        }

        requestPermissionLauncher?.launch(permission)
    }

    /**
     * Requests the given permission from a Fragment.
     * @param fragment The Fragment to request the permission from.
     * @param permission The permission to request.
     * @param callback A callback function to receive the permission result.
     */
    fun requestPermission(fragment: Fragment, permission: String, callback: (Boolean) -> Unit) {
        permissionCallback = callback

        if (checkPermission(permission)) {
            callback(true) // Permission already granted
            return
        }

        requestPermissionLauncher = fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            permissionCallback?.invoke(isGranted)
        }

        requestPermissionLauncher?.launch(permission)
    }



    // Example usage
    companion object {
        /**
         * Creates a PermissionManager instance.
         * @param context The context to use.
         * @return A new PermissionManager instance.
         */
        fun with(context: Context): PermissionManager1 {
            return PermissionManager1(context)
        }
    }
}

class PermissionManager(private val activity: FragmentActivity) {

    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            permissionCallback?.invoke(isGranted)
        }

    private var permissionCallback: ((Boolean) -> Unit)? = null

    /**
     * Checks if the given permission is granted.
     * @param permission The permission to check.
     * @return True if the permission is granted, false otherwise.
     */
    fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Requests the given permission from an Activity.
     * @param permission The permission to request.
     * @param callback A callback function to receive the permission result.
     */
    fun requestPermission(permission: String, callback: (Boolean) -> Unit) {
        permissionCallback = callback

        if (checkPermission(permission)) {
            callback(true) // Permission already granted
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }
}
