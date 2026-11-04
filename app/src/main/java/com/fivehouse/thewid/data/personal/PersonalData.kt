package com.fivehouse.thewid.data.personal

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

const val MAX_PEOPLE = 4
const val MIN_OPTIONS = 2
const val MAX_OPTIONS = 6
const val BUSY_ALERT_COUNT = 5
const val LOW_BATTERY_PERCENT = 15

enum class DayKind(val shape: ShapeCatalog) {
    Deadline(ShapeCatalog.Diamond),
    Busy(ShapeCatalog.Burst),
    Birthday(ShapeCatalog.Heart),
    Trip(ShapeCatalog.Arrow)
}

@Serializable
data class WeekEvent(
    val id: String = UUID.randomUUID().toString(),
    val epochDay: Long,
    val label: String,
    val kind: DayKind
) {
    val date: LocalDate get() = LocalDate.ofEpochDay(epochDay)
}

val PersonShapes = listOf(ShapeCatalog.Heart, ShapeCatalog.Flower, ShapeCatalog.Clover4Leaf, ShapeCatalog.Clover8Leaf)

/** @property lookupUri the contact to open. Only the name is copied at pick time, with no contacts access. */
@Serializable
data class Person(val lookupUri: String, val name: String, val shape: ShapeCatalog) {
    val initial: String get() = name.trim().take(1).uppercase()
}

val OptionShapes = listOf(
    ShapeCatalog.Cookie6Sided,
    ShapeCatalog.Clover4Leaf,
    ShapeCatalog.Diamond,
    ShapeCatalog.Sunny,
    ShapeCatalog.Flower,
    ShapeCatalog.Puffy
)

@Serializable
data class DecisionOption(val label: String, val shape: ShapeCatalog)

@Serializable
data class Decision(
    val question: String = "",
    val options: List<DecisionOption> = emptyList(),
    val lastIndex: Int? = null
) {
    val canRoll: Boolean get() = options.count { it.label.isNotBlank() } >= MIN_OPTIONS
    val result: DecisionOption? get() = lastIndex?.let(options::getOrNull)

    /** Picks an option, never the same one twice in a row, so a roll always visibly changes something. */
    fun rolled(random: Random = Random.Default): Decision {
        if (!canRoll) return this
        val candidates = options.indices.filter { it != lastIndex }
        return copy(lastIndex = candidates.random(random))
    }
}

@Serializable
data class MochiSettings(
    val name: String = "Mochi",
    val charging: Boolean = true,
    val lowBattery: Boolean = true,
    val silent: Boolean = true,
    val wifiOff: Boolean = true,
    val busy: Boolean = false
)

enum class MochiMood { Happy, Excited, Sleepy, Dozing, Lonely, Busy }

/** The strongest mood the user has switched on wins, in this order. */
fun moodFor(
    settings: MochiSettings,
    isCharging: Boolean,
    batteryPercent: Int?,
    isSilent: Boolean,
    isWifiOff: Boolean,
    alertCount: Int?
): MochiMood = when {
    settings.charging && isCharging -> MochiMood.Excited
    settings.lowBattery && batteryPercent != null && batteryPercent < LOW_BATTERY_PERCENT -> MochiMood.Sleepy
    settings.busy && alertCount != null && alertCount >= BUSY_ALERT_COUNT -> MochiMood.Busy
    settings.silent && isSilent -> MochiMood.Dozing
    settings.wifiOff && isWifiOff -> MochiMood.Lonely
    else -> MochiMood.Happy
}

@Serializable
data class BreatheLog(val epochDay: Long = 0, val sessionsToday: Int = 0) {
    fun sessionsOn(today: LocalDate): Int = if (epochDay == today.toEpochDay()) sessionsToday else 0

    fun afterSession(today: LocalDate): BreatheLog = BreatheLog(today.toEpochDay(), sessionsOn(today) + 1)
}

enum class MinuteMarker { Diamond, Circle, None }

/**
 * @property latitude rounded and kept on this phone, for working out sunset. Never sent anywhere.
 * @property exactMinute keeps the minute marker exact, using the exact alarm permission.
 */
@Serializable
data class TimeSettings(
    val marker: MinuteMarker = MinuteMarker.Diamond,
    val switchAtSunset: Boolean = false,
    val showDayTrack: Boolean = true,
    val seconds: Boolean = false,
    val exactMinute: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null
)

