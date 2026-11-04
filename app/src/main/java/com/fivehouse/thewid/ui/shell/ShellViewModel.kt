package com.fivehouse.thewid.ui.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fivehouse.thewid.data.settings.AppPreferences
import com.fivehouse.thewid.data.settings.AppPreferencesRepository
import com.fivehouse.thewid.data.settings.ThemeChoice
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import com.fivehouse.thewid.ui.theme.Atmosphere
import com.fivehouse.thewid.widget.PlacedWidget
import com.fivehouse.thewid.widget.PlacedWidgets
import com.fivehouse.thewid.widget.StatusFace
import com.fivehouse.thewid.widget.StatusFaces
import com.fivehouse.thewid.widget.StatusWidgetKind
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val placedWidgets: PlacedWidgets
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

    fun finishOnboarding() = update { setOnboardingDone() }

    fun setHero(kind: StatusWidgetKind) = update { setHero(kind) }

    fun setTheme(theme: ThemeChoice) = update { setTheme(theme) }

    fun setAtmosphere(atmosphere: Atmosphere?) = update { setAtmosphere(atmosphere) }

    fun addRecent(kind: StatusWidgetKind) = update { addRecent(kind) }

    private fun update(change: suspend AppPreferencesRepository.() -> Unit) {
        viewModelScope.launch { preferencesRepository.change() }
    }
}
