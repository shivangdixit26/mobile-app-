package com.bugsmirror.riskchecker

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class RiskReportTest {

    // Android's JSONObject (which the tests compile against) has keys(), not keySet().
    private fun JSONObject.keyNames(): Set<String> = keys().asSequence().toSet()

    @Test
    fun `toJson uses exactly the keys from the task`() {
        val report = RiskReport(
            score = 55,
            scannedAt = 1795420800000,
            findings = listOf(
                Finding("DEVELOPER_OPTIONS_ON", "MEDIUM", "Developer options are on", "Settings > System > Developer options"),
                Finding("OVERLAY_APPS", "MEDIUM", "Apps can draw over other apps", "Truecaller, Facebook"),
            ),
        )

        val json = JSONObject(report.toJson())

        assertEquals(setOf("score", "scannedAt", "findings"), json.keyNames())
        assertEquals(55, json.getInt("score"))
        assertEquals(1795420800000, json.getLong("scannedAt"))

        val findings = json.getJSONArray("findings")
        assertEquals(2, findings.length())
        val first = findings.getJSONObject(0)
        assertEquals(setOf("id", "severity", "title", "details"), first.keyNames())
        assertEquals("DEVELOPER_OPTIONS_ON", first.getString("id"))
        assertEquals("MEDIUM", first.getString("severity"))
        assertEquals("Developer options are on", first.getString("title"))
        assertEquals("Settings > System > Developer options", first.getString("details"))
        assertEquals("Truecaller, Facebook", findings.getJSONObject(1).getString("details"))
    }

    @Test
    fun `a report without findings has an empty array`() {
        val json = JSONObject(RiskReport(100, 0, emptyList()).toJson())
        assertEquals(0, json.getJSONArray("findings").length())
    }
}
