package com.anymindbreaker.core.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/** Enum values are stored by name so the tables stay readable and need no converters. */
@Entity(tableName = "game_session")
data class GameSessionEntity(
    @PrimaryKey val sessionId: String,
    val gameType: String,
    val puzzleId: String,
    val language: String?,
    val difficulty: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val durationSeconds: Long,
    val mistakes: Int,
    val hintsUsed: Int,
    val entries: Int,
    val score: Int,
    val result: String,
    @ColumnInfo(defaultValue = "1") val livesEnabled: Boolean = true,
)

/** At most one unfinished game per game type. */
@Entity(tableName = "saved_game")
data class SavedGameEntity(
    @PrimaryKey val gameType: String,
    val sessionId: String,
    val payload: String,
    val updatedAt: Long,
)

@Entity(tableName = "user_statistics")
data class UserStatisticsEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val totalGames: Int,
    val completedGames: Int,
    val failedGames: Int,
    val totalScore: Long,
    val totalMistakes: Int,
    val totalHints: Int,
    val totalPlayTimeSeconds: Long,
    val currentStreak: Int,
    val bestStreak: Int,
) {
    companion object {
        const val SINGLE_ROW_ID = 0
    }
}

@Dao
interface GameDao {
    @Upsert
    suspend fun upsertSession(session: GameSessionEntity)

    @Query("SELECT * FROM game_session WHERE sessionId = :sessionId")
    suspend fun session(sessionId: String): GameSessionEntity?

    @Query("SELECT * FROM game_session WHERE result != 'IN_PROGRESS' ORDER BY finishedAt DESC")
    fun observeFinishedSessions(): Flow<List<GameSessionEntity>>

    @Query("SELECT * FROM game_session WHERE sessionId IN (SELECT sessionId FROM saved_game)")
    fun observeSavedSessions(): Flow<List<GameSessionEntity>>

    @Upsert
    suspend fun upsertSavedGame(savedGame: SavedGameEntity)

    @Query("SELECT * FROM saved_game WHERE gameType = :gameType")
    suspend fun savedGame(gameType: String): SavedGameEntity?

    @Query("SELECT * FROM saved_game ORDER BY updatedAt DESC")
    fun observeSavedGames(): Flow<List<SavedGameEntity>>

    @Query("DELETE FROM saved_game WHERE sessionId = :sessionId")
    suspend fun deleteSavedGame(sessionId: String)

    @Query("SELECT * FROM user_statistics WHERE id = 0")
    suspend fun statistics(): UserStatisticsEntity?

    @Query("SELECT * FROM user_statistics WHERE id = 0")
    fun observeStatistics(): Flow<UserStatisticsEntity?>

    @Upsert
    suspend fun upsertStatistics(statistics: UserStatisticsEntity)

    @Query("DELETE FROM game_session")
    suspend fun deleteSessions()

    @Query("DELETE FROM saved_game")
    suspend fun deleteSavedGames()

    @Query("DELETE FROM user_statistics")
    suspend fun deleteStatistics()

    @Transaction
    suspend fun saveProgress(session: GameSessionEntity, savedGame: SavedGameEntity) {
        upsertSession(session)
        upsertSavedGame(savedGame)
    }

    @Transaction
    suspend fun finishSession(session: GameSessionEntity, statistics: UserStatisticsEntity) {
        upsertSession(session)
        deleteSavedGame(session.sessionId)
        upsertStatistics(statistics)
    }

    @Transaction
    suspend fun deleteEverything() {
        deleteSavedGames()
        deleteSessions()
        deleteStatistics()
    }
}

@Database(
    entities = [GameSessionEntity::class, SavedGameEntity::class, UserStatisticsEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        /** Adds the "played with lives" flag; games recorded before it existed count as played with lives. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE game_session ADD COLUMN livesEnabled INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATIONS = arrayOf(MIGRATION_1_2)
    }
}
