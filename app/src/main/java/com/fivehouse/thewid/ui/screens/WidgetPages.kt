package com.fivehouse.thewid.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fivehouse.thewid.R
import com.fivehouse.thewid.data.personal.Decision
import com.fivehouse.thewid.data.personal.DecisionOption
import com.fivehouse.thewid.data.personal.FOCUS_SESSIONS
import com.fivehouse.thewid.data.personal.FocusState
import com.fivehouse.thewid.data.personal.MinuteMarker
import com.fivehouse.thewid.data.personal.MochiMood
import com.fivehouse.thewid.data.personal.Person
import com.fivehouse.thewid.data.personal.TimeSettings
import com.fivehouse.thewid.data.status.NowPlayingStatus
import com.fivehouse.thewid.data.status.TimeStatus
import com.fivehouse.thewid.ui.components.BackButton
import com.fivehouse.thewid.ui.components.DottedRing
import com.fivehouse.thewid.ui.components.Eyebrow
import com.fivehouse.thewid.ui.components.FaceShape
import com.fivehouse.thewid.ui.components.FaceValueText
import com.fivehouse.thewid.ui.components.GlassIconButton
import com.fivehouse.thewid.ui.components.GlassTile
import com.fivehouse.thewid.ui.components.InfoChip
import com.fivehouse.thewid.ui.components.LiveDot
import com.fivehouse.thewid.ui.components.PrimaryPill
import com.fivehouse.thewid.ui.components.ScreenPreview
import com.fivehouse.thewid.ui.components.SegmentedPills
import com.fivehouse.thewid.ui.shapes.FaceExpression
import com.fivehouse.thewid.ui.shapes.GlossyShape
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import com.fivehouse.thewid.ui.shapes.ShapeSurface
import com.fivehouse.thewid.ui.theme.Accent
import com.fivehouse.thewid.ui.theme.Atmosphere
import com.fivehouse.thewid.ui.theme.GlossTint
import com.fivehouse.thewid.ui.theme.Numerals
import com.fivehouse.thewid.widget.PersonTints
import com.fivehouse.thewid.widget.StatusFace
import com.fivehouse.thewid.widget.StatusWidgetKind
import com.fivehouse.thewid.widget.formatCountdown
import com.fivehouse.thewid.widget.resolve
import com.fivehouse.thewid.widget.sampleFace
import com.fivehouse.thewid.widget.toFace
import java.time.ZonedDateTime
import kotlinx.coroutines.delay

private const val SPIN_MILLIS = 8_000

@Composable
private fun RoundControl(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(if (primary) 72.dp else 52.dp),
        colors = if (primary) {
            IconButtonDefaults.filledIconButtonColors()
        } else {
            IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.White.copy(
                    alpha = if (LocalContentColor.current ==
                        Color.White
                    ) {
                        0.12f
                    } else {
                        0.5f
                    }
                ),
                contentColor = LocalContentColor.current
            )
        }
    ) {
        Icon(painterResource(icon), stringResource(label))
    }
}

