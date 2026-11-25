package com.bugsmirror.riskchecker

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.annotation.WorkerThread
import com.bugsmirror.riskchecker.internal.InstalledApps
import com.bugsmirror.riskchecker.internal.LabelQuery
import com.bugsmirror.riskchecker.internal.PermissionLabels
import com.bugsmirror.riskchecker.internal.RiskScore
import com.bugsmirror.riskchecker.internal.checks.AccessibilityAppsCheck
import com.bugsmirror.riskchecker.internal.checks.CheckRunner
import com.bugsmirror.riskchecker.internal.checks.DeveloperOptionsCheck
import com.bugsmirror.riskchecker.internal.checks.EmulatorCheck
import com.bugsmirror.riskchecker.internal.checks.OverlayAppsCheck
import com.bugsmirror.riskchecker.internal.checks.RiskCheck
import com.bugsmirror.riskchecker.internal.checks.RootCheck
import com.bugsmirror.riskchecker.internal.checks.ScreenLockCheck
import com.bugsmirror.riskchecker.internal.checks.UsbDebuggingCheck
import com.bugsmirror.riskchecker.internal.isSystemApp
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * The library's entry point: checks the phone for security risks and finds apps by permission.
 *
 * Nothing here throws into the host app. A failure gives an empty result, `false`,
 * or a CHECK_FAILED finding instead. The library has no screens; all UI is in the host app.
 *
 * @param context any Context. Only its application context is kept, so passing an
 *   Activity does not leak it.
 */
class RiskChecker(context: Context) {

    private val appContext: Context = context.applicationContext ?: context
    private val installedApps = InstalledApps(appContext)

    /** Runs every check on a background thread, then calls [onResult] on the main thread. */
    fun scan(onResult: (RiskReport) -> Unit) {
        scanExecutor.execute {
            val report = runChecks()
            mainHandler.post { onResult(report) }
        }
    }

    /**
     * Apps that request any permission behind the given friendly names, such as "Camera"
     * or "SMS", or full names such as "android.permission.CAMERA". Case and extra spaces
     * are ignored. Names that are not recognised are returned in [PermissionResult.unknownLabels].
     *
     * Reads every visible installed app, which is slow: call it off the main thread.
     */
    @WorkerThread
    fun getAppsByPermissions(labels: List<String>): PermissionResult {
        val parsed = PermissionLabels.parse(labels)
        if (parsed.queries.isEmpty()) return PermissionResult(emptyList(), parsed.unknown)

        val apps = try {
            findApps(parsed.queries)
        } catch (t: Throwable) {
            Log.w(TAG, "Could not read the installed apps", t)
            emptyList()
        }
        return PermissionResult(apps, parsed.unknown)
    }

    /**
     * Opens the system "App info" screen for [packageName].
     * Returns false if the app is not installed or the screen could not be opened.
     */
    fun openAppSettings(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        return try {
            val installed = installedApps.applicationInfo(packageName) != null
            if (installed) {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null),
                )
                // Needed because we start it from the application context, not an Activity.
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                appContext.startActivity(intent)
            }
            installed
        } catch (e: Exception) {
            Log.w(TAG, "Could not open App info for $packageName", e)
            false
        }
    }

    /** Runs every check on the calling thread. Never throws. */
    @WorkerThread
    internal fun runChecks(): RiskReport {
        val findings = try {
            CheckRunner(createChecks()).runAll()
        } catch (t: Throwable) {
            // CheckRunner already catches every check, so this only guards against a bug in it.
            listOf(CheckRunner.checkFailed("SCAN", t))
        }
        return RiskReport(
            score = RiskScore.calculate(findings),
            scannedAt = System.currentTimeMillis(),
            findings = findings,
        )
    }

    /** The checks in the order their findings are reported. */
    private fun createChecks(): List<RiskCheck> = listOf(
        DeveloperOptionsCheck(appContext.contentResolver),
        UsbDebuggingCheck(appContext.contentResolver),
        ScreenLockCheck(appContext),
        AccessibilityAppsCheck(appContext, installedApps),
        OverlayAppsCheck(installedApps),
        RootCheck(),
        EmulatorCheck(),
    )

    private fun findApps(queries: List<LabelQuery>): List<AppInfo> =
        installedApps.packagesWithPermissions()
            .mapNotNull { pkg ->
                // One broken package must not hide all the others.
                try {
                    toAppInfo(pkg, queries)
                } catch (e: Exception) {
                    Log.w(TAG, "Skipping ${pkg.packageName}", e)
                    null
                }
            }
            // Apps the user installed first, then system apps; each group A to Z.
            .sortedWith(
                compareBy<AppInfo> { it.isSystemApp }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.appName },
            )

    /** The app as an [AppInfo] if it requests any of the [queries]' permissions, else null. */
    private fun toAppInfo(pkg: PackageInfo, queries: List<LabelQuery>): AppInfo? {
        val requested = pkg.requestedPermissions?.toList() ?: return null
        val matchedLabels = PermissionLabels.matchedLabels(queries, requested)
        if (matchedLabels.isEmpty()) return null
        val appInfo = pkg.applicationInfo ?: return null

        // requestedPermissionsFlags[i] tells whether requestedPermissions[i] is granted.
        val flags = pkg.requestedPermissionsFlags
        val matchedPermissions = requested.indices
            .filter { i -> queries.any { it.matches(requested[i]) } }
            .map { i ->
                val granted = flags != null && i < flags.size &&
                    (flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                MatchedPermission(requested[i], granted)
            }
            .distinctBy { it.name }

        return AppInfo(
            appName = installedApps.label(appInfo),
            packageName = pkg.packageName,
            icon = installedApps.icon(appInfo),
            matchedLabels = matchedLabels,
            isSystemApp = appInfo.isSystemApp,
            matchedPermissions = matchedPermissions,
        )
    }

    companion object {
        /** The friendly names [getAppsByPermissions] understands, for example to show as a hint. */
        @JvmField
        val SUPPORTED_LABELS: List<String> = PermissionLabels.FRIENDLY_NAMES.keys.toList()

        private const val TAG = "RiskChecker"

        /** One shared, low-priority background thread: scans run one after another. */
        private val scanExecutor: ExecutorService = Executors.newSingleThreadExecutor { task ->
            Thread(
                {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                    task.run()
                },
                "RiskChecker-scan",
            )
        }

        private val mainHandler = Handler(Looper.getMainLooper())
    }
}
