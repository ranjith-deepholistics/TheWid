package com.fivehouse.thewid.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fivehouse.thewid.ui.theme.Atmosphere
import com.fivehouse.thewid.widget.StatusWidgetKind
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeChoice { Light, Dark, Auto }

const val MAX_RECENT_SEARCHES = 3
private const val RECENT_SEPARATOR = ","

data class AppPreferences(
    val onboardingDone: Boolean = false,
    val hero: StatusWidgetKind = StatusWidgetKind.Time,
    val theme: ThemeChoice = ThemeChoice.Auto,
    val atmosphere: Atmosphere? = null,
    val recents: List<StatusWidgetKind> = emptyList()
) {
    /**
     * Dark theme uses Night throughout. Otherwise a chosen atmosphere replaces each screen's own,
     * and with none chosen every screen keeps the atmosphere it was designed with.
     */
    fun atmosphereFor(screenDefault: Atmosphere, systemDark: Boolean): Atmosphere {
        val dark = theme == ThemeChoice.Dark || (theme == ThemeChoice.Auto && systemDark)
        return if (dark) Atmosphere.Night else atmosphere ?: screenDefault
    }
}

/** The new value first, without duplicates, capped at [MAX_RECENT_SEARCHES]. */
fun List<StatusWidgetKind>.withRecent(kind: StatusWidgetKind): List<StatusWidgetKind> =
    (listOf(kind) + filterNot { it == kind }).take(MAX_RECENT_SEARCHES)

private val Context.dataStore by preferencesDataStore("app_preferences")

@Singleton
class AppPreferencesRepository @Inject constructor(@param:ApplicationContext private val context: Context) {
    val preferences: Flow<AppPreferences> = context.dataStore.data.map { it.toAppPreferences() }

    suspend fun setOnboardingDone() = context.dataStore.edit { it[OnboardingDone] = true }

    suspend fun setHero(kind: StatusWidgetKind) = context.dataStore.edit { it[Hero] = kind.name }

    suspend fun setTheme(theme: ThemeChoice) = context.dataStore.edit { it[Theme] = theme.name }

    suspend fun setAtmosphere(atmosphere: Atmosphere?) = context.dataStore.edit { prefs ->
        if (atmosphere == null) prefs.remove(ChosenAtmosphere) else prefs[ChosenAtmosphere] = atmosphere.name
    }

    suspend fun addRecent(kind: StatusWidgetKind) = context.dataStore.edit { prefs ->
        prefs[Recents] = prefs.toAppPreferences().recents.withRecent(kind).joinToString(RECENT_SEPARATOR) { it.name }
    }

    private fun Preferences.toAppPreferences() = AppPreferences(
        onboardingDone = this[OnboardingDone] ?: false,
        hero = enumOrNull<StatusWidgetKind>(this[Hero]) ?: StatusWidgetKind.Time,
        theme = enumOrNull<ThemeChoice>(this[Theme]) ?: ThemeChoice.Auto,
        atmosphere = enumOrNull<Atmosphere>(this[ChosenAtmosphere]),
        recents = this[Recents].orEmpty().split(RECENT_SEPARATOR).mapNotNull { enumOrNull<StatusWidgetKind>(it) }
    )

    private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
        enumValues<T>().firstOrNull { it.name == name }

    private companion object {
        val OnboardingDone = booleanPreferencesKey("onboarding_done")
        val Hero = stringPreferencesKey("hero")
        val Theme = stringPreferencesKey("theme")
        val ChosenAtmosphere = stringPreferencesKey("atmosphere")
        val Recents = stringPreferencesKey("recents")
    }
}
