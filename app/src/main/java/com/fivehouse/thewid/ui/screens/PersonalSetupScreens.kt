package com.fivehouse.thewid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fivehouse.thewid.R
import com.fivehouse.thewid.data.personal.DayKind
import com.fivehouse.thewid.data.personal.Decision
import com.fivehouse.thewid.data.personal.DecisionOption
import com.fivehouse.thewid.data.personal.MAX_OPTIONS
import com.fivehouse.thewid.data.personal.MAX_PEOPLE
import com.fivehouse.thewid.data.personal.MIN_OPTIONS
import com.fivehouse.thewid.data.personal.MochiMood
import com.fivehouse.thewid.data.personal.MochiSettings
import com.fivehouse.thewid.data.personal.OptionShapes
import com.fivehouse.thewid.data.personal.Person
import com.fivehouse.thewid.data.personal.PersonShapes
import com.fivehouse.thewid.data.personal.WeekEvent
import com.fivehouse.thewid.ui.components.BackButton
import com.fivehouse.thewid.ui.components.Eyebrow
import com.fivehouse.thewid.ui.components.FaceShape
import com.fivehouse.thewid.ui.components.GlassTile
import com.fivehouse.thewid.ui.components.OutlineTile
import com.fivehouse.thewid.ui.components.PrimaryPill
import com.fivehouse.thewid.ui.components.ScreenPreview
import com.fivehouse.thewid.ui.components.ShapesTextField
import com.fivehouse.thewid.ui.shapes.GlossyShape
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import com.fivehouse.thewid.ui.theme.Atmosphere
import com.fivehouse.thewid.ui.theme.GlossTint
import com.fivehouse.thewid.ui.theme.Numerals
import com.fivehouse.thewid.ui.theme.ShapesTheme
import com.fivehouse.thewid.widget.PersonTints
import com.fivehouse.thewid.widget.StatusFace
import com.fivehouse.thewid.widget.toFace
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle

private const val MAX_LABEL_CHARS = 24

private val DayKind.label: Int
    get() = when (this) {
        DayKind.Deadline -> R.string.week_kind_deadline
        DayKind.Busy -> R.string.week_kind_busy
        DayKind.Birthday -> R.string.week_kind_birthday
        DayKind.Trip -> R.string.week_kind_trip
    }

private val DayKind.tint: GlossTint
    get() = if (this == DayKind.Deadline) GlossTint.Accent else Atmosphere.Stone.gloss

/** Screen 18. Dates are entered by hand and saved as they change, with no calendar access. */
@Composable
fun WeekSetupScreen(
    events: List<WeekEvent>,
    onChange: (List<WeekEvent>) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    usingCalendar: Boolean = false
) {
    var adding by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(if (usingCalendar) R.string.week_using_calendar else R.string.week_no_calendar),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(stringResource(R.string.week_title), style = MaterialTheme.typography.headlineLarge)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (events.isEmpty()) {
                Text(stringResource(R.string.week_empty), style = MaterialTheme.typography.bodyMedium)
            }
            events.sortedBy { it.epochDay }.forEach { event ->
                EventRow(event, onRemove = { onChange(events.filterNot { it.id == event.id }) })
            }
        }
        TextButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(painterResource(R.drawable.ic_plus), null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.week_add))
        }
        PrimaryPill(stringResource(R.string.setup_continue), onContinue)
    }
    if (adding) {
        AddEventDialog(
            onAdd = { event ->
                onChange(events + event)
                adding = false
            },
            onDismiss = { adding = false }
        )
    }
}

@Composable
private fun EventRow(event: WeekEvent, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    GlassTile(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(event.date.dayOfMonth.toString().padStart(2, '0'), style = Numerals.Medium)
                Text(
                    event.date.month.getDisplayName(TextStyle.SHORT, locale).uppercase(locale),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    event.label,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(stringResource(event.kind.label), style = MaterialTheme.typography.bodySmall)
            }
            GlossyShape(event.kind.shape, null, Modifier.size(40.dp), tint = event.kind.tint)
            IconButton(onClick = onRemove) {
                Icon(
                    painterResource(R.drawable.ic_close),
                    stringResource(R.string.week_remove, event.label),
                    Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEventDialog(onAdd: (WeekEvent) -> Unit, onDismiss: () -> Unit) {
    var label by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf(DayKind.Deadline) }
    var epochDay by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.week_add_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ShapesTextField(
                    value = label,
                    onValueChange = { label = it.take(MAX_LABEL_CHARS) },
                    label = stringResource(R.string.week_label)
                )
                TextButton(onClick = { pickingDate = true }) { Text(LocalDate.ofEpochDay(epochDay).toString()) }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DayKind.entries.forEach { option ->
                        FilterChip(
                            selected = option == kind,
                            onClick = { kind = option },
                            label = { Text(stringResource(option.label), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(WeekEvent(epochDay = epochDay, label = label.trim(), kind = kind)) },
                enabled = label.isNotBlank()
            ) { Text(stringResource(R.string.setup_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.setup_cancel)) } }
    )
    if (pickingDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.ofEpochDay(epochDay)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        epochDay = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                    }
                    pickingDate = false
                }) { Text(stringResource(R.string.setup_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickingDate = false }) { Text(stringResource(R.string.setup_cancel)) }
            }
        ) { DatePicker(state) }
    }
}

