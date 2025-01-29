package com.hardik.calendarapp.utillities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.util.Arrays
import java.util.Locale


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

    private fun autoStart(
        context: Context,
        packages: List<String>,
        intents: List<Intent>,
        open: Boolean
    ): Boolean {
        return if (packages.stream().anyMatch { packageName: String ->
                isPackageExists(
                    context,
                    packageName
                )
            }) {
            if (open) openAutoStartScreen(context, intents) else areActivitiesFound(
                context,
                intents
            )
        } else false
    }

    private fun autoStartFromAction(
        context: Context,
        intents: List<Intent>,
        open: Boolean
    ): Boolean {
        return if (open) openAutoStartScreen(context, intents) else areActivitiesFound(
            context,
            intents
        )
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
}