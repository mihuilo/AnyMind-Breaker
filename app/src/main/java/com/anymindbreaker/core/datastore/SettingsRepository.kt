package com.anymindbreaker.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.anymindbreaker.core.common.game.Language
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

data class AppSettings(
    /** Language of the interface. */
    val uiLanguage: Language,
    /** Language of the puzzles; independent of the interface language. */
    val contentLanguage: Language,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val instantSudokuValidation: Boolean = true,
)

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun update(transform: (AppSettings) -> AppSettings)
}

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
    /** Used for both languages until the player chooses them. */
    private val defaultLanguage: Language,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data.map(::read)

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { preferences -> write(preferences, transform(read(preferences))) }
    }

    private fun read(preferences: Preferences): AppSettings {
        val uiLanguage = preferences[UI_LANGUAGE].toEnum(defaultLanguage)
        return AppSettings(
            uiLanguage = uiLanguage,
            contentLanguage = preferences[CONTENT_LANGUAGE].toEnum(uiLanguage),
            theme = preferences[THEME].toEnum(ThemeMode.SYSTEM),
            soundEnabled = preferences[SOUND_ENABLED] ?: true,
            vibrationEnabled = preferences[VIBRATION_ENABLED] ?: true,
            instantSudokuValidation = preferences[INSTANT_SUDOKU_VALIDATION] ?: true,
        )
    }

    private fun write(preferences: MutablePreferences, settings: AppSettings) {
        preferences[UI_LANGUAGE] = settings.uiLanguage.name
        preferences[CONTENT_LANGUAGE] = settings.contentLanguage.name
        preferences[THEME] = settings.theme.name
        preferences[SOUND_ENABLED] = settings.soundEnabled
        preferences[VIBRATION_ENABLED] = settings.vibrationEnabled
        preferences[INSTANT_SUDOKU_VALIDATION] = settings.instantSudokuValidation
    }

    private companion object {
        val UI_LANGUAGE = stringPreferencesKey("language")
        val CONTENT_LANGUAGE = stringPreferencesKey("contentLanguage")
        val THEME = stringPreferencesKey("theme")
        val SOUND_ENABLED = booleanPreferencesKey("soundEnabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibrationEnabled")
        val INSTANT_SUDOKU_VALIDATION = booleanPreferencesKey("instantSudokuValidation")

        /** An unknown stored value falls back to the default instead of failing. */
        inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
            enumValues<T>().firstOrNull { it.name == this } ?: default
    }
}

/** Keeps settings in memory. Used in tests instead of DataStore. */
class InMemorySettingsRepository(
    initial: AppSettings = AppSettings(uiLanguage = Language.EN, contentLanguage = Language.EN),
) : SettingsRepository {

    private val state = MutableStateFlow(initial)

    override val settings: Flow<AppSettings> = state

    override suspend fun update(transform: (AppSettings) -> AppSettings) = state.update(transform)
}
