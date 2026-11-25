package com.bugsmirror.riskchecker.internal.checks

import com.bugsmirror.riskchecker.Finding
import com.bugsmirror.riskchecker.FindingId
import com.bugsmirror.riskchecker.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckRunnerTest {

    private class FakeCheck(override val id: String, private val result: () -> Finding?) : RiskCheck {
        override fun run(): Finding? = result()
    }

    private val problem = Finding(FindingId.USB_DEBUGGING_ON, Severity.MEDIUM, "USB debugging is on", "details")

    @Test
    fun `checks without a problem add no finding`() {
        val findings = CheckRunner(listOf(FakeCheck(FindingId.DEVELOPER_OPTIONS_ON) { null })).runAll()
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `a throwing check becomes one CHECK_FAILED and the other checks still run`() {
        val findings = CheckRunner(
            listOf(
                FakeCheck(FindingId.NO_SCREEN_LOCK) { throw SecurityException("not allowed") },
                FakeCheck(FindingId.USB_DEBUGGING_ON) { problem },
                FakeCheck(FindingId.OVERLAY_APPS) { throw IllegalStateException() },
            ),
        ).runAll()

        assertEquals(3, findings.size)

        assertEquals(FindingId.CHECK_FAILED, findings[0].id)
        assertEquals(Severity.LOW, findings[0].severity)
        assertEquals("NO_SCREEN_LOCK check failed (SecurityException: not allowed)", findings[0].details)

        assertEquals(problem, findings[1])

        assertEquals(FindingId.CHECK_FAILED, findings[2].id)
        assertEquals("OVERLAY_APPS check failed (IllegalStateException)", findings[2].details)
    }

    @Test
    fun `an Error is caught too`() {
        val findings = CheckRunner(listOf(FakeCheck(FindingId.ACCESSIBILITY_APPS) { throw NoSuchMethodError("x") })).runAll()
        assertEquals(FindingId.CHECK_FAILED, findings.single().id)
    }
}
