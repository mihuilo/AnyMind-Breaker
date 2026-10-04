package com.anymindbreaker.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anymindbreaker.core.common.game.GameResult
import com.anymindbreaker.core.common.game.GameType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Data written by an installed older version must survive an update. */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "migration_test.db"

    @Before
    @After
    fun deleteDatabase() {
        context.deleteDatabase(name)
    }

    /** The tables exactly as version 1 of the app created them. */
    private fun createVersion1Database() {
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name).apply { parentFile?.mkdirs() }, null).use { db ->
            db.execSQL(
                "CREATE TABLE game_session (sessionId TEXT NOT NULL, gameType TEXT NOT NULL, puzzleId TEXT NOT NULL, " +
                    "language TEXT, difficulty TEXT NOT NULL, startedAt INTEGER NOT NULL, finishedAt INTEGER, " +
                    "durationSeconds INTEGER NOT NULL, mistakes INTEGER NOT NULL, hintsUsed INTEGER NOT NULL, " +
                    "entries INTEGER NOT NULL, score INTEGER NOT NULL, result TEXT NOT NULL, PRIMARY KEY(sessionId))",
            )
            db.execSQL(
                "CREATE TABLE saved_game (gameType TEXT NOT NULL, sessionId TEXT NOT NULL, payload TEXT NOT NULL, " +
                    "updatedAt INTEGER NOT NULL, PRIMARY KEY(gameType))",
            )
            db.execSQL(
                "CREATE TABLE user_statistics (id INTEGER NOT NULL, totalGames INTEGER NOT NULL, " +
                    "completedGames INTEGER NOT NULL, failedGames INTEGER NOT NULL, totalScore INTEGER NOT NULL, " +
                    "totalMistakes INTEGER NOT NULL, totalHints INTEGER NOT NULL, totalPlayTimeSeconds INTEGER NOT NULL, " +
                    "currentStreak INTEGER NOT NULL, bestStreak INTEGER NOT NULL, PRIMARY KEY(id))",
            )
            db.execSQL(
                "INSERT INTO game_session VALUES ('old', 'SUDOKU', 'p', NULL, 'HARD', 100, 900, 321, 2, 1, 40, 250, 'COMPLETED')",
            )
            db.execSQL("INSERT INTO user_statistics VALUES (0, 1, 1, 0, 250, 2, 1, 321, 1, 1)")
            db.version = 1
        }
    }

    @Test
    fun version1DataIsKeptAndCountsAsPlayedWithLives() = runBlocking {
        createVersion1Database()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(*AppDatabase.MIGRATIONS)
            .build()
        try {
            val repository = RoomGameRepository(database.gameDao())

            val session = repository.observeFinishedSessions().first().single()
            assertEquals("old", session.sessionId)
            assertEquals(GameType.SUDOKU, session.gameType)
            assertEquals(GameResult.COMPLETED, session.result)
            assertEquals(321, session.durationSeconds)
            assertEquals(250, session.score)
            assertTrue(session.livesEnabled)

            val statistics = repository.observeStatistics().first()
            assertEquals(1, statistics.completedGames)
            assertEquals(1, statistics.bestStreak)

            // New sessions can store the flag.
            repository.finishSession(session.copy(sessionId = "new", livesEnabled = false, finishedAt = 2_000))
            val newest = repository.observeFinishedSessions().first().first()
            assertEquals("new", newest.sessionId)
            assertEquals(false, newest.livesEnabled)
        } finally {
            database.close()
        }
    }
}
