package com.bugsmirror.riskchecker.internal

import com.bugsmirror.riskchecker.Finding
import com.bugsmirror.riskchecker.Severity
import org.junit.Assert.assertEquals
import org.junit.Test

class RiskScoreTest {

    private fun findings(vararg severities: String) = severities.map { Finding("ID", it, "title", "details") }

    @Test
    fun `no findings scores 100`() {
        assertEquals(100, RiskScore.calculate(emptyList()))
    }

    @Test
    fun `each severity costs its own penalty`() {
        assertEquals(70, RiskScore.calculate(findings(Severity.HIGH)))
        assertEquals(85, RiskScore.calculate(findings(Severity.MEDIUM)))
        assertEquals(95, RiskScore.calculate(findings(Severity.LOW)))
    }

    @Test
    fun `the task's example with three MEDIUM findings scores 55`() {
        assertEquals(55, RiskScore.calculate(findings(Severity.MEDIUM, Severity.MEDIUM, Severity.MEDIUM)))
    }

    @Test
    fun `penalties add up`() {
        assertEquals(50, RiskScore.calculate(findings(Severity.HIGH, Severity.MEDIUM, Severity.LOW)))
    }

    @Test
    fun `score never goes below 0`() {
        assertEquals(0, RiskScore.calculate(findings(Severity.HIGH, Severity.HIGH, Severity.HIGH, Severity.HIGH)))
        assertEquals(0, RiskScore.calculate(findings(*Array(10) { Severity.HIGH })))
    }

    @Test
    fun `an unknown severity costs nothing`() {
        assertEquals(100, RiskScore.calculate(findings("CRITICAL")))
    }
}
