package com.anymindbreaker.core.common.game

import kotlin.math.roundToInt

data class ScoreInput(
    val difficulty: Difficulty,
    val result: GameResult,
    val durationSeconds: Long,
    /** Time the puzzle is expected to take; faster solutions get a bonus, slower ones a penalty. */
    val targetSeconds: Long,
    val mistakes: Int,
    val hintsUsed: Int,
    val livesLeft: Int? = null,
)

/** Game balance lives here so it can change without touching game screens. */
interface ScoreCalculator {
    fun calculate(input: ScoreInput): Int
}

class DefaultScoreCalculator : ScoreCalculator {

    override fun calculate(input: ScoreInput): Int {
        if (input.result != GameResult.COMPLETED) return 0

        val base = input.difficulty.level * BASE_PER_LEVEL
        var score = base.toDouble()

        score += timeAdjustment(base, input.durationSeconds, input.targetSeconds)
        if (input.mistakes == 0) score += base * NO_MISTAKES_BONUS
        if (input.hintsUsed == 0) score += base * NO_HINTS_BONUS
        score -= input.mistakes * MISTAKE_PENALTY
        score -= input.hintsUsed * HINT_PENALTY
        score += (input.livesLeft ?: 0) * LIFE_BONUS

        return score.roundToInt().coerceAtLeast(0)
    }

    private fun timeAdjustment(base: Int, durationSeconds: Long, targetSeconds: Long): Double {
        if (targetSeconds <= 0) return 0.0
        val ratio = durationSeconds.toDouble() / targetSeconds
        return if (ratio <= 1.0) {
            base * MAX_SPEED_BONUS * (1.0 - ratio)
        } else {
            -base * MAX_OVERTIME_PENALTY * (ratio - 1.0).coerceAtMost(1.0)
        }
    }

    private companion object {
        const val BASE_PER_LEVEL = 100
        const val MAX_SPEED_BONUS = 0.5
        const val MAX_OVERTIME_PENALTY = 0.3
        const val NO_MISTAKES_BONUS = 0.2
        const val NO_HINTS_BONUS = 0.2
        const val MISTAKE_PENALTY = 15
        const val HINT_PENALTY = 25
        const val LIFE_BONUS = 10
    }
}
