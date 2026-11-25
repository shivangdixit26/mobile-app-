package com.bugsmirror.riskchecker.internal.checks

import com.bugsmirror.riskchecker.FindingId
import com.bugsmirror.riskchecker.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DeviceChecksTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val pixel = BuildInfo(
        fingerprint = "google/husky/husky:15/AP4A.250105.002/12701944:user/release-keys",
        model = "Pixel 8 Pro",
        manufacturer = "Google",
        hardware = "husky",
        product = "husky",
    )

    private val emulator = BuildInfo(
        fingerprint = "google/sdk_gphone64_x86_64/emu64xa:15/AE3A.240806.005/12228598:userdebug/dev-keys",
        model = "sdk_gphone64_x86_64",
        manufacturer = "Google",
        hardware = "ranchu",
        product = "sdk_gphone64_x86_64",
    )

    @Test
    fun `a real phone is not reported as an emulator`() {
        assertNull(EmulatorCheck(pixel).run())
    }

    @Test
    fun `the Android emulator is reported as LOW`() {
        val finding = EmulatorCheck(emulator).run()!!
        assertEquals(FindingId.EMULATOR_DETECTED, finding.id)
        assertEquals(Severity.LOW, finding.severity)
        assertEquals("Build.HARDWARE is ranchu", finding.details)
    }

    @Test
    fun `Genymotion is reported`() {
        assertEquals(
            "Build.MANUFACTURER is Genymotion",
            EmulatorCheck.emulatorReason(pixel.copy(manufacturer = "Genymotion")),
        )
    }

    @Test
    fun `no su file means not rooted`() {
        assertNull(RootCheck(listOf(tempFolder.root.path + "/missing/su")).run())
    }

    @Test
    fun `a su file is reported as LOW with its path`() {
        val su = tempFolder.newFile("su")
        val finding = RootCheck(listOf("/no/such/su", su.path)).run()!!
        assertEquals(FindingId.ROOTED_DEVICE, finding.id)
        assertEquals(Severity.LOW, finding.severity)
        assertEquals("su file found at ${su.path}", finding.details)
    }
}