/** Mockup 33: spins while it plays and squares up when paused. */
@Composable
fun NowPlayingPage(
    track: NowPlayingStatus,
    extraFaces: List<Pair<Int, StatusFace>>,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playing = (track as? NowPlayingStatus.Track)?.isPlaying == true
    val current = track as? NowPlayingStatus.Track
    var left by rememberSaveable(current?.leftMs) { mutableLongStateOf(current?.leftMs ?: 0L) }
    LaunchedEffect(current?.leftMs, playing) {
        while (playing && left > 0) {
            delay(1_000)
            left = (left - 1_000).coerceAtLeast(0)
        }
    }
    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.category_status), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.widget_now_playing), style = MaterialTheme.typography.headlineMedium)
            }
            if (playing) {
                LiveDot()
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.widget_live), style = MaterialTheme.typography.bodySmall)
            }
        }
        GlassTile(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Disc(track, playing, current?.durationMs?.let { 1f - left.toFloat() / it } ?: 0f)
                Spacer(Modifier.height(12.dp))
                Text(
                    current?.title ?: stringResource(R.string.now_playing_nothing),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val detail = listOfNotNull(
                    current?.artist,
                    current?.leftMs?.let { stringResource(R.string.now_playing_left, formatCountdown(left)) }
                ).joinToString(" · ")
                if (detail.isNotEmpty()) Text(detail, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RoundControl(R.drawable.ic_prev, R.string.now_playing_previous, onPrevious)
                    RoundControl(
                        if (playing) R.drawable.ic_pause else R.drawable.ic_play,
                        if (playing) R.string.now_playing_pause else R.string.now_playing_play,
                        onPlayPause,
                        primary = true
                    )
                    RoundControl(R.drawable.ic_next, R.string.now_playing_next, onNext)
                }
            }
        }
        GlassTile(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                ShapeLegend(ShapeCatalog.Cookie12Sided, R.string.widget_value_playing_label)
                ShapeLegend(ShapeCatalog.Square, R.string.widget_value_paused_label)
                extraFaces.forEach { (label, face) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FaceShape(face, null, Modifier.size(40.dp))
                        Text(stringResource(label), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(painterResource(R.drawable.ic_lock), null, Modifier.size(14.dp))
            Text(stringResource(R.string.now_playing_same_access), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ShapeLegend(shape: ShapeCatalog, @StringRes label: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GlossyShape(shape, null, Modifier.size(40.dp))
        Text(stringResource(label), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Disc(track: NowPlayingStatus, playing: Boolean, progress: Float, modifier: Modifier = Modifier) {
    val angle by rememberInfiniteTransition(label = "disc").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(SPIN_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "discAngle"
    )
    val face = track.toFace()
    Box(modifier.size(260.dp), contentAlignment = Alignment.Center) {
        DottedRing({ progress }, Modifier.fillMaxSize())
        FaceShape(
            face,
            null,
            Modifier
                .size(200.dp)
                .graphicsLayer { rotationZ = if (playing) angle else 0f }
        )
    }
}

@StringRes
private fun FaceExpression?.line(): Int = when (this) {
    FaceExpression.Excited -> R.string.mochi_line_excited
    FaceExpression.Sleepy -> R.string.mochi_line_sleepy
    FaceExpression.Dozing -> R.string.mochi_line_dozing
    FaceExpression.Lonely -> R.string.mochi_line_lonely
    FaceExpression.Busy -> R.string.mochi_line_busy
    else -> R.string.mochi_line_happy
}

/** Mockup 34. */
@Composable
fun MochiPage(
    face: StatusFace?,
    name: String,
    batteryFace: StatusFace?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.mochi_companion), style = MaterialTheme.typography.bodySmall)
                Text(name, style = MaterialTheme.typography.headlineMedium)
            }
            GlassIconButton(R.drawable.ic_pencil, stringResource(R.string.action_edit), onEdit)
        }
        GlassTile(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                batteryFace?.let { InfoChip(it.value.resolve(context)) }
                Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
                    if (face?.expression == FaceExpression.Excited) {
                        GlossyShape(
                            ShapeCatalog.Burst,
                            null,
                            Modifier.size(260.dp),
                            tint = GlossTint.Accent,
                            fill = 0.25f
                        )
                    }
                    face?.let { FaceShape(it, name, Modifier.size(180.dp)) }
                }
                Text(stringResource(face?.expression.line()), style = MaterialTheme.typography.bodyLarge)
            }
        }
        Text(stringResource(R.string.mochi_moods_title), style = MaterialTheme.typography.titleMedium)
        MochiMood.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { mood ->
                    MoodTile(mood, name, mood.toFace(name).expression == face?.expression, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MoodTile(mood: MochiMood, name: String, current: Boolean, modifier: Modifier = Modifier) {
    val face = mood.toFace(name)
    GlassTile(modifier, shape = MaterialTheme.shapes.large) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FaceShape(face, null, Modifier.size(52.dp))
            Column {
                FaceValueText(face, style = Numerals.Small)
                if (current) LiveDot()
            }
        }
    }
}

/** Mockup 36. Call and message ask for contacts access when tapped; the rest works without it. */
@Composable
fun InnerCirclePage(
    people: List<Person>,
    messages: Map<Person, Int>,
    canSeeMessages: Boolean,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onMessage: (Person) -> Unit,
    onCall: (Person) -> Unit,
    onVideo: (Person) -> Unit,
    onTurnOnMessages: () -> Unit,
    modifier: Modifier = Modifier
) {
    val person = people.getOrNull(selectedIndex) ?: return
    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Spacer(Modifier.size(12.dp))
            Text(
                stringResource(R.string.circle_page_title),
                Modifier.weight(1f),
                style = MaterialTheme.typography.headlineMedium
            )
            GlassIconButton(R.drawable.ic_pencil, stringResource(R.string.action_edit), onEdit)
        }
        Spacer(Modifier.weight(1f))
        GlossyShape(
            person.shape,
            person.name,
            Modifier.size(230.dp),
            tint = PersonTints[selectedIndex % PersonTints.size],
            castShadow = true
        ) {
            Text(person.initial, style = MaterialTheme.typography.displayLarge)
        }
        Text(person.name, style = MaterialTheme.typography.headlineSmall)
        MessageLine(messages[person] ?: 0, canSeeMessages, onTurnOnMessages)
        Spacer(Modifier.weight(1f))
        GlassTile(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                people.forEachIndexed { index, other ->
                    Column(
                        Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onSelect(index) }
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        GlossyShape(
                            other.shape,
                            null,
                            Modifier.size(48.dp),
                            tint = PersonTints[index % PersonTints.size]
                        ) {
                            Text(other.initial)
                        }
                        Text(
                            other.name,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RoundControl(R.drawable.ic_message, R.string.circle_message, { onMessage(person) })
            RoundControl(R.drawable.ic_phone, R.string.circle_call, { onCall(person) }, primary = true)
            RoundControl(R.drawable.ic_video, R.string.circle_video, { onVideo(person) })
        }
    }
}

@Composable
private fun MessageLine(count: Int, canSee: Boolean, onTurnOn: () -> Unit, modifier: Modifier = Modifier) {
    if (!canSee) {
        TextButton(onClick = onTurnOn, modifier = modifier) { Text(stringResource(R.string.circle_see_messages)) }
    } else if (count > 0) {
        Row(
            modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LiveDot()
            Text(
                pluralStringResource(R.plurals.circle_new_messages, count, count),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/** Mockup 37: let the shape choose. */
@Composable
fun DecidePage(
    decision: Decision,
    onRoll: () -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chosen = decision.result
    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.widget_decide), style = MaterialTheme.typography.bodySmall)
                Text(decision.question, style = MaterialTheme.typography.headlineSmall)
            }
            GlassIconButton(R.drawable.ic_pencil, stringResource(R.string.action_edit), onEdit)
        }
        Spacer(Modifier.weight(1f))
        GlossyShape(
            chosen?.shape ?: ShapeCatalog.Cookie6Sided,
            chosen?.label,
            Modifier.size(240.dp),
            tint = Atmosphere.Peach.gloss,
            castShadow = true
        )
        Text(chosen?.label.orEmpty(), style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        GlassTile(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                decision.options.forEach { option ->
                    OptionMark(option, option == chosen)
                }
            }
        }
        PrimaryPill(stringResource(R.string.decide_roll_again), onRoll)
    }
}

