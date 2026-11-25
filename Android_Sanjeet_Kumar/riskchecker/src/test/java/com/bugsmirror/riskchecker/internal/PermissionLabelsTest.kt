package com.bugsmirror.riskchecker.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionLabelsTest {

    private fun labelsOf(vararg input: String?) = PermissionLabels.parse(input.toList()).queries.map { it.label }

    private fun permissionsOf(name: String) = PermissionLabels.parse(listOf(name)).queries.single().permissions

    @Test
    fun `every friendly name in the table maps to its permissions`() {
        assertEquals(setOf("android.permission.camera"), permissionsOf("Camera"))
        assertEquals(setOf("android.permission.record_audio"), permissionsOf("Microphone"))
        assertEquals(
            setOf("android.permission.access_fine_location", "android.permission.access_coarse_location"),
            permissionsOf("Location"),
        )
        assertEquals(setOf("android.permission.read_contacts"), permissionsOf("Contacts"))
        assertEquals(
            setOf("android.permission.read_sms", "android.permission.receive_sms", "android.permission.send_sms"),
            permissionsOf("SMS"),
        )
        assertEquals(
            setOf("android.permission.read_phone_state", "android.permission.call_phone"),
            permissionsOf("Phone"),
        )
        assertEquals(setOf("android.permission.system_alert_window"), permissionsOf("Draw over other apps"))
    }

    @Test
    fun `case and extra spaces are ignored and the label is the table's name`() {
        assertEquals(listOf("Camera"), labelsOf(" camera "))
        assertEquals(listOf("SMS"), labelsOf("sms"))
        assertEquals(listOf("Draw over other apps"), labelsOf("  dRAW   over OTHER apps "))
    }

    @Test
    fun `the task's example splits into known and unknown names`() {
        val parsed = PermissionLabels.parse(listOf("Camera", " sms", " Camra"))
        assertEquals(listOf("Camera", "SMS"), parsed.queries.map { it.label })
        assertEquals(listOf("Camra"), parsed.unknown)
    }

    @Test
    fun `a full permission name is accepted for any permission`() {
        val parsed = PermissionLabels.parse(listOf("android.permission.BLUETOOTH_CONNECT"))
        assertEquals(listOf("android.permission.BLUETOOTH_CONNECT"), parsed.queries.map { it.label })
        assertEquals(setOf("android.permission.bluetooth_connect"), parsed.queries.single().permissions)
        assertTrue(parsed.unknown.isEmpty())
    }

    @Test
    fun `a full name from the table typed in any case gets its real spelling`() {
        assertEquals(listOf("android.permission.CAMERA"), labelsOf("  Android.Permission.camera "))
    }

    @Test
    fun `unknown names are returned as typed and never throw`() {
        val parsed = PermissionLabels.parse(listOf("Camra", "android.permission.", "android.permission.A B", "  Bad   name "))
        assertTrue(parsed.queries.isEmpty())
        assertEquals(listOf("Camra", "android.permission.", "android.permission.A B", "Bad name"), parsed.unknown)
    }

    @Test
    fun `blank and null entries are skipped`() {
        val parsed = PermissionLabels.parse(listOf("", "   ", null))
        assertTrue(parsed.queries.isEmpty())
        assertTrue(parsed.unknown.isEmpty())
    }

    @Test
    fun `repeated names are searched once`() {
        val parsed = PermissionLabels.parse(listOf("Camera", "CAMERA", " camera", "Camra", "camra"))
        assertEquals(listOf("Camera"), parsed.queries.map { it.label })
        assertEquals(listOf("Camra"), parsed.unknown)
    }

    @Test
    fun `an app matches when it requests at least one permission behind a name`() {
        val queries = PermissionLabels.parse(listOf("Camera", "SMS", "Microphone")).queries
        val requested = listOf("android.permission.INTERNET", "android.permission.SEND_SMS", "android.permission.CAMERA")
        assertEquals(listOf("Camera", "SMS"), PermissionLabels.matchedLabels(queries, requested))
    }

    @Test
    fun `an app that requests none of the permissions does not match`() {
        val queries = PermissionLabels.parse(listOf("Location")).queries
        assertTrue(PermissionLabels.matchedLabels(queries, listOf("android.permission.INTERNET")).isEmpty())
        assertTrue(PermissionLabels.matchedLabels(queries, emptyList()).isEmpty())
    }

    @Test
    fun `matching a full permission name ignores case`() {
        val queries = PermissionLabels.parse(listOf("android.permission.camera")).queries
        assertEquals(
            listOf("android.permission.CAMERA"),
            PermissionLabels.matchedLabels(queries, listOf("android.permission.CAMERA")),
        )
    }
}
