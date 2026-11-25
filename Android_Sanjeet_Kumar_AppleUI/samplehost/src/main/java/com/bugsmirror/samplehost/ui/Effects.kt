package com.bugsmirror.samplehost.ui

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.bugsmirror.samplehost.ui.theme.LocalEffectSettings

/** How far a control shrinks while pressed. Small on purpose: feedback, not decoration. */
private const val PRESSED_SCALE = 0.97f

/**
 * [spec], or an instant jump when the user has turned animations off.
 * Every effect in the app goes through this, so "Remove animations" switches them all off.
 */
@Composable
fun <T> motionSpec(spec: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> =
    if (LocalEffectSettings.current.reduceMotion) snap() else spec

/**
 * Touch response of Apple's interactive controls: shrink a little while pressed, spring back
 * on release. Returns the scale to draw with; pair it with [scaleBy].
 */
@Composable
fun rememberPressScale(interactionSource: InteractionSource): State<Float> {
    val pressed by interactionSource.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (pressed) PRESSED_SCALE else 1f,
        animationSpec = motionSpec(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)),
        label = "pressScale",
    )
}

/** Draws the content at [scale]. Reads the value only while drawing, so a press never recomposes. */
fun Modifier.scaleBy(scale: State<Float>): Modifier = graphicsLayer {
    scaleX = scale.value
    scaleY = scale.value
}
