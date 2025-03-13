package com.hardik.calendarapp.utillities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.PowerManager
import android.preference.PreferenceManager
import android.provider.Settings
import android.view.ViewGroup
import android.view.Window
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.databinding.DialogAutoStartPermissionBinding
import com.hardik.calendarapp.databinding.DialogBatteryOptimizationBinding
import com.hardik.calendarapp.databinding.DialogRequiredPermissionBinding
import java.util.Arrays
import java.util.Locale

object PermissionHandler {
    private const val TAG = BASE_TAG + "PermissionHandler"

    private lateinit var multiplePermissionsLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var batteryOptimizationLauncher: ActivityResultLauncher<Intent>
    private lateinit var settingsLauncher: ActivityResultLauncher<Intent>

    private lateinit var sharedPreferences: SharedPreferences
    private var isAutostartSet: Boolean = false

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
                checkBatteryOptimization(activity, permissionResults)
            }

        batteryOptimizationLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                Handler(activity.mainLooper).postDelayed({
                    val isBatteryOptimized = activity.isBatteryOptimizationPermissionGranted()
                    permissionResults["BatteryOptimization"] = isBatteryOptimized
                    //Log.e(TAG, "setupPermissionLaunchers: $isBatteryOptimized", )
                    checkAutoStart(activity, permissionResults)
                },1000)
            }

        settingsLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                //sharedPreferences.edit().putBoolean("PREF_KEY_AUTO_START_PERMISSION", true).apply()
                val autoStartPermissionHelper = AutoStartPermissionHelper.getInstance()
                val isAutoStartPermissionAvailable = autoStartPermissionHelper.isAutoStartPermissionAvailable(activity, false)
                permissionResults["AutoStart"] = isAutoStartPermissionAvailable && sharedPreferences.getBoolean("PREF_KEY_AUTO_START_PERMISSION", false)
            
                permissionCallback?.invoke(permissionResults, true)
            }
    }

    private fun requestEssentialPermissions(activity: AppCompatActivity) {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            //ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            !checkPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
        ) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionsToRequest.isNotEmpty()) {
            multiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            checkBatteryOptimization(activity, mutableMapOf())
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

    @SuppressLint("BatteryLife")
    private fun checkBatteryOptimization(activity: AppCompatActivity, permissionResults: MutableMap<String, Boolean>) {
        if (!activity.isBatteryOptimizationPermissionGranted()) {
            val isXiaomiDevice = AutoStartPermissionHelper.getInstance().isXiaomiDevice()
            //Log.e(TAG, "checkBatteryOptimization: $isXiaomiDevice", )
            if (isXiaomiDevice) {
                showAlertDialogBatteryOptimizationPermission(activity, permissionResults)
            }else{
                val intent = activity.requestBatteryOptimizationPermission()
                batteryOptimizationLauncher.launch(intent)
            }
        } else {
            checkAutoStart(activity, permissionResults)
        }
    }

    private fun showAlertDialogBatteryOptimizationPermission(activity: AppCompatActivity, permissionResults: MutableMap<String, Boolean>) {
        activity.run {
            // Inflate the custom layout
            val dialogView = layoutInflater.inflate(R.layout.dialog_battery_optimization, null)
            val dialogBinding = DialogBatteryOptimizationBinding.bind(dialogView)

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
                val intent = activity.requestBatteryOptimizationPermission()
                batteryOptimizationLauncher.launch(intent)
                dialog.dismiss()
            }

            dialogBinding.btnCancel.setOnClickListener {
                // Set BatteryOptimization to false directly
                permissionResults["BatteryOptimization"] = false
                checkAutoStart(activity, permissionResults)
                dialog.dismiss()
            }
        }
    }

    private fun checkAutoStart(activity: AppCompatActivity, permissionResults: MutableMap<String, Boolean>) {
        val autoStartPermissionHelper = AutoStartPermissionHelper.getInstance()
        val isAutoStartPermissionAvailable = autoStartPermissionHelper.isAutoStartPermissionAvailable(activity, false)
        isAutostartSet = sharedPreferences.getBoolean("PREF_KEY_AUTO_START_PERMISSION", false)
        if (isAutoStartPermissionAvailable && !isAutostartSet) {
            showAlertDialogAutoStartPermission(activity, permissionResults)
        } else {
            permissionCallback?.invoke(permissionResults, true)
        }
    }

    private fun showAlertDialogAutoStartPermission(activity: AppCompatActivity, permissionResults: MutableMap<String, Boolean>) {
        activity.run {
            // Inflate custom layout
            val dialogView = layoutInflater.inflate(R.layout.dialog_auto_start_permission, null)
            val binding = DialogAutoStartPermissionBinding.bind(dialogView)

            // Create Material AlertDialog
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false) // Prevent dismissing outside
                .create()

            // Set background to transparent if needed
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            // Make background transparent
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            // Adjust size on show
            dialog.setOnShowListener {
                dialog.window?.setLayout(
                    (activity.resources.displayMetrics.widthPixels * 0.85).toInt(), // 85% of screen width // ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            // Show the dialog
            dialog.show()

            // Handle button clicks
            binding.btnGoToNext.setOnClickListener {
                sharedPreferences.edit().putBoolean("PREF_KEY_AUTO_START_PERMISSION", true).apply()
                val autoStartPermissionHelper = AutoStartPermissionHelper.getInstance()
                val intent = autoStartPermissionHelper.getAutoStartIntent(activity)
                settingsLauncher.launch(intent)
                dialog.dismiss()
            }

            binding.btnCancel.setOnClickListener {
                permissionResults["AutoStart"] = false
                permissionCallback?.invoke(permissionResults, false)
                dialog.dismiss()
            }
        }
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
    @SuppressLint("BatteryLife")
    private fun Context.requestBatteryOptimizationPermission(): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply { data = Uri.parse("package:$packageName") }
    }

    private fun Context.isBatteryOptimizationPermissionGranted(): Boolean {
        val pkg = packageName
        val pm = getSystemService(PowerManager::class.java)
        return pm.isIgnoringBatteryOptimizations(pkg)
    }

    //----------------------------------------------------------------todo:AutoStart

    fun Activity.openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    //----------------------------------------------------------------todo:AutoStart

    /**
     * AutoStartPermissionHelper - Handles auto-start permissions for various Android devices.
     * @author koshurboii (telegram/Instagram/github : @koshurboii)
     */
    class AutoStartPermissionHelper private constructor() {
        fun getAutoStartPermission(context: Context, open: Boolean, newTask: Boolean): Boolean {
            return when (Build.BRAND.lowercase(Locale.getDefault())) {
                BRAND_ASUS -> autoStartAsus(context, open, newTask)
                BRAND_XIAOMI, BRAND_XIAOMI_POCO, BRAND_XIAOMI_REDMI -> autoStartXiaomi(context, open, newTask)
                BRAND_LETV -> autoStartLetv(context, open, newTask)
                BRAND_HONOR -> autoStartHonor(context, open, newTask)
                BRAND_HUAWEI -> autoStartHuawei(context, open, newTask)
                BRAND_OPPO -> autoStartOppo(context, open, newTask)
                BRAND_VIVO -> autoStartVivo(context, open, newTask)
                BRAND_NOKIA -> autoStartNokia(context, open, newTask)
                BRAND_SAMSUNG -> autoStartSamsung(context, open, newTask)
                BRAND_ONE_PLUS -> autoStartOnePlus(context, open, newTask)
                else -> false
            }
        }

        fun isAutoStartPermissionAvailable(context: Context, onlyIfSupported: Boolean): Boolean {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(0)
            for (packageInfo in packages) {
                if (PACKAGES_TO_CHECK_FOR_PERMISSION.contains(packageInfo.packageName)
                    && (!onlyIfSupported || getAutoStartPermission(context, false, false))
                ) {
                    return true
                }
            }
            return false
        }

        private fun autoStartXiaomi(context: Context, open: Boolean, newTask: Boolean): Boolean {
            return autoStart(
                context, Arrays.asList(PACKAGE_XIAOMI_MAIN),
                Arrays.asList(getIntent(PACKAGE_XIAOMI_MAIN, PACKAGE_XIAOMI_COMPONENT, newTask)), open
            )
            // Similar updates for other autoStart() calls...
        }

        private fun autoStartAsus(context: Context, open: Boolean, newTask: Boolean): Boolean {
            return autoStart(
                context, Arrays.asList(PACKAGE_ASUS_MAIN),
                Arrays.asList(
                    getIntent(PACKAGE_ASUS_MAIN, PACKAGE_ASUS_COMPONENT, newTask),
                    getIntent(PACKAGE_ASUS_MAIN, PACKAGE_ASUS_COMPONENT_FALLBACK, newTask)
                ), open
            )
        }

        private fun autoStartLetv(context: Context, open: Boolean, newTask: Boolean): Boolean {
            return autoStart(
                context, Arrays.asList(PACKAGE_LETV_MAIN),
                Arrays.asList(getIntent(PACKAGE_LETV_MAIN, PACKAGE_LETV_COMPONENT, newTask)), open
            )
        }

        private fun autoStartHonor(context: Context, open: Boolean, newTask: Boolean): Boolean {
            return autoStart(
                context, Arrays.asList(PACKAGE_HONOR_MAIN),
                Arrays.asList(getIntent(PACKAGE_HONOR_MAIN, PACKAGE_HONOR_COMPONENT, newTask)), open
            )
        }

        private fun autoStartHuawei(context: Context, open: Boolean, newTask: Boolean): Boolean {
            return autoStart(
                context, Arrays.asList(PACKAGE_HUAWEI_MAIN),
                Arrays.asList(
                    getIntent(PACKAGE_HUAWEI_MAIN, PACKAGE_HUAWEI_COMPONENT, newTask),
                    getIntent(PACKAGE_HUAWEI_MAIN, PACKAGE_HUAWEI_COMPONENT_FALLBACK, newTask)
                ), open
            )
        }

        private fun autoStartOppo(context: Context, open: Boolean, newTask: Boolean): Boolean {
            return autoStart(
                context, Arrays.asList(PACKAGE_OPPO_MAIN, PACKAGE_OPPO_FALLBACK),
                Arrays.asList(
                    getIntent(PACKAGE_OPPO_MAIN, PACKAGE_OPPO_COMPONENT, newTask),
                    getIntent(PACKAGE_OPPO_FALLBACK, PACKAGE_OPPO_COMPONENT_FALLBACK, newTask),
                    getIntent(PACKAGE_OPPO_MAIN, PACKAGE_OPPO_COMPONENT_FALLBACK_A, newTask)
                ), open
            )
        }

        private fun launchOppoAppInfo(context: Context, open: Boolean, newTask: Boolean): Boolean {
            return try {
                val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                i.addCategory(Intent.CATEGORY_DEFAULT)
                i.data = Uri.parse("package:" + context.packageName)
                if (open) {
                    context.startActivity(i)
                    true
                } else {
                    isActivityFound(context, i)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                false
            }
        }

        private fun autoStartVivo(context: Context, open: Boolean, newTask: Boolean): Boolean {
            return autoStart(
                context, Arrays.asList(PACKAGE_VIVO_MAIN, PACKAGE_VIVO_FALLBACK),
                Arrays.asList(
                    getIntent(PACKAGE_VIVO_MAIN, PACKAGE_VIVO_COMPONENT, newTask),
                    getIntent(PACKAGE_VIVO_FALLBACK, PACKAGE_VIVO_COMPONENT_FALLBACK, newTask),
                    getIntent(PACKAGE_VIVO_MAIN, PACKAGE_VIVO_COMPONENT_FALLBACK_A, newTask)
                ), open
            )
        }

        private fun autoStartNokia(context: Context, open: Boolean, newTask: Boolean): Boolean {
            return autoStart(
                context, Arrays.asList(PACKAGE_NOKIA_MAIN),
                Arrays.asList(getIntent(PACKAGE_NOKIA_MAIN, PACKAGE_NOKIA_COMPONENT, newTask)), open
            )
        }

        private fun autoStartSamsung(context: Context, open: Boolean, newTask: Boolean): Boolean {
            return autoStart(
                context, Arrays.asList(PACKAGE_SAMSUNG_MAIN),
                Arrays.asList(
                    getIntent(PACKAGE_SAMSUNG_MAIN, PACKAGE_SAMSUNG_COMPONENT, newTask),
                    getIntent(PACKAGE_SAMSUNG_MAIN, PACKAGE_SAMSUNG_COMPONENT_2, newTask),
                    getIntent(PACKAGE_SAMSUNG_MAIN, PACKAGE_SAMSUNG_COMPONENT_3, newTask)
                ), open
            )
        }

        private fun autoStartOnePlus(context: Context, open: Boolean, newTask: Boolean): Boolean {
            return (autoStart(
                context,
                Arrays.asList(PACKAGE_ONE_PLUS_MAIN),
                Arrays.asList(getIntent(PACKAGE_ONE_PLUS_MAIN, PACKAGE_ONE_PLUS_COMPONENT, newTask)),
                open
            )
                    || autoStartFromAction(
                context, Arrays.asList(
                    getIntentFromAction(
                        PACKAGE_ONE_PLUS_ACTION, newTask
                    )
                ), open
            ))
        }

        @Throws(Exception::class)
        private fun startIntent(context: Context, intent: Intent) {
            try {
                context.startActivity(intent)
            } catch (exception: Exception) {
                exception.printStackTrace()
                throw exception
            }
        }

        private fun isPackageExists(context: Context, targetPackage: String): Boolean {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(0)
            for (packageInfo in packages) {
                if (packageInfo.packageName == targetPackage) {
                    return true
                }
            }
            return false
        }

        private fun getIntent(packageName: String, componentName: String, newTask: Boolean): Intent {
            val intent = Intent()
            intent.component = ComponentName(packageName, componentName)
            if (newTask) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return intent
        }

        private fun getIntentFromAction(intentAction: String, newTask: Boolean): Intent {
            val intent = Intent()
            intent.action = intentAction
            if (newTask) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return intent
        }

        private fun isActivityFound(context: Context, intent: Intent): Boolean {
            return context.packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            ).size > 0
        }

        private fun areActivitiesFound(context: Context, intents: List<Intent>): Boolean {
            for (intent in intents) {
                if (isActivityFound(context, intent)) {
                    return true
                }
            }
            return false
        }

        private fun openAutoStartScreen(context: Context, intents: List<Intent>): Boolean {
            for (intent in intents) {
                if (isActivityFound(context, intent)) {
                    context.startActivity(intent)
                    return true
                }
            }
            return false
        }

        private fun autoStart(context: Context, packages: List<String>, intents: List<Intent>, open: Boolean): Boolean {
            return if (packages.stream().anyMatch { packageName: String -> isPackageExists(context, packageName) })
            { if (open) openAutoStartScreen(context, intents) else areActivitiesFound(context, intents)
            } else false
        }

        private fun autoStartFromAction(context: Context, intents: List<Intent>, open: Boolean): Boolean {
            return if (open) openAutoStartScreen(context, intents) else areActivitiesFound(context, intents)
        }

        companion object {
            private const val BRAND_XIAOMI = "xiaomi"
            private const val BRAND_XIAOMI_POCO = "poco"
            private const val BRAND_XIAOMI_REDMI = "redmi"
            private const val PACKAGE_XIAOMI_MAIN = "com.miui.securitycenter"
            private const val PACKAGE_XIAOMI_COMPONENT = "com.miui.permcenter.autostart.AutoStartManagementActivity"
            private const val BRAND_LETV = "letv"
            private const val PACKAGE_LETV_MAIN = "com.letv.android.letvsafe"
            private const val PACKAGE_LETV_COMPONENT = "com.letv.android.letvsafe.AutobootManageActivity"
            private const val BRAND_ASUS = "asus"
            private const val PACKAGE_ASUS_MAIN = "com.asus.mobilemanager"
            private const val PACKAGE_ASUS_COMPONENT = "com.asus.mobilemanager.powersaver.PowerSaverSettings"
            private const val PACKAGE_ASUS_COMPONENT_FALLBACK = "com.asus.mobilemanager.autostart.AutoStartActivity"
            private const val BRAND_HONOR = "honor"
            private const val PACKAGE_HONOR_MAIN = "com.huawei.systemmanager"
            private const val PACKAGE_HONOR_COMPONENT = "com.huawei.systemmanager.optimize.process.ProtectActivity"
            private const val BRAND_HUAWEI = "huawei"
            private const val PACKAGE_HUAWEI_MAIN = "com.huawei.systemmanager"
            private const val PACKAGE_HUAWEI_COMPONENT = "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            private const val PACKAGE_HUAWEI_COMPONENT_FALLBACK = "com.huawei.systemmanager.optimize.process.ProtectActivity"
            private const val BRAND_OPPO = "oppo"
            private const val PACKAGE_OPPO_MAIN = "com.coloros.safecenter"
            private const val PACKAGE_OPPO_FALLBACK = "com.oppo.safe"
            private const val PACKAGE_OPPO_COMPONENT = "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            private const val PACKAGE_OPPO_COMPONENT_FALLBACK = "com.oppo.safe.permission.startup.StartupAppListActivity"
            private const val PACKAGE_OPPO_COMPONENT_FALLBACK_A = "com.coloros.safecenter.startupapp.StartupAppListActivity"
            private const val BRAND_VIVO = "vivo"
            private const val PACKAGE_VIVO_MAIN = "com.iqoo.secure"
            private const val PACKAGE_VIVO_FALLBACK = "com.vivo.permissionmanager"
            private const val PACKAGE_VIVO_COMPONENT = "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
            private const val PACKAGE_VIVO_COMPONENT_FALLBACK = "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            private const val PACKAGE_VIVO_COMPONENT_FALLBACK_A = "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"
            private const val BRAND_NOKIA = "nokia"
            private const val PACKAGE_NOKIA_MAIN = "com.evenwell.powersaving.g3"
            private const val PACKAGE_NOKIA_COMPONENT = "com.evenwell.powersaving.g3.exception.PowerSaverExceptionActivity"
            private const val BRAND_SAMSUNG = "samsung"
            private const val PACKAGE_SAMSUNG_MAIN = "com.samsung.android.lool"
            private const val PACKAGE_SAMSUNG_COMPONENT = "com.samsung.android.sm.ui.battery.BatteryActivity"
            private const val PACKAGE_SAMSUNG_COMPONENT_2 = "com.samsung.android.sm.battery.ui.usage.CheckableAppListActivity"
            private const val PACKAGE_SAMSUNG_COMPONENT_3 = "com.samsung.android.sm.battery.ui.BatteryActivity"
            private const val BRAND_ONE_PLUS = "oneplus"
            private const val PACKAGE_ONE_PLUS_MAIN = "com.oneplus.security"
            private const val PACKAGE_ONE_PLUS_COMPONENT = "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
            private const val PACKAGE_ONE_PLUS_ACTION = "com.android.settings.action.BACKGROUND_OPTIMIZE"
            private val PACKAGES_TO_CHECK_FOR_PERMISSION: List<String> = ArrayList(
                Arrays.asList(
                    PACKAGE_ASUS_MAIN,
                    PACKAGE_XIAOMI_MAIN,
                    PACKAGE_LETV_MAIN,
                    PACKAGE_HONOR_MAIN,
                    PACKAGE_OPPO_MAIN,
                    PACKAGE_OPPO_FALLBACK,
                    PACKAGE_VIVO_MAIN,
                    PACKAGE_VIVO_FALLBACK,
                    PACKAGE_NOKIA_MAIN,
                    PACKAGE_HUAWEI_MAIN,
                    PACKAGE_SAMSUNG_MAIN,
                    PACKAGE_ONE_PLUS_MAIN
                )
            )
            private var instance: AutoStartPermissionHelper? = null

            fun getInstance(): AutoStartPermissionHelper {
                if (instance == null) {
                    instance = AutoStartPermissionHelper()
                }
                return instance!!
            }
        }

        fun getAutoStartIntent(context: Context): Intent {
            val manufacturer = android.os.Build.MANUFACTURER.lowercase(Locale.ROOT) // android.os.Build.MANUFACTURER.lowercase()
            val intent = when (manufacturer) {
                "xiaomi" -> Intent().setComponent(
                    ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                )
                "asus" -> Intent().setComponent(
                    ComponentName(
                        "com.asus.mobilemanager",
                        "com.asus.mobilemanager.entry.FunctionActivity"
                        //"com.asus.mobilemanager.powersaver.PowerSaverSettings"//if above its not working so use it instead
                        //"com.asus.mobilemanager.autostart.AutoStartActivity"//if above its not working so use it instead
                    )
                )
                "letv" -> Intent().setComponent(
                    ComponentName(
                        "com.letv.android.letvsafe",
                        "com.letv.android.letvsafe.AutobootManageActivity"
                    )
                )
                "honor" -> Intent().setComponent(
                    ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                    )
                )
                "huawei" -> Intent().setComponent(
                    ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                    )
                )
                "oppo" -> Intent().setComponent(
                    ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                    )
                )
                "vivo" -> Intent().setComponent(
                    ComponentName(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                    )
                )
                "nokia" -> Intent().setComponent(
                    ComponentName(
                        "com.evenwell.powersaving.g3",
                        "com.evenwell.powersaving.g3.exception.PowerSaverExceptionActivity"
                    )
                )
                "samsung" -> Intent().setComponent(
                    ComponentName(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.ui.battery.BatteryActivity"
                    )
                )
                "oneplus" -> Intent().setComponent(
                    ComponentName(
                        "com.oneplus.security",
                        "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                    )
                )
                else -> null
            }

            // Check if the intent is resolvable
            return intent?.takeIf { it.resolveActivity(context.packageManager) != null }
                ?: Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:${context.packageName}"))
        }

        private fun getDeviceBrand(): String { return android.os.Build.BRAND.lowercase(Locale.ROOT) }

        private fun getManufacturer(): String{ return Build.MANUFACTURER.lowercase(Locale.ROOT) }
        fun isXiaomiDevice(): Boolean {
            val brand = getDeviceBrand()
            return brand == "xiaomi" || brand == "poco" || brand == "redmi"
        }

        fun isXiaomiManufacturer(): Boolean {
            val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
            return manufacturer == "xiaomi" || manufacturer == "poco" || manufacturer == "redmi"
        }

    }
}

