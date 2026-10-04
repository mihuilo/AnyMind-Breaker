package com.anymindbreaker.core.database

import com.anymindbreaker.core.common.game.Difficulty
import com.anymindbreaker.core.common.game.GameRepository
import com.anymindbreaker.core.common.game.GameResult
import com.anymindbreaker.core.common.game.GameSession
import com.anymindbreaker.core.common.game.GameType
import com.anymindbreaker.core.common.game.Language
import com.anymindbreaker.core.common.game.SavedGame
import com.anymindbreaker.core.common.game.UserStatistics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RoomGameRepository(private val dao: GameDao) : GameRepository {

    override suspend fun saveProgress(session: GameSession, payload: String, updatedAt: Long) {
        dao.saveProgress(
            session = session.toEntity(),
            savedGame = SavedGameEntity(session.gameType.name, session.sessionId, payload, updatedAt),
        )
    }

    override suspend fun finishSession(session: GameSession) {
        val statistics = (dao.statistics()?.toModel() ?: UserStatistics()).afterSession(session)
        dao.finishSession(session.toEntity(), statistics.toEntity())
    }

    override suspend fun savedGame(gameType: GameType): SavedGame? {
        val saved = dao.savedGame(gameType.name) ?: return null
        val session = dao.session(saved.sessionId) ?: return null
        return SavedGame(session.toModel(), saved.payload, saved.updatedAt)
    }

    override fun observeSavedGames(): Flow<List<SavedGame>> =
        combine(dao.observeSavedGames(), dao.observeSavedSessions()) { savedGames, sessions ->
            val byId = sessions.associateBy { it.sessionId }
            savedGames.mapNotNull { saved ->
                byId[saved.sessionId]?.let { SavedGame(it.toModel(), saved.payload, saved.updatedAt) }
            }
        }

    override fun observeFinishedSessions(): Flow<List<GameSession>> =
        dao.observeFinishedSessions().map { sessions -> sessions.map { it.toModel() } }

    override fun observeStatistics(): Flow<UserStatistics> =
        dao.observeStatistics().map { it?.toModel() ?: UserStatistics() }

    override suspend fun resetProgress() = dao.deleteEverything()
}

private fun GameSession.toEntity() = GameSessionEntity(
    sessionId = sessionId,
    gameType = gameType.name,
    puzzleId = puzzleId,
    language = language?.name,
    difficulty = difficulty.name,
    startedAt = startedAt,
    finishedAt = finishedAt,
    durationSeconds = durationSeconds,
    mistakes = mistakes,
    hintsUsed = hintsUsed,
    entries = entries,
    score = score,
    result = result.name,
    livesEnabled = livesEnabled,
)

private fun GameSessionEntity.toModel() = GameSession(
    sessionId = sessionId,
    gameType = GameType.valueOf(gameType),
    puzzleId = puzzleId,
    language = language?.let(Language::valueOf),
    difficulty = Difficulty.valueOf(difficulty),
    startedAt = startedAt,
    finishedAt = finishedAt,
    durationSeconds = durationSeconds,
    mistakes = mistakes,
    hintsUsed = hintsUsed,
    entries = entries,
    score = score,
    result = GameResult.valueOf(result),
    livesEnabled = livesEnabled,
)

private fun UserStatistics.toEntity() = UserStatisticsEntity(
    totalGames = totalGames,
    completedGames = completedGames,
    failedGames = failedGames,
    totalScore = totalScore,
    totalMistakes = totalMistakes,
    totalHints = totalHints,
    totalPlayTimeSeconds = totalPlayTimeSeconds,
    currentStreak = currentStreak,
    bestStreak = bestStreak,
)

private fun UserStatisticsEntity.toModel() = UserStatistics(
    totalGames = totalGames,
    completedGames = completedGames,
    failedGames = failedGames,
    totalScore = totalScore,
    totalMistakes = totalMistakes,
    totalHints = totalHints,
    totalPlayTimeSeconds = totalPlayTimeSeconds,
    currentStreak = currentStreak,
    bestStreak = bestStreak,
)
