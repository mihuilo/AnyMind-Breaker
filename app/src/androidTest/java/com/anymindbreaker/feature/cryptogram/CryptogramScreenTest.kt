package com.anymindbreaker.feature.cryptogram

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anymindbreaker.core.common.game.DefaultScoreCalculator
import com.anymindbreaker.core.common.game.Difficulty
import com.anymindbreaker.core.common.game.Language
import com.anymindbreaker.core.ui.theme.AnyMindBreakerTheme
import com.anymindbreaker.feature.cryptogram.data.CryptogramText
import com.anymindbreaker.feature.cryptogram.data.CryptogramTextSource
import com.anymindbreaker.feature.cryptogram.domain.CryptogramAlphabet
import com.anymindbreaker.feature.cryptogram.domain.CryptogramGenerator
import com.anymindbreaker.feature.cryptogram.presentation.CryptogramScreen
import com.anymindbreaker.feature.cryptogram.presentation.CryptogramViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class CryptogramScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val phrase = CryptogramText("test", Difficulty.EASY, "Practice makes perfect.")

    private val textSource = object : CryptogramTextSource {
        override suspend fun texts(language: Language) = listOf(phrase)
    }

    @Test
    fun startingAndSolvingPuzzleShowsResult() {
        val seed = 1
        // The same seed gives the screen the same key, so the test knows the answers.
        val puzzle = CryptogramGenerator(Random(seed))
            .generate(phrase.id, phrase.text, Language.EN, phrase.difficulty)
        val viewModel = CryptogramViewModel(
            textSource = textSource,
            scoreCalculator = DefaultScoreCalculator(),
            generator = CryptogramGenerator(Random(seed)),
        )

        composeRule.setContent {
            AnyMindBreakerTheme {
                CryptogramScreen(onBack = {}, viewModel = viewModel)
            }
        }

        composeRule.onNodeWithTag("crypto_start").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("crypto_cell_0").fetchSemanticsNodes().isNotEmpty()
        }

        val alphabet = CryptogramAlphabet.of(Language.EN)
        val hinted = puzzle.hints.map { it.cipher }.toSet()
        val solved = mutableSetOf<Char>()
        for (index in puzzle.cipherText.indices) {
            val cipher = puzzle.cipherText[index]
            if (cipher !in alphabet || cipher in hinted || !solved.add(cipher)) continue
            composeRule.onNodeWithTag("crypto_cell_$index").performScrollTo().performClick()
            composeRule.onNodeWithTag("crypto_key_${puzzle.text[index]}").performClick()
        }

        composeRule.onNodeWithTag("crypto_result").assertIsDisplayed()
    }
}
