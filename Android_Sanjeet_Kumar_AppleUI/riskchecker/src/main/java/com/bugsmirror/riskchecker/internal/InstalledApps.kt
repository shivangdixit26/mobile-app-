package com.bugsmirror.riskchecker.internal

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build

/**
 * A small wrapper around PackageManager.
 *
 * On Android 11 and newer, PackageManager only returns the apps this app may see: the
 * ones matched by the `<queries>` in this library's AndroidManifest.xml (apps with a
 * launcher icon, and accessibility services), plus a few that are always visible.
 */
internal class InstalledApps(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    /** Every visible installed app, with its requested permissions. May throw. */
    fun packagesWithPermissions(): List<PackageInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        }

    /** The app's ApplicationInfo, or null if it is not installed or we may not see it. */
    fun applicationInfo(packageName: String): ApplicationInfo? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(packageName, 0)
        }
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    /** The name shown under the app's icon, or its package name if it has none. */
    fun label(info: ApplicationInfo): String =
        packageManager.getApplicationLabel(info).toString().ifBlank { info.packageName }

    /** The app's icon, or null if it cannot be loaded. */
    fun icon(info: ApplicationInfo): Drawable? = try {
        packageManager.getApplicationIcon(info)
    } catch (e: RuntimeException) {
        null
    }

    /**
     * The name to put in a finding's details, or null if the app should be ignored
     * because it is a system app or the host app itself.
     */
    fun reportableName(info: ApplicationInfo): String? =
        if (info.packageName == context.packageName || info.isSystemApp) null else label(info)

    /**
     * Same as above, by package name. An app we cannot look up is still reported, by its
     * package name: for a security check, missing a risk is worse than a false alarm.
     */
    fun reportableName(packageName: String): String? {
        if (packageName == context.packageName) return null
        val info = applicationInfo(packageName) ?: return packageName
        return reportableName(info)
    }
}

/** True for apps that came with the phone, including ones updated since from the store. */
internal val ApplicationInfo.isSystemApp: Boolean
    get() = (flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
