package com.anymindbreaker.feature.statistics

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
    }

    @Test
    fun emptyHistoryShowsPlaceholder() {
        show()
        composeRule.onNodeWithText("No games played yet").assertIsDisplayed()
    }

    @Test
    fun finishedGameAppearsInStatisticsAndHistory() {
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

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("statistics_general").fetchSemanticsNodes().isNotEmpty()
        }
        // With a single solved game the average and the best time are the same.
        composeRule.onAllNodesWithText("04:44").assertCountEquals(2)
        composeRule.onNodeWithText("840").assertIsDisplayed()

        composeRule.onNodeWithTag("statistics_tab_HISTORY").performClick()
        composeRule.onNodeWithText("Cryptogram").assertIsDisplayed()
        composeRule.onNodeWithText("04:44 · score: 840 · mistakes: 2 · hints: 1").assertIsDisplayed()

        composeRule.onNodeWithTag("statistics_tab_ACHIEVEMENTS").performClick()
        composeRule.onNodeWithText("First win").assertIsDisplayed()
    }
}
