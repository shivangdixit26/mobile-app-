package com.bugsmirror.riskchecker.internal

import com.bugsmirror.riskchecker.Finding
import com.bugsmirror.riskchecker.Severity

/** The score formula: 100, minus 30 per HIGH, 15 per MEDIUM and 5 per LOW. Never below 0. */
internal object RiskScore {

    private const val MAX_SCORE = 100

    fun penaltyFor(severity: String): Int = when (severity) {
        Severity.HIGH -> 30
        Severity.MEDIUM -> 15
        Severity.LOW -> 5
        else -> 0
    }

    fun calculate(findings: List<Finding>): Int =
        (MAX_SCORE - findings.sumOf { penaltyFor(it.severity) }).coerceAtLeast(0)
}
