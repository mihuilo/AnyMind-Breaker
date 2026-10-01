package com.anymindbreaker.feature.statistics.domain

import com.anymindbreaker.core.common.game.GameResult
import com.anymindbreaker.core.common.game.GameSession
import com.anymindbreaker.core.common.game.GameType
import com.anymindbreaker.core.common.game.Language
import com.anymindbreaker.core.common.game.UserStatistics

/** Measurable facts about a set of finished games. Interpretation is left to the player. */
data class GameSummary(
    val played: Int = 0,
    val completed: Int = 0,
    val failed: Int = 0,
    /** Share of played games that were solved, or null when nothing was played. */
    val successPercent: Int? = null,
    /** Average and best time of solved games, in seconds. */
    val averageSeconds: Long? = null,
    val bestSeconds: Long? = null,
    val mistakes: Int = 0,
    val hints: Int = 0,
    val score: Long = 0,
    /** Share of correct entries, or null when nothing was entered. */
    val accuracyPercent: Int? = null,
)

data class StatisticsOverview(
    val overall: GameSummary = GameSummary(),
    val byGame: Map<GameType, GameSummary> = emptyMap(),
    val byLanguage: Map<Language, GameSummary> = emptyMap(),
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
)

data class TodaySummary(
    val playSeconds: Long = 0,
    val accuracyPercent: Int? = null,
)

enum class Achievement {
    FIRST_WIN,
    SOLVED_10,
    SOLVED_100,
    FLAWLESS_10,
    SUDOKU_STREAK_10,
    CRYPTOGRAM_NO_HINTS,
    UNDER_A_MINUTE,
}

object StatisticsCalculator {

    fun summarize(sessions: List<GameSession>): GameSummary {
        val solved = sessions.filter { it.result == GameResult.COMPLETED }
        return GameSummary(
            played = sessions.size,
            completed = solved.size,
            failed = sessions.count { it.result == GameResult.FAILED },
            successPercent = if (sessions.isEmpty()) null else solved.size * 100 / sessions.size,
            averageSeconds = if (solved.isEmpty()) null else solved.sumOf { it.durationSeconds } / solved.size,
            bestSeconds = solved.minOfOrNull { it.durationSeconds },
            mistakes = sessions.sumOf { it.mistakes },
            hints = sessions.sumOf { it.hintsUsed },
            score = sessions.sumOf { it.score.toLong() },
            accuracyPercent = accuracy(sessions),
        )
    }

    /** [sessions] are finished sessions; streaks come from the aggregated totals. */
    fun overview(sessions: List<GameSession>, totals: UserStatistics): StatisticsOverview = StatisticsOverview(
        overall = summarize(sessions),
        byGame = GameType.entries.associateWith { type -> summarize(sessions.filter { it.gameType == type }) },
        byLanguage = Language.entries.associateWith { language -> summarize(sessions.filter { it.language == language }) },
        currentStreak = totals.currentStreak,
        bestStreak = totals.bestStreak,
    )

    fun today(sessions: List<GameSession>, startOfDayMillis: Long): TodaySummary {
        val today = sessions.filter { (it.finishedAt ?: 0) >= startOfDayMillis }
        return TodaySummary(
            playSeconds = today.sumOf { it.durationSeconds },
            accuracyPercent = accuracy(today),
        )
    }

    fun achievements(sessions: List<GameSession>): Set<Achievement> {
        val solved = sessions.filter { it.result == GameResult.COMPLETED }
        val unlocked = mutableSetOf<Achievement>()
        if (solved.isNotEmpty()) unlocked += Achievement.FIRST_WIN
        if (solved.size >= 10) unlocked += Achievement.SOLVED_10
        if (solved.size >= 100) unlocked += Achievement.SOLVED_100
        if (solved.count { it.mistakes == 0 } >= 10) unlocked += Achievement.FLAWLESS_10
        if (longestSudokuStreak(sessions) >= 10) unlocked += Achievement.SUDOKU_STREAK_10
        if (solved.any { it.gameType == GameType.CRYPTOGRAM && it.hintsUsed == 0 }) {
            unlocked += Achievement.CRYPTOGRAM_NO_HINTS
        }
        if (solved.any { it.durationSeconds < 60 }) unlocked += Achievement.UNDER_A_MINUTE
        return unlocked
    }

    private fun accuracy(sessions: List<GameSession>): Int? {
        val entries = sessions.sumOf { it.entries }
        if (entries == 0) return null
        return (entries - sessions.sumOf { it.mistakes }).coerceAtLeast(0) * 100 / entries
    }

    /** Longest run of solved sudoku games; an abandoned game does not break the run, a lost one does. */
    private fun longestSudokuStreak(sessions: List<GameSession>): Int {
        var best = 0
        var current = 0
        for (session in sessions.filter { it.gameType == GameType.SUDOKU }.sortedBy { it.finishedAt ?: 0 }) {
            when (session.result) {
                GameResult.COMPLETED -> current++
                GameResult.FAILED -> current = 0
                GameResult.ABANDONED, GameResult.IN_PROGRESS -> Unit
            }
            best = maxOf(best, current)
        }
        return best
    }
}
