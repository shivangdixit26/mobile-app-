package com.bugsmirror.riskchecker

/**
 * One problem found by [RiskChecker.scan].
 *
 * @property id one of the [FindingId] values, for example "USB_DEBUGGING_ON".
 * @property severity one of the [Severity] values: "HIGH", "MEDIUM" or "LOW".
 * @property title short summary for people.
 * @property details where to fix it, or which apps are involved.
 */
data class Finding(
    val id: String,
    val severity: String,
    val title: String,
    val details: String,
)

/** The values used in [Finding.severity]. */
object Severity {
    const val HIGH = "HIGH"
    const val MEDIUM = "MEDIUM"
    const val LOW = "LOW"
}

/** The values used in [Finding.id]. */
object FindingId {
    const val DEVELOPER_OPTIONS_ON = "DEVELOPER_OPTIONS_ON"
    const val USB_DEBUGGING_ON = "USB_DEBUGGING_ON"
    const val NO_SCREEN_LOCK = "NO_SCREEN_LOCK"
    const val ACCESSIBILITY_APPS = "ACCESSIBILITY_APPS"
    const val OVERLAY_APPS = "OVERLAY_APPS"
    const val CHECK_FAILED = "CHECK_FAILED"

    // Bonus checks, both LOW severity.
    const val ROOTED_DEVICE = "ROOTED_DEVICE"
    const val EMULATOR_DETECTED = "EMULATOR_DETECTED"
}
