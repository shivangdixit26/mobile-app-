package com.bugsmirror.samplehost.ui.theme

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

    /**
     * The score colour for text and the ring drawn straight on a card. On dark cards the base
     * colours reach only about 2.8:1, so they are mixed with 35% white there (about 4.5:1 to 5.3:1).
     * Tags keep the base colours because they carry white text.
     */
    @Composable
    fun forScoreOnSurface(score: Int): Color {
        val base = forScore(score)
        val darkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        return if (darkSurface) lerp(base, Color.White, 0.35f) else base
    }
}

/**
 * Grouped look, as in Apple's Settings app: a soft page colour with brighter cards on it.
 * Built from Material colour roles, so it follows the wallpaper colours and dark mode.
 */
object Surfaces {
    val page: Color
        @Composable get() = if (isDark()) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer

    val card: Color
        @Composable get() = if (isDark()) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLowest

    @Composable
    private fun isDark(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f
}

/**
 * The system settings the visual effects respect (Apple design skill, liquid-glass.md, checklist 5):
 * motion must be optional, and transparency must give way when people ask for more contrast.
 */
data class EffectSettings(
    /** "Remove animations" is on: effects jump to their end state instead of animating. */
    val reduceMotion: Boolean,
    /** The system contrast is raised (Android 14+): translucent surfaces become solid. */
    val increasedContrast: Boolean,
)

val LocalEffectSettings = staticCompositionLocalOf { EffectSettings(reduceMotion = false, increasedContrast = false) }

private fun readEffectSettings(context: Context): EffectSettings {
    // Accessibility > "Remove animations" (and the developer animation scale) set this to 0.
    val animatorScale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    // Android 14 added a system contrast level: 0 is standard, above 0 is higher contrast.
    val contrast = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        context.getSystemService(UiModeManager::class.java)?.contrast ?: 0f
    } else {
        0f
    }
    return EffectSettings(reduceMotion = animatorScale == 0f, increasedContrast = contrast > 0f)
}

/** Softer, larger corners on cards and fields; buttons keep Material's full capsule shape. */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

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
    // Read once per Activity; a settings change shows the next time the screen is created.
    val effects = remember(context) { readEffectSettings(context) }
    CompositionLocalProvider(LocalEffectSettings provides effects) {
        MaterialTheme(colorScheme = colorScheme, shapes = AppShapes, content = content)
    }
}
