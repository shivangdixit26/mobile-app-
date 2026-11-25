package com.bugsmirror.riskchecker

import android.content.Context
import android.util.Log
import androidx.annotation.MainThread

/**
 * Bonus: the library starts one scan by itself when the host app starts (see
 * `internal/AutoScanProvider`), so the host app gets a report without calling any code.
 *
 * A host that wants to show it can read [latestReport] or call [observe].
 * To turn the start-up scan off, a host removes the provider in its own manifest:
 * `<provider android:name="com.bugsmirror.riskchecker.internal.AutoScanProvider" tools:node="remove" />`
 */
object RiskCheckerAutoScan {

    private const val TAG = "RiskChecker"

    /** The start-up scan's report, or null until that scan has finished. */
    @Volatile
    var latestReport: RiskReport? = null
        private set

    /** Listeners waiting for the report. Only touched on the main thread. */
    private val waiting = mutableListOf<(RiskReport) -> Unit>()

    /**
     * Calls [listener] once, on the main thread, with the start-up report:
     * right away if it is ready, otherwise when the scan finishes.
     */
    @MainThread
    fun observe(listener: (RiskReport) -> Unit) {
        val report = latestReport
        if (report != null) listener(report) else waiting += listener
    }

    /** Forgets a listener given to [observe], for example when its screen goes away. */
    @MainThread
    fun removeObserver(listener: (RiskReport) -> Unit) {
        waiting -= listener
    }

    @MainThread
    internal fun start(context: Context) {
        RiskChecker(context).scan { report ->
            latestReport = report
            Log.i(TAG, "Start-up scan done: score=${report.score}, findings=${report.findings.size}")
            val listeners = waiting.toList()
            waiting.clear()
            listeners.forEach { it(report) }
        }
    }
}
