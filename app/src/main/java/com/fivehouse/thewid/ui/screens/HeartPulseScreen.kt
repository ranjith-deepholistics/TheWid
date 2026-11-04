package com.fivehouse.thewid.ui.screens

import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fivehouse.thewid.R
import com.fivehouse.thewid.data.personal.HeartPulseSettings
import com.fivehouse.thewid.data.personal.PulseColor
import com.fivehouse.thewid.heartpulse.HeartRings
import com.fivehouse.thewid.ui.components.BackButton
import com.fivehouse.thewid.ui.components.GlassTile
import com.fivehouse.thewid.ui.components.PrimaryPill
import com.fivehouse.thewid.ui.components.ScreenPreview
import com.fivehouse.thewid.ui.theme.Atmosphere

private const val PREVIEW_BEAT_MS = 1_400

/** Screen 8. */
@Composable
fun HeartPulseScreen(
    settings: HeartPulseSettings,
    canDrawOverlays: Boolean,
    hasNotificationAccess: Boolean,
    onChange: (HeartPulseSettings) -> Unit,
    onNotificationToggle: (Boolean) -> Unit,
    onTurnOn: () -> Unit,
    onTurnOff: () -> Unit,
    onTest: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOn = settings.enabled && canDrawOverlays
    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                BackButton(onBack, Modifier.align(Alignment.CenterStart))
                RingsPreview(Color(settings.color.argb), Modifier.size(140.dp))
            }
            Text(stringResource(R.string.widget_heart_pulse), style = MaterialTheme.typography.displaySmall)
            Text(stringResource(R.string.heart_pulse_body), style = MaterialTheme.typography.bodyMedium)
            GlassTile(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TriggerRow(
                        R.string.heart_pulse_notification,
                        settings.onNotification && hasNotificationAccess,
                        hint = if (hasNotificationAccess) null else R.string.heart_pulse_needs_notifications,
                        onChange = onNotificationToggle
                    )
                    TriggerRow(R.string.heart_pulse_charging, settings.onCharging) {
                        onChange(settings.copy(onCharging = it))
                    }
                    TriggerRow(R.string.heart_pulse_breathe, settings.duringBreathe) {
                        onChange(settings.copy(duringBreathe = it))
                    }
                    TriggerRow(R.string.heart_pulse_hour, settings.everyHour) {
                        onChange(settings.copy(everyHour = it))
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.heart_pulse_colour), Modifier.weight(1f))
                ColourChoices(settings.color, { onChange(settings.copy(color = it)) })
            }
        }
        Text(
            stringResource(R.string.heart_pulse_needs_overlay),
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
        if (isOn) {
            TextButton(onClick = onTest, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.heart_pulse_test))
            }
            PrimaryPill(stringResource(R.string.heart_pulse_turn_off), onTurnOff)
        } else {
            PrimaryPill(stringResource(R.string.heart_pulse_turn_on), onTurnOn)
        }
    }
}

@Composable
private fun RingsPreview(color: Color, modifier: Modifier = Modifier) {
    val rings = remember { HeartRings() }
    val progress by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(PREVIEW_BEAT_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "pulseProgress"
    )
    Box(
        modifier.drawBehind {
            drawIntoCanvas {
                rings.draw(it.nativeCanvas, size.minDimension, density, color.toArgbInt(), progress)
            }
        }
    )
}

private fun Color.toArgbInt(): Int = android.graphics.Color.argb(alpha, red, green, blue)

@Composable
private fun TriggerRow(
    @StringRes title: Int,
    checked: Boolean,
    modifier: Modifier = Modifier,
    @StringRes hint: Int? = null,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(title))
            if (hint != null) Text(stringResource(hint), style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ColourChoices(selected: PulseColor, onChoose: (PulseColor) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        PulseColor.entries.forEach { choice ->
            val isSelected = choice == selected
            Box(
                Modifier
                    .size(48.dp)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onChoose(choice) }),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(choice.argb))
                        .then(if (isSelected) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
                )
            }
        }
    }
}

@Preview(name = "Off", widthDp = 390, heightDp = 844)
@Composable
private fun HeartPulseOffPreview() {
    ScreenPreview(Atmosphere.Night) {
        HeartPulseScreen(HeartPulseSettings(), false, false, {}, {}, {}, {}, {}, {})
    }
}

@Preview(name = "On", widthDp = 390, heightDp = 844)
@Composable
private fun HeartPulseOnPreview() {
    ScreenPreview(Atmosphere.Night) {
        HeartPulseScreen(HeartPulseSettings(enabled = true, color = PulseColor.Mint), true, true, {
        }, {}, {}, {}, {}, {})
    }
}