/** Screen 19. */
@Composable
fun InnerCircleSetupScreen(
    people: List<Person>,
    onAddPerson: () -> Unit,
    onChange: (List<Person>) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val selected = people.getOrNull(selectedIndex)
    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.circle_count, people.size, MAX_PEOPLE),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(stringResource(R.string.circle_title), style = MaterialTheme.typography.headlineLarge)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val slots = (people.indices.toList() + if (people.size < MAX_PEOPLE) listOf(-1) else emptyList()).chunked(2)
            slots.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { index ->
                        if (index >= 0) {
                            PersonTile(
                                people[index],
                                index,
                                index == selectedIndex,
                                { selectedIndex = index },
                                Modifier.weight(1f)
                            )
                        } else {
                            AddPersonTile(onAddPerson, Modifier.weight(1f))
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            if (selected != null) {
                Eyebrow(stringResource(R.string.circle_shape_for, selected.name.uppercase()))
                ShapeChoices(PersonShapes, selected.shape, onChoose = { shape ->
                    onChange(people.map { if (it == selected) it.copy(shape = shape) else it })
                })
                TextButton(onClick = {
                    onChange(people.filterNot { it == selected })
                    selectedIndex = 0
                }) { Text(stringResource(R.string.circle_remove, selected.name)) }
            }
            Text(stringResource(R.string.circle_privacy), style = MaterialTheme.typography.bodySmall)
        }
        PrimaryPill(stringResource(R.string.setup_save), onSave, enabled = people.isNotEmpty())
    }
}

@Composable
private fun PersonTile(
    person: Person,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = MaterialTheme.shapes.extraLarge
    GlassTile(
        modifier
            .aspectRatio(1f)
            .clip(shape)
            .then(if (selected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.tertiary, shape) else Modifier)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    ) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            GlossyShape(person.shape, null, Modifier.size(76.dp), tint = PersonTints[index % PersonTints.size]) {
                Text(person.initial, style = MaterialTheme.typography.headlineSmall)
            }
            Text(
                person.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AddPersonTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlineTile(onClick, modifier.aspectRatio(1f)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painterResource(R.drawable.ic_plus), null)
            Text(stringResource(R.string.circle_add), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ShapeChoices(
    options: List<ShapeCatalog>,
    chosen: ShapeCatalog,
    onChoose: (ShapeCatalog) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        options.forEach { option ->
            val selected = option == chosen
            val corner = RoundedCornerShape(16.dp)
            Box(
                Modifier
                    .size(56.dp)
                    .clip(corner)
                    .background(Color.White.copy(alpha = 0.6f))
                    .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, corner) else Modifier)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onChoose(option) }),
                contentAlignment = Alignment.Center
            ) {
                GlossyShape(option, option.name, Modifier.size(32.dp))
            }
        }
    }
}

/** Screen 20. Edits stay in the screen until Save. */
@Composable
fun DecideSetupScreen(
    decision: Decision,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onSave: (Decision) -> Unit
) {
    var question by rememberSaveable(decision.question) { mutableStateOf(decision.question) }
    var labels by rememberSaveable(decision.options) {
        val saved = decision.options.map { it.label }
        mutableStateOf(saved.ifEmpty { List(MIN_OPTIONS) { "" } })
    }
    val filled = labels.count { it.isNotBlank() }
    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.decide_range, MIN_OPTIONS, MAX_OPTIONS),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Eyebrow(stringResource(R.string.decide_question))
            ShapesTextField(
                value = question,
                onValueChange = { question = it.take(MAX_LABEL_CHARS * 2) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(R.string.decide_question_hint)
            )
            Eyebrow(stringResource(R.string.decide_options))
            labels.forEachIndexed { index, label ->
                OptionRow(
                    label = label,
                    shape = OptionShapes[index % OptionShapes.size],
                    canRemove = labels.size > MIN_OPTIONS,
                    onChange = { value ->
                        labels = labels.toMutableList().also { it[index] = value.take(MAX_LABEL_CHARS) }
                    },
                    onRemove = { labels = labels.toMutableList().also { it.removeAt(index) } }
                )
            }
            if (labels.size < MAX_OPTIONS) {
                OutlineTile(
                    onClick = { labels = labels + "" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_plus), null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.decide_add_option))
                    }
                }
            }
        }
        PrimaryPill(
            stringResource(R.string.setup_save),
            onClick = {
                val options = labels.mapIndexedNotNull { index, label ->
                    label.trim().takeIf { it.isNotEmpty() }?.let {
                        DecisionOption(it, OptionShapes[index % OptionShapes.size])
                    }
                }
                val unchanged = options == decision.options
                onSave(Decision(question.trim(), options, if (unchanged) decision.lastIndex else null))
            },
            enabled = filled >= MIN_OPTIONS
        )
    }
}