/** How many messages are waiting from each person, by matching notification sender names. */
fun messagesPerPerson(people: List<Person>, senders: List<String>): Map<Person, Int> = people.associateWith { person ->
    senders.count { it.trim().equals(person.name.trim(), ignoreCase = true) }
}

/** Calendar dates only show as Busy, since a calendar doesn't say what kind of day an event is. */
fun withCalendar(events: List<WeekEvent>, calendar: List<WeekEvent>): List<WeekEvent> = events + calendar

private val Context.personalStore: DataStore<Preferences> by preferencesDataStore("personal")

@Singleton
class PersonalRepository @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    val events: Flow<List<WeekEvent>> = read(EventsKey, ListSerializer(WeekEvent.serializer()), emptyList())
    val people: Flow<List<Person>> = read(PeopleKey, ListSerializer(Person.serializer()), emptyList())
    val decision: Flow<Decision> = read(DecisionKey, Decision.serializer(), Decision())
    val mochi: Flow<MochiSettings> = read(MochiKey, MochiSettings.serializer(), MochiSettings())
    val breathe: Flow<BreatheLog> = read(BreatheKey, BreatheLog.serializer(), BreatheLog())
    val timeSettings: Flow<TimeSettings> = read(TimeKey, TimeSettings.serializer(), TimeSettings())
    val useCalendar: Flow<Boolean> = read(CalendarKey, Boolean.serializer(), false)
    val focus: Flow<FocusState> = read(FocusKey, FocusState.serializer(), FocusState())

    suspend fun setEvents(value: List<WeekEvent>) = write(EventsKey, ListSerializer(WeekEvent.serializer()), value)

    suspend fun setPeople(value: List<Person>) = write(PeopleKey, ListSerializer(Person.serializer()), value)

    suspend fun setDecision(value: Decision) = write(DecisionKey, Decision.serializer(), value)

    suspend fun setMochi(value: MochiSettings) = write(MochiKey, MochiSettings.serializer(), value)

    suspend fun setTimeSettings(value: TimeSettings) = write(TimeKey, TimeSettings.serializer(), value)

    suspend fun setUseCalendar(value: Boolean) = write(CalendarKey, Boolean.serializer(), value)

    suspend fun updateFocus(change: (FocusState) -> FocusState) =
        update(FocusKey, FocusState.serializer(), FocusState(), change)

    suspend fun rollDecision() = update(DecisionKey, Decision.serializer(), Decision()) { it.rolled() }

    suspend fun logBreatheSession(today: LocalDate) =
        update(BreatheKey, BreatheLog.serializer(), BreatheLog()) { it.afterSession(today) }

    private fun <T> read(key: Preferences.Key<String>, serializer: KSerializer<T>, fallback: T): Flow<T> =
        context.personalStore.data.map { prefs -> decode(prefs[key], serializer, fallback) }

    private suspend fun <T> write(key: Preferences.Key<String>, serializer: KSerializer<T>, value: T) {
        context.personalStore.edit { it[key] = json.encodeToString(serializer, value) }
    }

    private suspend fun <T> update(
        key: Preferences.Key<String>,
        serializer: KSerializer<T>,
        fallback: T,
        change: (T) -> T
    ) {
        context.personalStore.edit { prefs ->
            prefs[key] = json.encodeToString(serializer, change(decode(prefs[key], serializer, fallback)))
        }
    }

    private fun <T> decode(raw: String?, serializer: KSerializer<T>, fallback: T): T = try {
        raw?.let { json.decodeFromString(serializer, it) } ?: fallback
    } catch (_: SerializationException) {
        fallback // Saved by an older shape of this data. Starting over beats crashing the widget.
    }

    private companion object {
        val EventsKey = stringPreferencesKey("week_events")
        val PeopleKey = stringPreferencesKey("people")
        val DecisionKey = stringPreferencesKey("decision")
        val MochiKey = stringPreferencesKey("mochi")
        val BreatheKey = stringPreferencesKey("breathe")
        val TimeKey = stringPreferencesKey("time_settings")
        val CalendarKey = stringPreferencesKey("use_calendar")
        val FocusKey = stringPreferencesKey("focus")
    }
}
