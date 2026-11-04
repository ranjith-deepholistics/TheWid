package com.fivehouse.thewid.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

// This file is the only one that knows about Haze. To move to official Compose backdrop blur,
// reimplement the three declarations below and nothing else changes.

/** Links the layers that get blurred to the tiles that blur them. */
@Stable
class FrostedGlassState internal constructor(internal val haze: HazeState)

@Composable
fun rememberFrostedGlassState(): FrostedGlassState = remember { FrostedGlassState(HazeState()) }

/** Marks this layer as something frosted glass above it can blur. */
fun Modifier.frostedGlassSource(state: FrostedGlassState): Modifier = hazeSource(state.haze)

/** Blurs whatever [frostedGlassSource] layers sit behind this element, then washes it with [tint]. */
fun Modifier.frostedGlass(state: FrostedGlassState, tint: Color): Modifier = hazeEffect(
    state = state.haze,
    style = HazeStyle(
        tint = HazeTint(tint),
        blurRadius = 24.dp,
        noiseFactor = 0f,
        fallbackTint = HazeTint(tint)
    )
)
