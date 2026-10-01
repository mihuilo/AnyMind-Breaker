package com.anymindbreaker.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

// Animations are short on purpose: they confirm an action and must never slow down solving.

/** Duration of the fade between setup, play and result. */
const val PHASE_FADE_MILLIS = 180

private const val SHAKE_STEP_MILLIS = 40
private val SHAKE_OFFSETS_DP = listOf(-5f, 5f, -3f, 3f, 0f)

private const val POP_SCALE = 1.25f
private const val POP_MILLIS = 160

/**
 * Horizontal offset in dp for a short shake. The shake runs when [mistakes] grows while
 * [enabled] is true, so only the element that caused the mistake moves.
 */
@Composable
fun rememberShakeOffset(mistakes: Int, enabled: Boolean): State<Float> {
    val offset = remember { Animatable(0f) }
    val seen = remember { mutableIntStateOf(mistakes) }
    LaunchedEffect(mistakes) {
        val grew = mistakes > seen.intValue
        seen.intValue = mistakes
        if (grew && enabled) {
            for (target in SHAKE_OFFSETS_DP) offset.animateTo(target, tween(SHAKE_STEP_MILLIS))
        }
    }
    return offset.asState()
}

/** Scale for a soft "pop" that runs when [value] changes to something non-null. */
@Composable
fun <T> rememberPopScale(value: T?): State<Float> {
    val scale = remember { Animatable(1f) }
    val seen = remember { mutableStateOf(value) }
    LaunchedEffect(value) {
        val changed = value != seen.value
        seen.value = value
        if (changed && value != null) {
            scale.snapTo(POP_SCALE)
            scale.animateTo(1f, tween(POP_MILLIS))
        }
    }
    return scale.asState()
}
