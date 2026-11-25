package com.bugsmirror.riskchecker.internal.checks

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.bugsmirror.riskchecker.Finding
import com.bugsmirror.riskchecker.FindingId
import com.bugsmirror.riskchecker.Severity
import com.bugsmirror.riskchecker.internal.InstalledApps

/**
 * An app has an accessibility service turned on. Such a service can read the screen and tap
 * for the user, which is how a lot of banking malware works. System apps and the host
 * app itself are ignored.
 */
internal class AccessibilityAppsCheck(
    private val context: Context,
    private val apps: InstalledApps,
) : RiskCheck {

    override val id = FindingId.ACCESSIBILITY_APPS

    override fun run(): Finding? {
        val names = enabledServicePackages()
            .mapNotNull { apps.reportableName(it) }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
        if (names.isEmpty()) return null
        return Finding(id, Severity.HIGH, "Apps can read and control your screen", names.joinToString(", "))
    }

    /** Package names of every enabled accessibility service, from two sources. */
    private fun enabledServicePackages(): Set<String> {
        val packages = LinkedHashSet<String>()

        // 1. The accessibility service: services that are on and running.
        context.getSystemService(AccessibilityManager::class.java)
            ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            ?.mapNotNullTo(packages) { it.resolveInfo?.serviceInfo?.packageName }

        // 2. The setting behind the switches, "pkg/.Service:pkg2/.Service2".
        //    It also lists services that are switched on but not running right now.
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?.split(':')
            ?.mapNotNullTo(packages) { ComponentName.unflattenFromString(it.trim())?.packageName }

        return packages
    }
}

/**
 * Apps that request "Draw over other apps" (SYSTEM_ALERT_WINDOW). Such an app can cover
 * other apps, for example with a fake login screen. System apps and the host app itself
 * are ignored.
 */
internal class OverlayAppsCheck(private val apps: InstalledApps) : RiskCheck {

    override val id = FindingId.OVERLAY_APPS

    override fun run(): Finding? {
        val names = apps.packagesWithPermissions()
            .filter { pkg -> pkg.requestedPermissions?.contains(Manifest.permission.SYSTEM_ALERT_WINDOW) == true }
            .mapNotNull { pkg -> pkg.applicationInfo?.let { apps.reportableName(it) } }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
        if (names.isEmpty()) return null
        return Finding(id, Severity.MEDIUM, "Apps can draw over other apps", names.joinToString(", "))
    }
}
