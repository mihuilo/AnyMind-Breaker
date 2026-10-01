package com.anymindbreaker.feature.sudoku

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anymindbreaker.core.common.game.Difficulty
import com.anymindbreaker.core.ui.theme.AnyMindBreakerTheme
import com.anymindbreaker.feature.sudoku.domain.SudokuGenerator
import com.anymindbreaker.feature.sudoku.presentation.SudokuScreen
import com.anymindbreaker.feature.sudoku.presentation.SudokuViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class SudokuScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun startingAndSolvingPuzzleShowsResult() {
        val seed = 1
        // The same seed gives the screen the same puzzle, so the test knows the answers.
        val puzzle = SudokuGenerator(Random(seed)).generate(Difficulty.EASY)
        val viewModel = SudokuViewModel(generator = SudokuGenerator(Random(seed)))

        composeRule.setContent {
            AnyMindBreakerTheme {
                SudokuScreen(onBack = {}, viewModel = viewModel)
            }
        }

        composeRule.onNodeWithTag("sudoku_start").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("sudoku_cell_0").fetchSemanticsNodes().isNotEmpty()
        }

        for (cell in puzzle.givens.indices) {
            if (puzzle.givens[cell] != 0) continue
            composeRule.onNodeWithTag("sudoku_cell_$cell").performClick()
            composeRule.onNodeWithTag("sudoku_digit_${puzzle.solution[cell]}").performClick()
        }

        composeRule.onNodeWithTag("sudoku_result").assertIsDisplayed()
    }
}