/*
    //todo : Manifest permission check
    <uses-feature android:name="android.hardware.telephony" android:required="false" />
    <uses-permission android:name="android.permission.SEND_SMS"/>
    <uses-permission android:name="android.permission.READ_CONTACTS"/>

    <!-- Phone State (for detecting calls) -->
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />

    <!-- Post Notifications (for Android 13+) -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <!-- Battery Optimization (requires special intent, not a direct permission) -->
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"/>

    <!-- Auto Start (handled via manufacturer-specific settings, not a direct permission) -->


    //todo : in side your activity (onCreate())
    // Register the launchers here, in onCreate
    PermissionHandler.setupPermissionLaunchers(this)

    findViewById<Button>(R.id.btnRequestPermissions).setOnClickListener {
        PermissionHandler.requestAllPermissions(this) { permissionResults, allPermissionsGranted ->
            if (allPermissionsGranted) {
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                PermissionHandler.showAppSettingsDialog(this)
                Toast.makeText(this, "Permissions denied or partially granted", Toast.LENGTH_SHORT).show()
            }

            if (permissionResults.isEmpty()) {
                Log.e(TAG, "PermissionHandler Permission results map is empty.")
            } else {
                permissionResults.forEach { (permission, granted) ->
                    Log.e(TAG,"Permission: $permission, Granted: $granted")
                    if (permission == Manifest.permission.READ_PHONE_STATE && granted) {
                        Toast.makeText(this, "Phone state permission granted", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
*/