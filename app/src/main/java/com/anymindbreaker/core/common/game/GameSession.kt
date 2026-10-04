package com.anymindbreaker.core.common.game

/** One attempt at a puzzle. Sessions are the source of truth for detailed statistics. */
data class GameSession(
    val sessionId: String,
    val gameType: GameType,
    val puzzleId: String,
    val language: Language?,
    val difficulty: Difficulty,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val durationSeconds: Long = 0,
    val mistakes: Int = 0,
    val hintsUsed: Int = 0,
    /** Number of answers the player entered; not in the original spec, needed for accuracy. */
    val entries: Int = 0,
    /** False when mistakes were not limited; such a game resets the win streak. */
    val livesEnabled: Boolean = true,
    val score: Int = 0,
    val result: GameResult = GameResult.IN_PROGRESS,
)
