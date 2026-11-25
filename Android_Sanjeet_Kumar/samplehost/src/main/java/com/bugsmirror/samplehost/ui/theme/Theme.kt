package com.bugsmirror.samplehost.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.bugsmirror.riskchecker.Severity

/** Fixed colours for risk levels. Each one is readable with white text on top (contrast 5:1 or more). */
object RiskColors {
    val High = Color(0xFFC62828)
    val Medium = Color(0xFFB45309)
    val Low = Color(0xFF1565C0)
    val Good = Color(0xFF2E7D32)

    fun forSeverity(severity: String): Color = when (severity) {
        Severity.HIGH -> High
        Severity.MEDIUM -> Medium
        else -> Low
    }

    fun forScore(score: Int): Color = when {
        score >= 80 -> Good
        score >= 50 -> Medium
        else -> High
    }
}

/** Material 3 theme: the wallpaper's colours on Android 12+, the default colours before that. */
@Composable
fun SampleHostTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
