package com.bugsmirror.riskchecker.internal.checks

import android.util.Log
import com.bugsmirror.riskchecker.Finding
import com.bugsmirror.riskchecker.FindingId
import com.bugsmirror.riskchecker.Severity

/** One security check run by RiskChecker.scan(). */
internal interface RiskCheck {

    /** The finding id this check reports. Also names the check in a CHECK_FAILED finding. */
    val id: String

    /** A finding if the problem is present, otherwise null. May throw; [CheckRunner] handles that. */
    fun run(): Finding?
}

/** Runs checks one by one, so that one failing check cannot stop the others. */
internal class CheckRunner(private val checks: List<RiskCheck>) {

    fun runAll(): List<Finding> = checks.mapNotNull { check ->
        try {
            check.run()
        } catch (t: Throwable) {
            // Throwable, not just Exception: an Error such as NoSuchMethodError on an unusual
            // phone must not crash the host app either. It becomes a LOW finding instead.
            Log.w(TAG, "Check ${check.id} failed", t)
            checkFailed(check.id, t)
        }
    }

    companion object {
        private const val TAG = "RiskChecker"

        /** The CHECK_FAILED finding for a check that threw [error]. */
        fun checkFailed(checkId: String, error: Throwable): Finding {
            val reason = error.message?.let { "${error.javaClass.simpleName}: $it" }
                ?: error.javaClass.simpleName
            return Finding(
                id = FindingId.CHECK_FAILED,
                severity = Severity.LOW,
                title = "A check could not run",
                details = "$checkId check failed ($reason)",
            )
        }
    }
}
