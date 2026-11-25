package com.bugsmirror.riskchecker.internal.checks

import android.app.KeyguardManager
import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import com.bugsmirror.riskchecker.Finding
import com.bugsmirror.riskchecker.FindingId
import com.bugsmirror.riskchecker.Severity

/** The "Developer options" switch is on. */
internal class DeveloperOptionsCheck(private val resolver: ContentResolver) : RiskCheck {

    override val id = FindingId.DEVELOPER_OPTIONS_ON

    override fun run(): Finding? {
        val enabled = Settings.Global.getInt(resolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0
        if (!enabled) return null
        return Finding(id, Severity.MEDIUM, "Developer options are on", "Settings > System > Developer options")
    }
}

/** USB debugging (adb) is on: a computer plugged in by USB can control the phone. */
internal class UsbDebuggingCheck(private val resolver: ContentResolver) : RiskCheck {

    override val id = FindingId.USB_DEBUGGING_ON

    override fun run(): Finding? {
        val enabled = Settings.Global.getInt(resolver, Settings.Global.ADB_ENABLED, 0) != 0
        if (!enabled) return null
        return Finding(id, Severity.MEDIUM, "USB debugging is on", "Settings > Developer options > USB debugging")
    }
}

/** The phone has no PIN, pattern or password. */
internal class ScreenLockCheck(private val context: Context) : RiskCheck {

    override val id = FindingId.NO_SCREEN_LOCK

    override fun run(): Finding? {
        val keyguard = context.getSystemService(KeyguardManager::class.java)
            ?: error("KeyguardManager is not available")
        // isDeviceSecure: true when a PIN, pattern or password is set.
        if (keyguard.isDeviceSecure) return null
        return Finding(
            id,
            Severity.MEDIUM,
            "No screen lock is set",
            "Anyone who picks up the phone can use it. Set a PIN, pattern or password in Settings > Security",
        )
    }
}
