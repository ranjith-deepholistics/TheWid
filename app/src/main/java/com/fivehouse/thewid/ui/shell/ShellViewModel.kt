package com.fivehouse.thewid.ui.shell

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fivehouse.thewid.data.personal.BreatheLog
import com.fivehouse.thewid.data.personal.Decision
import com.fivehouse.thewid.data.personal.FocusState
import com.fivehouse.thewid.data.personal.HeartPulseSettings
import com.fivehouse.thewid.data.personal.MochiSettings
import com.fivehouse.thewid.data.personal.Person
import com.fivehouse.thewid.data.personal.PersonalRepository
import com.fivehouse.thewid.data.personal.TimeSettings
import com.fivehouse.thewid.data.personal.WeekEvent
import com.fivehouse.thewid.data.settings.AppPreferences
import com.fivehouse.thewid.data.settings.AppPreferencesRepository
import com.fivehouse.thewid.data.settings.ThemeChoice
import com.fivehouse.thewid.data.status.NowPlayingRepository
import com.fivehouse.thewid.data.status.NowPlayingStatus
import com.fivehouse.thewid.data.status.platform.MediaTransport
import com.fivehouse.thewid.heartpulse.HeartPulse
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import com.fivehouse.thewid.ui.theme.Atmosphere
import com.fivehouse.thewid.widget.ExactMinute
import com.fivehouse.thewid.widget.PlacedWidget
import com.fivehouse.thewid.widget.PlacedWidgets
import com.fivehouse.thewid.widget.StatusFace
import com.fivehouse.thewid.widget.StatusFaces
import com.fivehouse.thewid.widget.StatusWidgetKind
import com.fivehouse.thewid.widget.WidgetRefresh
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MS = 5_000L

@HiltViewModel
class ShellViewModel @Inject constructor(
    private val preferencesRepository: AppPreferencesRepository,
    private val statusFaces: StatusFaces,
    private val placedWidgets: PlacedWidgets,
    private val personal: PersonalRepository,
    private val nowPlayingRepository: NowPlayingRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {
    /** Null until read from disk, so the splash knows when it can route. */
    val preferences: StateFlow<AppPreferences?> = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val faces: StateFlow<Map<StatusWidgetKind, StatusFace>> =
        combine(StatusWidgetKind.entries.map { kind -> statusFaces.faces(kind).map { kind to it } }) { it.toMap() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyMap())

    private val placedState = MutableStateFlow<List<PlacedWidget>?>(null)
    val placed: StateFlow<List<PlacedWidget>?> = placedState.asStateFlow()

    fun face(kind: StatusWidgetKind, shape: ShapeCatalog?, glow: Boolean): Flow<StatusFace> =
        statusFaces.faces(kind, shape, glow)

    fun refreshPlaced() {
        viewModelScope.launch { placedState.value = placedWidgets.load() }
    }

    val events = personal.events.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())
    val people = personal.people.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())
    val decision = personal.decision.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        Decision()
    )
    val mochi = personal.mochi.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MochiSettings())
    val breathe = personal.breathe.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        BreatheLog()
    )

    val timeSettings = personal.timeSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TimeSettings())
    val useCalendar = personal.useCalendar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)
    val focus = personal.focus.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), FocusState())
    val nowPlaying = nowPlayingRepository.status
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), NowPlayingStatus.Nothing)

    val heartPulse = personal.heartPulse
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HeartPulseSettings())

    fun setHeartPulse(value: HeartPulseSettings) {
        viewModelScope.launch {
            personal.setHeartPulse(value)
            HeartPulse.syncHourly(context, value)
        }
    }

    private val transport = MediaTransport(context)

    fun playPause() = transport.playPause()

    fun nextTrack() = transport.next()

    fun previousTrack() = transport.previous()

    fun rollDecision() = updatePersonal { rollDecision() }

    fun setTimeSettings(value: TimeSettings) = updatePersonal {
        setTimeSettings(value)
        if (value.exactMinute) ExactMinute.scheduleNext(context) else ExactMinute.cancel(context)
    }

    fun setUseCalendar(value: Boolean) = updatePersonal { setUseCalendar(value) }

    fun updateFocus(change: (FocusState) -> FocusState) = updatePersonal { updateFocus(change) }

    fun setEvents(value: List<WeekEvent>) = updatePersonal { setEvents(value) }

    fun setPeople(value: List<Person>) = updatePersonal { setPeople(value) }

    fun setDecision(value: Decision) = updatePersonal { setDecision(value) }

    fun setMochi(value: MochiSettings) = updatePersonal { setMochi(value) }

    fun logBreatheSession() = updatePersonal { logBreatheSession(LocalDate.now()) }

    private fun updatePersonal(change: suspend PersonalRepository.() -> Unit) {
        viewModelScope.launch {
            personal.change()
            WidgetRefresh.refreshNow(context)
        }
    }

    fun finishOnboarding() = update { setOnboardingDone() }

    fun setHero(kind: StatusWidgetKind) = update { setHero(kind) }

    fun setTheme(theme: ThemeChoice) = update { setTheme(theme) }

    fun setAtmosphere(atmosphere: Atmosphere?) = update { setAtmosphere(atmosphere) }

    fun addRecent(kind: StatusWidgetKind) = update { addRecent(kind) }

    private fun update(change: suspend AppPreferencesRepository.() -> Unit) {
        viewModelScope.launch { preferencesRepository.change() }
    }
}
