package com.anymindbreaker.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.anymindbreaker.core.common.game.DefaultScoreCalculator
import com.anymindbreaker.core.common.game.Difficulty
import com.anymindbreaker.core.common.game.GameResult
import com.anymindbreaker.core.common.game.GameSession
import com.anymindbreaker.core.common.game.GameType
import com.anymindbreaker.core.common.game.InMemoryGameRepository
import com.anymindbreaker.core.common.game.Language
import com.anymindbreaker.feature.cryptogram.data.CryptogramText
import com.anymindbreaker.feature.cryptogram.data.CryptogramTextSource
import com.anymindbreaker.feature.cryptogram.presentation.CryptogramViewModel
import com.anymindbreaker.feature.settings.presentation.SettingsViewModel
import com.anymindbreaker.feature.sudoku.presentation.SudokuViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun repository(scope: CoroutineScope, defaultLanguage: Language = Language.RU) =
        DataStoreSettingsRepository(
            dataStore = PreferenceDataStoreFactory.create(scope = scope) {
                folder.root.resolve("settings.preferences_pb")
            },
            defaultLanguage = defaultLanguage,
        )

    @Test
    fun defaultsFollowTheDefaultLanguage() = runTest(dispatcher) {
        val settings = repository(backgroundScope, Language.RU).settings.first()
        assertEquals(
            AppSettings(uiLanguage = Language.RU, contentLanguage = Language.RU),
            settings,
        )
        assertEquals(ThemeMode.SYSTEM, settings.theme)
        assertTrue(settings.soundEnabled && settings.vibrationEnabled && settings.instantSudokuValidation)
    }

    @Test
    fun interfaceAndContentLanguagesAreIndependent() = runTest(dispatcher) {
        val repository = repository(backgroundScope)
        repository.update { it.copy(contentLanguage = Language.EN) }
        assertEquals(Language.RU, repository.settings.first().uiLanguage)
        assertEquals(Language.EN, repository.settings.first().contentLanguage)

        repository.update { it.copy(uiLanguage = Language.EN, contentLanguage = Language.RU) }
        assertEquals(Language.EN, repository.settings.first().uiLanguage)
        assertEquals(Language.RU, repository.settings.first().contentLanguage)
    }

    @Test
    fun changesSurviveReopeningTheStore() = runTest(dispatcher) {
        val firstJob = Job()
        val changed = AppSettings(
            uiLanguage = Language.EN,
            contentLanguage = Language.RU,
            theme = ThemeMode.DARK,
            soundEnabled = false,
            vibrationEnabled = false,
            instantSudokuValidation = false,
        )
        repository(CoroutineScope(dispatcher + firstJob)).update { changed }
        // Only one DataStore may be open for a file at a time.
        firstJob.cancelAndJoin()

        assertEquals(changed, repository(backgroundScope).settings.first())
    }

    @Test
    fun settingsViewModelUpdatesSettingsAndResetsProgress() = runTest(dispatcher) {
        val settings = InMemorySettingsRepository()
        val games = InMemoryGameRepository()
        val session = GameSession("s", GameType.SUDOKU, "p", null, Difficulty.EASY, startedAt = 0)
        games.saveProgress(session, "payload", 1)
        games.finishSession(session.copy(sessionId = "done", result = GameResult.COMPLETED, finishedAt = 2))

        val viewModel = SettingsViewModel(settings, games)
        viewModel.update { it.copy(theme = ThemeMode.DARK, soundEnabled = false) }
        assertEquals(ThemeMode.DARK, settings.settings.first().theme)
        assertFalse(settings.settings.first().soundEnabled)

        viewModel.resetProgress()
        assertNull(games.savedGame(GameType.SUDOKU))
        assertTrue(games.observeFinishedSessions().first().isEmpty())
        assertEquals(0, games.observeStatistics().first().totalGames)
        // Resetting progress keeps the settings.
        assertEquals(ThemeMode.DARK, settings.settings.first().theme)
    }

    @Test
    fun gameSetupStartsFromSettingsAndWritesChoicesBack() = runTest(dispatcher) {
        val settings = InMemorySettingsRepository(
            AppSettings(uiLanguage = Language.EN, contentLanguage = Language.RU, instantSudokuValidation = false),
        )
        val scope = CoroutineScope(dispatcher)

        val sudoku = SudokuViewModel(persistenceScope = scope, settings = settings)
        assertFalse(sudoku.uiState.value.instantCheck)
        sudoku.onInstantCheckChanged(true)
        assertTrue(settings.settings.first().instantSudokuValidation)

        val source = object : CryptogramTextSource {
            override suspend fun texts(language: Language) = emptyList<CryptogramText>()
        }
        val cryptogram = CryptogramViewModel(source, DefaultScoreCalculator(), persistenceScope = scope, settings = settings)
        assertEquals(Language.RU, cryptogram.uiState.value.language)
        cryptogram.onLanguageSelected(Language.EN)
        assertEquals(Language.EN, settings.settings.first().contentLanguage)
        assertEquals(Language.EN, settings.settings.first().uiLanguage)
    }
}
