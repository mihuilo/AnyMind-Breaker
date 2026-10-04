package com.anymindbreaker.feature.statistics

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anymindbreaker.core.common.game.Difficulty
import com.anymindbreaker.core.common.game.GameResult
import com.anymindbreaker.core.common.game.GameSession
import com.anymindbreaker.core.common.game.GameType
import com.anymindbreaker.core.common.game.InMemoryGameRepository
import com.anymindbreaker.core.common.game.Language
import com.anymindbreaker.core.ui.theme.AnyMindBreakerTheme
import com.anymindbreaker.feature.statistics.presentation.StatisticsScreen
import com.anymindbreaker.feature.statistics.presentation.StatisticsViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatisticsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val repository = InMemoryGameRepository()

    private fun show() {
        val viewModel = StatisticsViewModel(repository)
        composeRule.setContent {
            AnyMindBreakerTheme {
                StatisticsScreen(viewModel = viewModel)
            }
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("statistics_card_wins").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun card(name: String) = composeRule.onNodeWithTag("statistics_card_$name", useUnmergedTree = false)

    @Test
    fun emptyStatisticsShowZerosAndAnEmptyHistory() {
        show()
        card("wins").assertTextContains("0")
        card("best_time").assertTextContains("—")

        composeRule.onNodeWithTag("statistics_tab_HISTORY").performClick()
        composeRule.onNodeWithText("No games played yet").assertIsDisplayed()
    }

    @Test
    fun finishedGameAppearsUnderItsGameAndDifficulty() {
        runBlocking {
            repository.finishSession(
                GameSession(
                    sessionId = "s1",
                    gameType = GameType.CRYPTOGRAM,
                    puzzleId = "p",
                    language = Language.EN,
                    difficulty = Difficulty.HARD,
                    startedAt = 0,
                    finishedAt = System.currentTimeMillis(),
                    durationSeconds = 284,
                    mistakes = 0,
                    hintsUsed = 1,
                    entries = 20,
                    score = 840,
                    result = GameResult.COMPLETED,
                ),
            )
        }
        show()

        composeRule.onNodeWithTag("statistics_tab_CRYPTOGRAM").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("statistics_language_ALL").fetchSemanticsNodes().isNotEmpty()
        }
        // One win this week, so the gain is shown next to the total.
        card("wins").assertTextContains("1")
        card("wins").assertTextContains("+1")
        card("flawless").assertTextContains("1")
        card("streak").assertTextContains("1")
        card("best_time").assertTextContains("04:44")
        composeRule.onNodeWithTag("statistics_details").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("840").assertIsDisplayed()

        // The game was played on Hard, so Easy has nothing.
        composeRule.onNodeWithTag("statistics_difficulty_EASY").performClick()
        composeRule.waitUntil(5_000) {
            runCatching { card("best_time").assertTextContains("—") }.isSuccess
        }
        card("wins").assertTextContains("0")

        // Sudoku has no puzzle language to choose.
        composeRule.onNodeWithTag("statistics_tab_SUDOKU").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("statistics_language_ALL").fetchSemanticsNodes().isEmpty()
        }
        card("wins").assertTextContains("0")
    }

    @Test
    fun historyAndAchievementsStayAvailable() {
        runBlocking {
            repository.finishSession(
                GameSession(
                    sessionId = "s1",
                    gameType = GameType.CRYPTOGRAM,
                    puzzleId = "p",
                    language = Language.EN,
                    difficulty = Difficulty.HARD,
                    startedAt = 0,
                    finishedAt = 1_000,
                    durationSeconds = 284,
                    mistakes = 2,
                    hintsUsed = 1,
                    entries = 20,
                    score = 840,
                    result = GameResult.COMPLETED,
                ),
            )
        }
        show()

        composeRule.onNodeWithTag("statistics_tab_HISTORY").performClick()
        composeRule.onNodeWithText("04:44 · score: 840 · mistakes: 2 · hints: 1").assertIsDisplayed()

        composeRule.onNodeWithTag("statistics_tab_ACHIEVEMENTS").performClick()
        composeRule.onNodeWithText("First win").assertIsDisplayed()
    }
}
