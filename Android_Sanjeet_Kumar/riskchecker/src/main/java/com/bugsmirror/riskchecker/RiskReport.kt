package com.bugsmirror.riskchecker

import org.json.JSONArray
import org.json.JSONObject

/**
 * Result of [RiskChecker.scan].
 *
 * @property score 0 to 100. Starts at 100 and loses 30 for each HIGH, 15 for each MEDIUM
 *   and 5 for each LOW finding, but never goes below 0.
 * @property scannedAt when the scan finished, in milliseconds since 1 January 1970 (UTC).
 * @property findings the problems found. Empty when nothing is wrong.
 */
data class RiskReport(
    val score: Int,
    val scannedAt: Long,
    val findings: List<Finding>,
) {

    /**
     * The report as JSON, with exactly these keys:
     * `{"score": .., "scannedAt": .., "findings": [{"id", "severity", "title", "details"}, ..]}`
     */
    fun toJson(): String {
        val findingsJson = JSONArray()
        for (finding in findings) {
            findingsJson.put(
                JSONObject()
                    .put("id", finding.id)
                    .put("severity", finding.severity)
                    .put("title", finding.title)
                    .put("details", finding.details),
            )
        }
        return JSONObject()
            .put("score", score)
            .put("scannedAt", scannedAt)
            .put("findings", findingsJson)
            .toString()
    }
}
