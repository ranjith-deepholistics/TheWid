package com.fivehouse.thewid.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fivehouse.thewid.R
import com.fivehouse.thewid.data.personal.BREATHE_ROUNDS
import com.fivehouse.thewid.data.personal.BreathPhase
import com.fivehouse.thewid.data.personal.BreathState
import com.fivehouse.thewid.data.personal.SessionMillis
import com.fivehouse.thewid.data.personal.breathStateAt
import com.fivehouse.thewid.ui.components.BackButton
import com.fivehouse.thewid.ui.components.GlassTile
import com.fivehouse.thewid.ui.components.InfoChip
import com.fivehouse.thewid.ui.components.PrimaryPill
import com.fivehouse.thewid.ui.components.ScreenPreview
import com.fivehouse.thewid.ui.shapes.GlossyShape
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import com.fivehouse.thewid.ui.theme.Accent
import com.fivehouse.thewid.ui.theme.Atmosphere
import com.fivehouse.thewid.ui.theme.Numerals
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val DIAL_TICKS = 72
private const val SMALL_SCALE = 0.72f

/** Screen from concept shot 32. Keeps running time itself so pausing is just not advancing the clock. */
@Composable
fun BreatheSessionScreen(
    onBack: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    heartPulseOn: Boolean = false,
    onBreathChange: () -> Unit = {}
) {
    var elapsed by rememberSaveable { mutableLongStateOf(0L) }
    var paused by rememberSaveable { mutableStateOf(false) }
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(paused) {
        if (paused) return@LaunchedEffect
        var last = withFrameMillis { it }
        while (elapsed < SessionMillis) {
            val now = withFrameMillis { it }
            elapsed += now - last
            last = now
        }
        onFinished()
    }
    val state = breathStateAt(elapsed)
    LaunchedEffect(state.round, state.phase, paused) {
        if (!paused && !state.isComplete) onBreathChange()
    }
    BreatheSessionContent(state, paused, onBack, { paused = !paused }, modifier, heartPulseOn)
}

@Composable
private fun BreatheSessionContent(
    state: BreathState,
    paused: Boolean,
    onBack: () -> Unit,
    onPauseToggle: () -> Unit,
    modifier: Modifier = Modifier,
    heartPulseOn: Boolean = false
) {
    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            BackButton(onBack, Modifier.align(Alignment.CenterStart))
            Text(stringResource(R.string.breathe_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.breathe_rounds, state.round, BREATHE_ROUNDS),
                Modifier.align(Alignment.CenterEnd),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.weight(1f))
        BreathDial(state)
        Spacer(Modifier.height(16.dp))
        Text("${state.secondsLeft}", style = Numerals.Hero)
        Text(
            stringResource(
                when (state.phase) {
                    BreathPhase.Inhale -> R.string.breathe_inhale
                    BreathPhase.Hold -> R.string.breathe_hold
                    BreathPhase.Exhale -> R.string.breathe_exhale
                }
            ),
            style = MaterialTheme.typography.titleLarge
        )
        if (heartPulseOn) {
            Spacer(Modifier.height(12.dp))
            InfoChip(stringResource(R.string.breathe_heart_chip), border = Accent)
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onPauseToggle, shape = CircleShape) {
            Text(stringResource(if (paused) R.string.breathe_resume else R.string.breathe_pause))
        }
        Spacer(Modifier.height(12.dp))
    }
}

/** Rule 4: the shape morphs between Circle and Flower while growing and shrinking with the breath. */
@Composable
private fun BreathDial(state: BreathState, modifier: Modifier = Modifier) {
    val isOpen = state.phase != BreathPhase.Exhale
    val scale by animateFloatAsState(
        targetValue = if (isOpen) 1f else SMALL_SCALE,
        animationSpec = tween(
            durationMillis =
            (if (state.phase == BreathPhase.Exhale) BreathPhase.Exhale else BreathPhase.Inhale).seconds * 1_000
        ),
        label = "breathScale"
    )
    Box(modifier.size(300.dp), contentAlignment = Alignment.Center) {
        ProgressTicks(state.sessionProgress, Modifier.fillMaxSize())
        GlossyShape(
            shape = if (isOpen) ShapeCatalog.Flower else ShapeCatalog.Circle,
            contentDescription = null,
            modifier = Modifier
                .size(220.dp)
                .scale(scale),
            castShadow = true
        )
    }
}

@Composable
internal fun ProgressTicks(progress: Float, modifier: Modifier = Modifier) {
    val ink = LocalContentColor.current
    Canvas(modifier) {
        val outer = size.minDimension / 2
        val inner = outer - 12.dp.toPx()
        val width = 1.5.dp.toPx()
        repeat(DIAL_TICKS) { index ->
            val fraction = index.toFloat() / DIAL_TICKS
            val angle = 2 * PI * fraction - PI / 2
            val unit = Offset(cos(angle).toFloat(), sin(angle).toFloat())
            drawLine(
                color = if (fraction <= progress) ink else ink.copy(alpha = 0.2f),
                start = center + unit * inner,
                end = center + unit * outer,
                strokeWidth = width,
                cap = StrokeCap.Round
            )
        }
    }
}

/** Screen 22: the flower settles into a soft burst. */
@Composable
fun BreatheCompleteScreen(
    sessionsToday: Int,
    onAgain: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    heartPulseOn: Boolean = false
) {
    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        GlossyShape(ShapeCatalog.SoftBurst, null, Modifier.size(200.dp), castShadow = true) {
            Icon(painterResource(R.drawable.ic_check), null, Modifier.size(64.dp))
        }
        Text(stringResource(R.string.breathe_done_title), style = MaterialTheme.typography.headlineLarge)
        if (heartPulseOn) {
            Text(stringResource(R.string.breathe_heart_done), style = MaterialTheme.typography.bodySmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("$BREATHE_ROUNDS", R.string.breathe_stat_rounds, Modifier.weight(1f))
            Stat(
                stringResource(R.string.breathe_minutes_value, Math.round(SessionMillis / 60_000.0).toInt()),
                R.string.breathe_stat_minutes,
                Modifier.weight(1f)
            )
            Stat("$sessionsToday", R.string.breathe_stat_today, Modifier.weight(1f))
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(onClick = onAgain, modifier = Modifier.height(56.dp)) {
                Text(stringResource(R.string.breathe_again))
            }
            PrimaryPill(stringResource(R.string.breathe_done), onDone, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(value: String, label: Int, modifier: Modifier = Modifier) {
    GlassTile(modifier) {
        Column {
            Text(value, style = Numerals.Medium)
            Text(stringResource(label), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(name = "Inhale", widthDp = 390, heightDp = 844)
@Composable
private fun BreatheInhalePreview() {
    ScreenPreview(Atmosphere.Mint) { BreatheSessionContent(breathStateAt(21_000), false, {}, {}) }
}

@Preview(name = "Paused on exhale", widthDp = 390, heightDp = 844)
@Composable
private fun BreathePausedPreview() {
    ScreenPreview(Atmosphere.Mint) { BreatheSessionContent(breathStateAt(14_000), true, {}, {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun BreatheCompletePreview() {
    ScreenPreview(Atmosphere.Mint) { BreatheCompleteScreen(3, {}, {}) }
}

@Preview(name = "With Heart Pulse", widthDp = 390, heightDp = 844)
@Composable
private fun BreatheCompleteHeartPreview() {
    ScreenPreview(Atmosphere.Mint) { BreatheCompleteScreen(3, {}, {}, heartPulseOn = true) }
}
