package com.anymindbreaker.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anymindbreaker.core.common.game.Difficulty
import com.anymindbreaker.core.common.game.GameResult
import com.anymindbreaker.core.common.game.GameSession
import com.anymindbreaker.core.common.game.GameType
import com.anymindbreaker.core.common.game.Language
import com.anymindbreaker.core.common.game.UserStatistics
import com.anymindbreaker.feature.sudoku.domain.SudokuAction
import com.anymindbreaker.feature.sudoku.domain.SudokuGenerator
import com.anymindbreaker.feature.sudoku.presentation.SudokuViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/** Game → Repository → Room, against a real (in-memory) database. */
@RunWith(AndroidJUnit4::class)
class RoomGameRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: RoomGameRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .build()
        repository = RoomGameRepository(database.gameDao())
    }

    @After
    fun tearDown() = database.close()

    private fun session(id: String, type: GameType = GameType.CRYPTOGRAM) = GameSession(
        sessionId = id,
        gameType = type,
        puzzleId = "puzzle_$id",
        language = if (type == GameType.CRYPTOGRAM) Language.EN else null,
        difficulty = Difficulty.HARD,
        startedAt = 1_000,
        durationSeconds = 284,
        mistakes = 2,
        hintsUsed = 1,
        entries = 20,
    )

    @Test
    fun savedGameRoundTripsThroughDatabase() = runBlocking {
        val original = session("a")
        repository.saveProgress(original, payload = "{\"k\":1}", updatedAt = 5_000)

        val saved = checkNotNull(repository.savedGame(GameType.CRYPTOGRAM))
        assertEquals(original, saved.session)
        assertEquals("{\"k\":1}", saved.payload)
        assertEquals(5_000, saved.updatedAt)
        assertNull(repository.savedGame(GameType.SUDOKU))
        assertEquals(emptyList<GameSession>(), repository.observeFinishedSessions().first())
    }

    @Test
    fun newSaveReplacesPreviousOneOfSameGame() = runBlocking {
        repository.saveProgress(session("a"), "first", 1)
        repository.saveProgress(session("a").copy(mistakes = 5), "second", 2)

        val saved = checkNotNull(repository.savedGame(GameType.CRYPTOGRAM))
        assertEquals("second", saved.payload)
        assertEquals(5, saved.session.mistakes)
        assertEquals(1, repository.observeSavedGames().first().size)
    }

    @Test
    fun savedGamesAreOrderedByLastPlayed() = runBlocking {
        repository.saveProgress(session("a", GameType.CRYPTOGRAM), "c", updatedAt = 10)
        repository.saveProgress(session("b", GameType.SUDOKU), "s", updatedAt = 20)

        assertEquals(
            listOf(GameType.SUDOKU, GameType.CRYPTOGRAM),
            repository.observeSavedGames().first().map { it.session.gameType },
        )
    }

    @Test
    fun finishingSessionRemovesSaveAndUpdatesStatistics() = runBlocking {
        repository.saveProgress(session("a"), "payload", 1)
        val finished = session("a").copy(result = GameResult.COMPLETED, score = 840, finishedAt = 9_000)
        repository.finishSession(finished)

        assertNull(repository.savedGame(GameType.CRYPTOGRAM))
        assertEquals(listOf(finished), repository.observeFinishedSessions().first())
        assertEquals(
            UserStatistics(
                totalGames = 1,
                completedGames = 1,
                totalScore = 840,
                totalMistakes = 2,
                totalHints = 1,
                totalPlayTimeSeconds = 284,
                currentStreak = 1,
                bestStreak = 1,
            ),
            repository.observeStatistics().first(),
        )
    }

    @Test
    fun finishedSessionsAreNewestFirst() = runBlocking {
        repository.finishSession(session("old").copy(result = GameResult.FAILED, finishedAt = 100))
        repository.finishSession(session("new").copy(result = GameResult.COMPLETED, finishedAt = 200))

        assertEquals(listOf("new", "old"), repository.observeFinishedSessions().first().map { it.sessionId })
        assertEquals(1, repository.observeStatistics().first().currentStreak)
    }

    @Test
    fun resetRemovesEverything() = runBlocking {
        repository.saveProgress(session("a"), "payload", 1)
        repository.finishSession(session("b").copy(result = GameResult.COMPLETED, finishedAt = 1))
        repository.resetProgress()

        assertNull(repository.savedGame(GameType.CRYPTOGRAM))
        assertEquals(emptyList<GameSession>(), repository.observeFinishedSessions().first())
        assertEquals(UserStatistics(), repository.observeStatistics().first())
    }

    @Test
    fun gameStartedInViewModelCanBeContinuedFromDatabase() = runBlocking {
        val seed = 3
        val writes = CoroutineScope(Dispatchers.IO)
        val solution = SudokuGenerator(Random(seed)).generate(Difficulty.EASY).solution

        val first = SudokuViewModel(repository, writes, generator = SudokuGenerator(Random(seed)))
        first.onStartGame()
        val started = withTimeout(10_000) { first.uiState.first { it.game != null } }
        val cell = checkNotNull(started.game).values.indexOf(0)
        first.onAction(SudokuAction.SelectCell(cell))
        first.onAction(SudokuAction.InputDigit(solution[cell]))
        val expected = first.uiState.value.game

        // Wait until the queued write has reached the database.
        withTimeout(10_000) {
            repository.observeSavedGames().first { it.firstOrNull()?.session?.entries == 1 }
        }

        val second = SudokuViewModel(repository, writes, resumeSavedGame = true)
        val resumed = withTimeout(10_000) { second.uiState.first { it.game != null } }
        assertEquals(expected, resumed.game)
    }
}
