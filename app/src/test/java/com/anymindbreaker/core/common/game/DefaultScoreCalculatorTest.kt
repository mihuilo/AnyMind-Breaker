package com.anymindbreaker.core.common.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultScoreCalculatorTest {

    private val calculator = DefaultScoreCalculator()

    private fun input(
        difficulty: Difficulty = Difficulty.NORMAL,
        result: GameResult = GameResult.COMPLETED,
        durationSeconds: Long = 300,
        targetSeconds: Long = 300,
        mistakes: Int = 0,
        hintsUsed: Int = 0,
        livesLeft: Int? = null,
    ) = ScoreInput(difficulty, result, durationSeconds, targetSeconds, mistakes, hintsUsed, livesLeft)

    @Test
    fun unfinishedGameGivesNoScore() {
        assertEquals(0, calculator.calculate(input(result = GameResult.FAILED)))
        assertEquals(0, calculator.calculate(input(result = GameResult.ABANDONED)))
        assertEquals(0, calculator.calculate(input(result = GameResult.IN_PROGRESS)))
    }

    @Test
    fun cleanGameOnTargetTimeGetsBasePlusCleanBonuses() {
        // 200 base + 40 for no mistakes + 40 for no hints
        assertEquals(280, calculator.calculate(input()))
    }

    @Test
    fun higherDifficultyGivesMorePoints() {
        val scores = Difficulty.entries.map { calculator.calculate(input(difficulty = it)) }
        assertEquals(scores.sorted(), scores)
        assertEquals(scores.size, scores.distinct().size)
    }

    @Test
    fun fasterSolutionGivesMorePoints() {
        val fast = calculator.calculate(input(durationSeconds = 150))
        val onTime = calculator.calculate(input(durationSeconds = 300))
        val slow = calculator.calculate(input(durationSeconds = 450))
        assertTrue(fast > onTime)
        assertTrue(onTime > slow)
    }

    @Test
    fun overtimePenaltyIsCapped() {
        val twiceOver = calculator.calculate(input(durationSeconds = 600))
        val wayOver = calculator.calculate(input(durationSeconds = 60_000))
        assertEquals(twiceOver, wayOver)
    }

    @Test
    fun mistakesAndHintsReduceScore() {
        val clean = calculator.calculate(input())
        // loses the 40 point bonus and 15 per mistake
        assertEquals(clean - 40 - 30, calculator.calculate(input(mistakes = 2)))
        // loses the 40 point bonus and 25 per hint
        assertEquals(clean - 40 - 25, calculator.calculate(input(hintsUsed = 1)))
    }

    @Test
    fun remainingLivesAddBonus() {
        val clean = calculator.calculate(input())
        assertEquals(clean + 30, calculator.calculate(input(livesLeft = 3)))
    }

    @Test
    fun scoreNeverGoesBelowZero() {
        val score = calculator.calculate(
            input(difficulty = Difficulty.EASY, durationSeconds = 6000, mistakes = 50, hintsUsed = 50),
        )
        assertEquals(0, score)
    }

    @Test
    fun missingTargetTimeGivesNoTimeAdjustment() {
        assertEquals(280, calculator.calculate(input(durationSeconds = 9999, targetSeconds = 0)))
    }
}
