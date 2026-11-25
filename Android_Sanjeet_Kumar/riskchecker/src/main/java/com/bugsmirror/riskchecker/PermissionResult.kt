package com.bugsmirror.riskchecker

import android.graphics.drawable.Drawable

/**
 * Result of [RiskChecker.getAppsByPermissions].
 *
 * @property apps installed apps that request at least one of the permissions searched for.
 * @property unknownLabels the names that were not recognised, as the caller typed them
 *   (without the extra spaces), for example "Camra".
 */
data class PermissionResult(
    val apps: List<AppInfo>,
    val unknownLabels: List<String>,
)

/**
 * An installed app that matched a permission search.
 *
 * @property matchedLabels the friendly names it matched, such as "Camera". When the caller
 *   searched with a full permission name, that full name, such as "android.permission.CAMERA".
 * @property isSystemApp true for apps that came with the phone (also after an update).
 * @property matchedPermissions bonus: each matched Android permission and whether the user has
 *   granted it or the app only requests it. Has a default value, so the five properties from
 *   the task's API can still be used on their own.
 */
data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable?,
    val matchedLabels: List<String>,
    val isSystemApp: Boolean,
    val matchedPermissions: List<MatchedPermission> = emptyList(),
)

/** A permission that an app requests, and whether it has been granted. */
data class MatchedPermission(
    val name: String,
    val isGranted: Boolean,
)