@Composable
private fun OptionRow(
    label: String,
    shape: ShapeCatalog,
    canRemove: Boolean,
    onChange: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GlossyShape(shape, shape.name, Modifier.size(40.dp), tint = Atmosphere.Peach.gloss)
        ShapesTextField(
            value = label,
            onValueChange = onChange,
            modifier = Modifier.weight(1f),
            placeholder = stringResource(R.string.decide_option_hint)
        )
        if (canRemove) {
            IconButton(onClick = onRemove) {
                Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_clear), Modifier.size(16.dp))
            }
        }
    }
}

/** Screen 21. */
@Composable
fun MochiSetupScreen(
    settings: MochiSettings,
    face: StatusFace?,
    modifier: Modifier = Modifier,
    onSave: (MochiSettings) -> Unit
) {
    var draft by remember(settings) { mutableStateOf(settings) }
    val name = draft.name.ifBlank { "Mochi" }
    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FaceShape(face ?: MochiMood.Happy.toFace(name), null, Modifier.size(96.dp), castShadow = true)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Eyebrow(stringResource(R.string.mochi_name))
                ShapesTextField(
                    value = draft.name,
                    onValueChange = { draft = draft.copy(name = it.take(MAX_LABEL_CHARS)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Eyebrow(stringResource(R.string.mochi_moods))
        GlassTile(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MoodRow(
                    ShapeCatalog.SoftBoom,
                    R.string.mochi_charging,
                    R.string.mochi_charging_body,
                    name,
                    draft.charging
                ) {
                    draft = draft.copy(charging = it)
                }
                MoodRow(
                    ShapeCatalog.Ghostish,
                    R.string.mochi_low_battery,
                    R.string.mochi_low_battery_body,
                    name,
                    draft.lowBattery
                ) {
                    draft = draft.copy(lowBattery = it)
                }
                MoodRow(ShapeCatalog.Puffy, R.string.mochi_silent, R.string.mochi_silent_body, name, draft.silent) {
                    draft = draft.copy(silent = it)
                }
                MoodRow(ShapeCatalog.Fan, R.string.mochi_wifi_off, R.string.mochi_wifi_off_body, name, draft.wifiOff) {
                    draft = draft.copy(wifiOff = it)
                }
                MoodRow(ShapeCatalog.SoftBurst, R.string.mochi_busy, R.string.mochi_busy_body, name, draft.busy) {
                    draft = draft.copy(busy = it)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        PrimaryPill(stringResource(R.string.setup_save), { onSave(draft) })
    }
}

@Composable
private fun MoodRow(
    shape: ShapeCatalog,
    title: Int,
    body: Int,
    name: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onChange: (Boolean) -> Unit
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        GlossyShape(shape, null, Modifier.size(32.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(body, name), style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private val SampleEvents = listOf(
    WeekEvent("1", LocalDate.of(2026, 11, 5).toEpochDay(), "Your event", DayKind.Deadline),
    WeekEvent("2", LocalDate.of(2026, 11, 14).toEpochDay(), "Their name", DayKind.Birthday)
)

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun WeekSetupPreview() {
    ScreenPreview(Atmosphere.Stone) { WeekSetupScreen(SampleEvents, {}, {}, {}) }
}

@Preview(name = "Empty", widthDp = 390, heightDp = 844)
@Composable
private fun WeekSetupEmptyPreview() {
    ScreenPreview(Atmosphere.Stone) { WeekSetupScreen(emptyList(), {}, {}, {}) }
}

private val SamplePeople = listOf(
    Person("content://a", "Partner", ShapeCatalog.Heart),
    Person("content://b", "Mom", ShapeCatalog.Flower)
)

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun InnerCircleSetupPreview() {
    ScreenPreview(Atmosphere.DuskBloom) { InnerCircleSetupScreen(SamplePeople, {}, {}, {}, {}) }
}

@Preview(name = "Empty", widthDp = 390, heightDp = 844)
@Composable
private fun InnerCircleSetupEmptyPreview() {
    ScreenPreview(Atmosphere.DuskBloom) { InnerCircleSetupScreen(emptyList(), {}, {}, {}, {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun DecideSetupPreview() {
    ScreenPreview(Atmosphere.Peach) {
        DecideSetupScreen(
            Decision(
                "Where to eat tonight?",
                listOf(DecisionOption("Dosa", ShapeCatalog.Cookie6Sided), DecisionOption("Pizza", ShapeCatalog.Diamond))
            ),
            {}
        ) {}
    }
}

@Preview(name = "Empty", widthDp = 390, heightDp = 844)
@Composable
private fun DecideSetupEmptyPreview() {
    ScreenPreview(Atmosphere.Peach) { DecideSetupScreen(Decision(), {}) {} }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun MochiSetupPreview() {
    ScreenPreview(Atmosphere.Stone) { ShapesTheme(Atmosphere.Stone) { MochiSetupScreen(MochiSettings(), null) {} } }
}