@Composable
private fun OptionMark(option: DecisionOption, selected: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        GlossyShape(
            option.shape,
            null,
            Modifier.size(40.dp),
            surface = if (selected) ShapeSurface.Glossy else ShapeSurface.Outline,
            tint = GlossTint.Accent
        )
        Text(option.label, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Mockup 28. */
@Composable
fun FocusPage(
    state: FocusState,
    nowMs: Long,
    onBack: () -> Unit,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
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
            Text(stringResource(R.string.focus_title), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(320.dp), contentAlignment = Alignment.Center) {
            ProgressTicks(state.progressAt(nowMs), Modifier.fillMaxSize())
            GlossyShape(
                if (state.onBreak) ShapeCatalog.Circle else ShapeCatalog.VerySunny,
                null,
                Modifier.size(210.dp),
                tint = Atmosphere.DuskBloom.gloss,
                castShadow = true
            )
        }
        Text(formatCountdown(state.remainingAt(nowMs)), style = Numerals.Hero)
        Text(
            stringResource(
                if (state.onBreak) R.string.focus_break_of else R.string.focus_session_of,
                state.session,
                FOCUS_SESSIONS
            ),
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            RoundControl(R.drawable.ic_reset, R.string.focus_reset, onReset)
            RoundControl(
                if (state.isRunning) R.drawable.ic_pause else R.drawable.ic_play,
                if (state.isRunning) R.string.now_playing_pause else R.string.now_playing_play,
                onToggle,
                primary = true
            )
            RoundControl(R.drawable.ic_next, R.string.focus_skip, onSkip)
        }
        Spacer(Modifier.height(12.dp))
    }
}

private val SampleDay = ZonedDateTime.parse("2026-11-02T09:42:00+05:30")
private val SampleNight = ZonedDateTime.parse("2026-11-02T21:12:00+05:30")

/** Screen 17. Options that need a permission ask for it when switched on. */
@Composable
fun TimeSetupScreen(
    settings: TimeSettings,
    locationMissing: Boolean,
    onChange: (TimeSettings) -> Unit,
    onSunsetToggle: (Boolean) -> Unit,
    onExactToggle: (Boolean) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            BackButton(onBack, Modifier.align(Alignment.CenterStart))
            Text(stringResource(R.string.widget_time), style = MaterialTheme.typography.titleMedium)
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimePreview(R.string.time_day, TimeStatus(SampleDay, isDay = true), settings, Modifier.weight(1f))
                TimePreview(R.string.time_night, TimeStatus(SampleNight, isDay = false), settings, Modifier.weight(1f))
            }
            Eyebrow(stringResource(R.string.time_marker))
            SegmentedPills(
                options = MinuteMarker.entries,
                selected = settings.marker,
                label = { marker ->
                    stringResource(
                        when (marker) {
                            MinuteMarker.Diamond -> R.string.time_marker_diamond
                            MinuteMarker.Circle -> R.string.time_marker_circle
                            MinuteMarker.None -> R.string.time_marker_none
                        }
                    )
                },
                onSelect = { onChange(settings.copy(marker = it)) },
                modifier = Modifier.fillMaxWidth()
            )
            GlassTile(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SwitchRow(R.string.time_sunset, settings.switchAtSunset, onChange = onSunsetToggle)
                    if (locationMissing) {
                        Text(stringResource(R.string.time_location_missing), style = MaterialTheme.typography.bodySmall)
                    }
                    SwitchRow(R.string.time_day_track, settings.showDayTrack) {
                        onChange(settings.copy(showDayTrack = it))
                    }
                    SwitchRow(R.string.time_seconds, settings.seconds) { onChange(settings.copy(seconds = it)) }
                    SwitchRow(R.string.time_exact, settings.exactMinute, onChange = onExactToggle)
                }
            }
        }
        PrimaryPill(stringResource(R.string.setup_save), onSave)
    }
}

