package com.anymindbreaker.core.common.game

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** An unfinished game: its session and the serialized puzzle and state needed to continue it. */
data class SavedGame(
    val session: GameSession,
    val payload: String,
    val updatedAt: Long,
)

/** Aggregated totals. Detailed statistics are computed from the sessions themselves. */
data class UserStatistics(
    val totalGames: Int = 0,
    val completedGames: Int = 0,
    val failedGames: Int = 0,
    val totalScore: Long = 0,
    val totalMistakes: Int = 0,
    val totalHints: Int = 0,
    val totalPlayTimeSeconds: Long = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
) {
    /** Totals after one more finished session. A win extends the streak and a loss resets it. */
    fun afterSession(session: GameSession): UserStatistics {
        val streak = when (session.result) {
            GameResult.COMPLETED -> currentStreak + 1
            GameResult.FAILED -> 0
            GameResult.ABANDONED, GameResult.IN_PROGRESS -> currentStreak
        }
        return copy(
            totalGames = totalGames + 1,
            completedGames = completedGames + if (session.result == GameResult.COMPLETED) 1 else 0,
            failedGames = failedGames + if (session.result == GameResult.FAILED) 1 else 0,
            totalScore = totalScore + session.score,
            totalMistakes = totalMistakes + session.mistakes,
            totalHints = totalHints + session.hintsUsed,
            totalPlayTimeSeconds = totalPlayTimeSeconds + session.durationSeconds,
            currentStreak = streak,
            bestStreak = maxOf(bestStreak, streak),
        )
    }
}

interface GameRepository {
    /** Stores the current progress of an unfinished game, replacing the previous save of that game type. */
    suspend fun saveProgress(session: GameSession, payload: String, updatedAt: Long)

    /** Records the final result, removes the save of that game type and updates the totals. */
    suspend fun finishSession(session: GameSession)

    suspend fun savedGame(gameType: GameType): SavedGame?

    /** Unfinished games, most recently played first. */
    fun observeSavedGames(): Flow<List<SavedGame>>

    /** Finished sessions, newest first. */
    fun observeFinishedSessions(): Flow<List<GameSession>>

    fun observeStatistics(): Flow<UserStatistics>

    suspend fun resetProgress()
}

/** Keeps everything in memory. Used in tests and previews instead of the database. */
class InMemoryGameRepository : GameRepository {

    private data class Data(
        val sessions: Map<String, GameSession> = emptyMap(),
        val saved: Map<GameType, SavedGame> = emptyMap(),
        val statistics: UserStatistics = UserStatistics(),
    )

    private val data = MutableStateFlow(Data())

    override suspend fun saveProgress(session: GameSession, payload: String, updatedAt: Long) {
        data.update {
            it.copy(
                sessions = it.sessions + (session.sessionId to session),
                saved = it.saved + (session.gameType to SavedGame(session, payload, updatedAt)),
            )
        }
    }

    override suspend fun finishSession(session: GameSession) {
        data.update {
            it.copy(
                sessions = it.sessions + (session.sessionId to session),
                saved = it.saved.filterValues { saved -> saved.session.sessionId != session.sessionId },
                statistics = it.statistics.afterSession(session),
            )
        }
    }

    override suspend fun savedGame(gameType: GameType): SavedGame? = data.value.saved[gameType]

    override fun observeSavedGames(): Flow<List<SavedGame>> =
        data.map { it.saved.values.sortedByDescending { saved -> saved.updatedAt } }

    override fun observeFinishedSessions(): Flow<List<GameSession>> = data.map {
        it.sessions.values
            .filter { session -> session.result != GameResult.IN_PROGRESS }
            .sortedByDescending { session -> session.finishedAt }
    }

    override fun observeStatistics(): Flow<UserStatistics> = data.map { it.statistics }

    override suspend fun resetProgress() {
        data.value = Data()
    }
}
