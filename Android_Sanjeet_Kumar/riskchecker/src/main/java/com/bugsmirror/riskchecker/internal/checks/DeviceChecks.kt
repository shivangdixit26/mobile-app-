package com.bugsmirror.riskchecker.internal.checks

import android.os.Build
import com.bugsmirror.riskchecker.Finding
import com.bugsmirror.riskchecker.FindingId
import com.bugsmirror.riskchecker.Severity
import java.io.File

/** Bonus: a `su` file on the phone usually means it is rooted. */
internal class RootCheck(private val suPaths: List<String> = SU_PATHS) : RiskCheck {

    override val id = FindingId.ROOTED_DEVICE

    override fun run(): Finding? {
        val found = suPaths.firstOrNull { File(it).exists() } ?: return null
        return Finding(id, Severity.LOW, "The phone may be rooted", "su file found at $found")
    }

    companion object {
        /** Places where rooting tools usually put the su binary. */
        val SU_PATHS = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/su",
            "/su/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/data/local/bin/su",
            "/data/local/xbin/su",
            "/vendor/bin/su",
        )
    }
}

/** The android.os.Build values the emulator check looks at. A class so tests can fake them. */
internal data class BuildInfo(
    val fingerprint: String,
    val model: String,
    val manufacturer: String,
    val hardware: String,
    val product: String,
) {
    companion object {
        fun current() = BuildInfo(
            fingerprint = Build.FINGERPRINT.orEmpty(),
            model = Build.MODEL.orEmpty(),
            manufacturer = Build.MANUFACTURER.orEmpty(),
            hardware = Build.HARDWARE.orEmpty(),
            product = Build.PRODUCT.orEmpty(),
        )
    }
}

/** Bonus: the app runs on the Android emulator or Genymotion, not a real phone. */
internal class EmulatorCheck(private val build: BuildInfo = BuildInfo.current()) : RiskCheck {

    override val id = FindingId.EMULATOR_DETECTED

    override fun run(): Finding? {
        val reason = emulatorReason(build) ?: return null
        return Finding(id, Severity.LOW, "Running on an emulator", reason)
    }

    companion object {
        /** Which Build value gave the emulator away, or null if it looks like a real phone. */
        fun emulatorReason(build: BuildInfo): String? = with(build) {
            when {
                hardware.contains("goldfish", ignoreCase = true) ||
                    hardware.contains("ranchu", ignoreCase = true) -> "Build.HARDWARE is $hardware"
                fingerprint.startsWith("generic", ignoreCase = true) ||
                    fingerprint.contains("emulator", ignoreCase = true) ||
                    fingerprint.contains("sdk_gphone", ignoreCase = true) -> "Build.FINGERPRINT is $fingerprint"
                model.contains("Emulator", ignoreCase = true) ||
                    model.contains("Android SDK built for", ignoreCase = true) ||
                    model.contains("sdk_gphone", ignoreCase = true) -> "Build.MODEL is $model"
                manufacturer.contains("Genymotion", ignoreCase = true) -> "Build.MANUFACTURER is $manufacturer"
                product.contains("sdk_gphone", ignoreCase = true) ||
                    product.contains("emulator", ignoreCase = true) ||
                    product.contains("simulator", ignoreCase = true) -> "Build.PRODUCT is $product"
                else -> null
            }
        }
    }
}