@Composable
private fun TimePreview(
    @StringRes label: Int,
    status: TimeStatus,
    settings: TimeSettings,
    modifier: Modifier = Modifier
) {
    GlassTile(modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(label), style = MaterialTheme.typography.bodySmall)
            FaceShape(status.toFace(settings), null, Modifier.size(110.dp))
        }
    }
}

@Composable
private fun SwitchRow(
    @StringRes title: Int,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onChange: (Boolean) -> Unit
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(title), Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun NowPlayingPagePreview() {
    ScreenPreview(Atmosphere.Moss) {
        NowPlayingPage(
            NowPlayingStatus.Track("Track name", "Artist", true, null, 161_000, 240_000),
            listOf(R.string.widget_battery to sampleFace(StatusWidgetKind.Battery)),
            {},
            {},
            {},
            {}
        )
    }
}

@Preview(name = "Nothing playing", widthDp = 390, heightDp = 844)
@Composable
private fun NowPlayingEmptyPreview() {
    ScreenPreview(Atmosphere.Moss) { NowPlayingPage(NowPlayingStatus.Nothing, emptyList(), {}, {}, {}, {}) }
}

@Preview(widthDp = 390, heightDp = 1000)
@Composable
private fun MochiPagePreview() {
    ScreenPreview(Atmosphere.Moss) {
        MochiPage(
            MochiMood.Excited.toFace(
                "Mochi"
            ),
            "Mochi",
            sampleFace(StatusWidgetKind.Battery),
            {
            },
            {}
        )
    }
}

private val PreviewPeople = listOf(
    Person("a", "Partner", ShapeCatalog.Heart),
    Person("b", "Mom", ShapeCatalog.Flower),
    Person("c", "Sam", ShapeCatalog.Clover4Leaf)
)

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun InnerCirclePagePreview() {
    ScreenPreview(Atmosphere.DuskBloom) {
        InnerCirclePage(PreviewPeople, mapOf(PreviewPeople[0] to 2), true, 0, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(name = "No notification access", widthDp = 390, heightDp = 844)
@Composable
private fun InnerCircleNoAccessPreview() {
    ScreenPreview(Atmosphere.DuskBloom) {
        InnerCirclePage(PreviewPeople, emptyMap(), false, 1, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun DecidePagePreview() {
    ScreenPreview(Atmosphere.Peach) {
        DecidePage(
            Decision(
                "Where to eat tonight?",
                listOf(
                    DecisionOption("Dosa", ShapeCatalog.Cookie6Sided),
                    DecisionOption("Biryani", ShapeCatalog.Clover4Leaf)
                ),
                lastIndex = 1
            ),
            {},
            {},
            {}
        )
    }
}

@Preview(name = "Running", widthDp = 390, heightDp = 844)
@Composable
private fun FocusRunningPreview() {
    ScreenPreview(Atmosphere.DuskBloom) {
        FocusPage(FocusState(session = 3).started(0), 42_000, {}, {}, {}, {})
    }
}

@Preview(name = "On break", widthDp = 390, heightDp = 844)
@Composable
private fun FocusBreakPreview() {
    ScreenPreview(Atmosphere.DuskBloom) {
        FocusPage(FocusState(session = 1, onBreak = true, remainingMs = 120_000), 0, {}, {}, {}, {})
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun TimeSetupPreview() {
    ScreenPreview(Atmosphere.Night) { TimeSetupScreen(TimeSettings(), false, {}, {}, {}, {}, {}) }
}

@Preview(name = "Location missing", widthDp = 390, heightDp = 844)
@Composable
private fun TimeSetupNoLocationPreview() {
    ScreenPreview(Atmosphere.Night) {
        TimeSetupScreen(TimeSettings(switchAtSunset = true), true, {}, {}, {}, {}, {})
    }
}
