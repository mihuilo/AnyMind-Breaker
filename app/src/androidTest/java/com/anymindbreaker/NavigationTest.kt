package com.anymindbreaker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anymindbreaker.app.AnyMindBreakerApplication
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Moves between the real screens of the app, with its real database and settings. */
@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearProgress() {
        // An unfinished game left on the device would put a "continue?" dialog in the way.
        val application = ApplicationProvider.getApplicationContext<AnyMindBreakerApplication>()
        runBlocking { application.container.gameRepository.resetProgress() }
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun bottomBarOpensEveryTopLevelScreen() {
        composeRule.onNodeWithTag("nav_GAMES").performClick()
        composeRule.onNodeWithTag("game_card_CRYPTOGRAM").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_STATISTICS").performClick()
        composeRule.onNodeWithTag("statistics_tab_SUDOKU").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_SETTINGS").performClick()
        waitForTag("settings_sound")

        composeRule.onNodeWithTag("nav_HOME").performClick()
        composeRule.onNodeWithTag("game_card_SUDOKU").assertIsDisplayed()
    }

    @Test
    fun gameOpensFromHomeHidesBottomBarAndReturnsBack() {
        composeRule.onNodeWithTag("game_card_SUDOKU").performScrollTo().performClick()
        waitForTag("sudoku_start")
        // Game screens take the whole screen.
        composeRule.onNodeWithTag("nav_HOME").assertDoesNotExist()

        composeRule.onNodeWithTag("game_back").performClick()
        waitForTag("nav_HOME")
        composeRule.onNodeWithTag("game_card_SUDOKU").assertIsDisplayed()
    }

    @Test
    fun cryptogramOpensFromGamesTab() {
        composeRule.onNodeWithTag("nav_GAMES").performClick()
        composeRule.onNodeWithTag("game_card_CRYPTOGRAM").performClick()
        waitForTag("crypto_start")
        composeRule.onNodeWithTag("crypto_start").performScrollTo().assertIsDisplayed()
    }
}
