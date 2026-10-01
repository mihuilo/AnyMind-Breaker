package com.anymindbreaker.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anymindbreaker.core.common.game.Difficulty
import com.anymindbreaker.core.common.game.GameSession
import com.anymindbreaker.core.common.game.GameType
import com.anymindbreaker.core.common.game.InMemoryGameRepository
import com.anymindbreaker.core.common.game.Language
import com.anymindbreaker.core.datastore.AppSettings
import com.anymindbreaker.core.datastore.InMemorySettingsRepository
import com.anymindbreaker.core.datastore.ThemeMode
import com.anymindbreaker.core.ui.theme.AnyMindBreakerTheme
import com.anymindbreaker.feature.settings.presentation.SettingsScreen
import com.anymindbreaker.feature.settings.presentation.SettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val settings = InMemorySettingsRepository(
        AppSettings(uiLanguage = Language.EN, contentLanguage = Language.EN),
    )
    private val games = InMemoryGameRepository()

    private fun current() = runBlocking { settings.settings.first() }

    @Test
    fun changingOptionsUpdatesStoredSettings() {
        val viewModel = SettingsViewModel(settings, games)
        composeRule.setContent {
            AnyMindBreakerTheme {
                SettingsScreen(viewModel = viewModel)
            }
        }

        composeRule.onNodeWithTag("settings_sound").performScrollTo().performClick()
        composeRule.waitUntil(5_000) { !current().soundEnabled }
        assertFalse(current().soundEnabled)

        composeRule.onNodeWithText("Dark").performScrollTo().performClick()
        composeRule.waitUntil(5_000) { current().theme == ThemeMode.DARK }

        // The interface language group comes first, so the first "Русский" belongs to it.
        composeRule.onNodeWithTag("settings_ui_language").assertIsDisplayed()
        assertEquals(Language.EN, current().contentLanguage)
    }

    @Test
    fun resetAsksForConfirmationBeforeDeleting() {
        runBlocking {
            games.saveProgress(
                GameSession("s", GameType.SUDOKU, "p", null, Difficulty.EASY, startedAt = 0),
                payload = "payload",
                updatedAt = 1,
            )
        }
        val viewModel = SettingsViewModel(settings, games)
        composeRule.setContent {
            AnyMindBreakerTheme {
                SettingsScreen(viewModel = viewModel)
            }
        }

        composeRule.onNodeWithTag("settings_reset").performScrollTo().performClick()
        // Nothing is deleted until the dialog is confirmed.
        assertEquals("payload", runBlocking { games.savedGame(GameType.SUDOKU) }?.payload)

        composeRule.onNodeWithTag("settings_reset_confirm").performClick()
        composeRule.waitUntil(5_000) { runBlocking { games.savedGame(GameType.SUDOKU) } == null }
        assertNull(runBlocking { games.savedGame(GameType.SUDOKU) })
    }
}
