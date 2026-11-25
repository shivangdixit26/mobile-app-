package com.bugsmirror.riskchecker.internal

import android.Manifest
import java.util.Locale

/** One thing to search for: the label to report back, and the permissions behind it. */
internal data class LabelQuery(
    val label: String,
    /** Lower case, so that matching ignores case. */
    val permissions: Set<String>,
) {
    fun matches(permission: String): Boolean = permission.lowercase(Locale.ROOT) in permissions
}

/** What [PermissionLabels.parse] made of the caller's input. */
internal data class ParsedLabels(
    val queries: List<LabelQuery>,
    val unknown: List<String>,
)

/**
 * Turns friendly names such as "Camera" into Android permissions.
 *
 * Uses only Kotlin and compile-time constants (no Android calls), so it is unit tested
 * on the plain JVM.
 */
internal object PermissionLabels {

    private const val PERMISSION_PREFIX = "android.permission."
    private val WHITESPACE = Regex("\\s+")

    /** Friendly name -> the permissions behind it, as in the task's table. */
    val FRIENDLY_NAMES: Map<String, List<String>> = linkedMapOf(
        "Camera" to listOf(Manifest.permission.CAMERA),
        "Microphone" to listOf(Manifest.permission.RECORD_AUDIO),
        "Location" to listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ),
        "Contacts" to listOf(Manifest.permission.READ_CONTACTS),
        "SMS" to listOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
        ),
        "Phone" to listOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
        ),
        "Draw over other apps" to listOf(Manifest.permission.SYSTEM_ALERT_WINDOW),
    )

    /** Every permission in the table, to give full names typed in any case their real spelling. */
    private val tablePermissions: Set<String> = FRIENDLY_NAMES.values.flatten().toSet()

    /** normalize(friendly name) -> its query, for case-insensitive lookup. */
    private val friendlyQueries: Map<String, LabelQuery> =
        FRIENDLY_NAMES.entries.associate { (name, permissions) ->
            normalize(name) to LabelQuery(name, permissions.mapTo(LinkedHashSet()) { it.lowercase(Locale.ROOT) })
        }

    /** Trims, turns runs of spaces into one space, and lower-cases: " Draw  over APPS " -> "draw over apps". */
    fun normalize(text: String): String = text.trim().replace(WHITESPACE, " ").lowercase(Locale.ROOT)

    /**
     * Splits [labels] into queries and names we do not know. Keeps the caller's order,
     * skips blank entries, and searches a repeated name only once.
     * Takes nullable items so that a Java caller passing a null in the list cannot crash us.
     */
    fun parse(labels: List<String?>): ParsedLabels {
        val queries = LinkedHashMap<String, LabelQuery>() // normalized text -> query
        val unknown = LinkedHashMap<String, String>()     // normalized text -> text as typed
        for (raw in labels) {
            if (raw == null) continue
            val key = normalize(raw)
            if (key.isEmpty()) continue
            val query = friendlyQueries[key] ?: fullPermissionQuery(raw.trim(), key)
            if (query != null) {
                queries.getOrPut(key) { query }
            } else {
                unknown.getOrPut(key) { raw.trim().replace(WHITESPACE, " ") }
            }
        }
        return ParsedLabels(queries.values.toList(), unknown.values.toList())
    }

    /** The labels of the [queries] that [requestedPermissions] match, in the order of [queries]. */
    fun matchedLabels(queries: List<LabelQuery>, requestedPermissions: List<String>): List<String> {
        val requested = requestedPermissions.mapTo(HashSet()) { it.lowercase(Locale.ROOT) }
        return queries
            .filter { query -> query.permissions.any { it in requested } }
            .map { it.label }
    }

    /**
     * "android.permission.X" is accepted for any X. The label is the name as typed,
     * except that a permission from the table gets its real spelling ("...CAMERA").
     */
    private fun fullPermissionQuery(typed: String, key: String): LabelQuery? {
        if (!key.startsWith(PERMISSION_PREFIX)) return null
        val name = key.removePrefix(PERMISSION_PREFIX)
        if (name.isEmpty() || name.contains(' ')) return null
        val label = tablePermissions.firstOrNull { it.equals(typed, ignoreCase = true) } ?: typed
        return LabelQuery(label, setOf(key))
    }
}
