package com.anymindbreaker.core.common.game

import com.anymindbreaker.feature.cryptogram.data.CryptogramText
import com.anymindbreaker.feature.cryptogram.data.CryptogramTextSource
import com.anymindbreaker.feature.cryptogram.domain.CryptogramAction
import com.anymindbreaker.feature.cryptogram.domain.CryptogramGenerator
import com.anymindbreaker.feature.cryptogram.presentation.CryptogramViewModel
import com.anymindbreaker.feature.sudoku.domain.SudokuAction
import com.anymindbreaker.feature.sudoku.domain.SudokuGenerator
import com.anymindbreaker.feature.sudoku.presentation.SudokuViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class UserStatisticsTest {

    private fun session(result: GameResult, score: Int = 100) = GameSession(
        sessionId = "s",
        gameType = GameType.SUDOKU,
        puzzleId = "p",
        language = null,
        difficulty = Difficulty.EASY,
        startedAt = 0,
        durationSeconds = 60,
        mistakes = 2,
        hintsUsed = 1,
        score = score,
        result = result,
    )

    @Test
    fun totalsAccumulate() {
        val stats = UserStatistics()
            .afterSession(session(GameResult.COMPLETED))
            .afterSession(session(GameResult.FAILED, score = 0))
        assertEquals(2, stats.totalGames)
        assertEquals(1, stats.completedGames)
        assertEquals(1, stats.failedGames)
        assertEquals(100, stats.totalScore)
        assertEquals(4, stats.totalMistakes)
        assertEquals(2, stats.totalHints)
        assertEquals(120, stats.totalPlayTimeSeconds)
    }

    @Test
    fun winsExtendStreakAndLossResetsIt() {
        var stats = UserStatistics()
        repeat(3) { stats = stats.afterSession(session(GameResult.COMPLETED)) }
        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.bestStreak)

        stats = stats.afterSession(session(GameResult.FAILED))
        assertEquals(0, stats.currentStreak)
        assertEquals(3, stats.bestStreak)

        stats = stats.afterSession(session(GameResult.COMPLETED))
        assertEquals(1, stats.currentStreak)
        assertEquals(3, stats.bestStreak)
    }

    @Test
    fun abandonedGameKeepsStreak() {
        val stats = UserStatistics()
            .afterSession(session(GameResult.COMPLETED))
            .afterSession(session(GameResult.ABANDONED, score = 0))
        assertEquals(1, stats.currentStreak)
        assertEquals(2, stats.totalGames)
        assertEquals(0, stats.failedGames)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class GamePersistenceTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = InMemoryGameRepository()
    private val seed = 5

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun sudokuViewModel(resume: Boolean = false) = SudokuViewModel(
        repository = repository,
        persistenceScope = CoroutineScope(dispatcher),
        generator = SudokuGenerator(Random(seed)),
        generationDispatcher = dispatcher,
        resumeSavedGame = resume,
    )

    private val solution = SudokuGenerator(Random(seed)).generate(Difficulty.EASY).solution

    private fun SudokuViewModel.enter(cell: Int, digit: Int) {
        onAction(SudokuAction.SelectCell(cell))
        onAction(SudokuAction.InputDigit(digit))
    }

    private fun SudokuViewModel.firstEmptyCell() = checkNotNull(uiState.value.game).values.indexOf(0)

    @Test
    fun unfinishedSudokuIsSavedAndContinued() = runTest(dispatcher) {
        val first = sudokuViewModel()
        first.onStartGame()
        val cell = first.firstEmptyCell()
        first.enter(cell, solution[cell])
        repeat(7) { first.onAction(SudokuAction.Tick) }
        first.onScreenVisibilityChanged(false)
        val before = checkNotNull(first.uiState.value.game)

        val saved = checkNotNull(repository.savedGame(GameType.SUDOKU))
        assertEquals(GameResult.IN_PROGRESS, saved.session.result)
        assertEquals(7, saved.session.durationSeconds)
        assertEquals(1, saved.session.entries)

        val second = sudokuViewModel(resume = true)
        assertEquals(GamePhase.PLAYING, second.uiState.value.phase)
        assertEquals(before, second.uiState.value.game)

        // The continued game keeps working with the same puzzle.
        val next = second.firstEmptyCell()
        second.enter(next, solution[next])
        assertEquals(0, checkNotNull(second.uiState.value.game).mistakes)
        assertEquals(saved.session.sessionId, checkNotNull(repository.savedGame(GameType.SUDOKU)).session.sessionId)
    }

    @Test
    fun savedGameIsOfferedInsteadOfResumedByDefault() = runTest(dispatcher) {
        sudokuViewModel().apply {
            onStartGame()
            onScreenVisibilityChanged(false)
        }

        val second = sudokuViewModel()
        assertTrue(second.savedGameAvailable.value)
        assertEquals(GamePhase.SETUP, second.uiState.value.phase)

        second.onResumeSavedGame()
        assertFalse(second.savedGameAvailable.value)
        assertEquals(GamePhase.PLAYING, second.uiState.value.phase)
    }

    @Test
    fun finishedGameIsRecordedAndSaveIsRemoved() = runTest(dispatcher) {
        val viewModel = sudokuViewModel()
        viewModel.onStartGame()
        val start = checkNotNull(viewModel.uiState.value.game).values
        for (cell in start.indices) {
            if (start[cell] == 0) viewModel.enter(cell, solution[cell])
        }

        assertEquals(GamePhase.FINISHED, viewModel.uiState.value.phase)
        assertNull(repository.savedGame(GameType.SUDOKU))

        val sessions = repository.observeFinishedSessions().first()
        assertEquals(1, sessions.size)
        assertEquals(GameResult.COMPLETED, sessions[0].result)
        assertEquals(viewModel.uiState.value.score, sessions[0].score)
        assertNotNull(sessions[0].finishedAt)

        val statistics = repository.observeStatistics().first()
        assertEquals(1, statistics.completedGames)
        assertEquals(1, statistics.currentStreak)
    }

    @Test
    fun startingNewGameAbandonsTheSavedOne() = runTest(dispatcher) {
        val first = sudokuViewModel()
        first.onStartGame()
        val oldId = checkNotNull(repository.savedGame(GameType.SUDOKU)).session.sessionId

        val second = sudokuViewModel()
        second.onDismissSavedGame()
        second.onStartGame()

        val sessions = repository.observeFinishedSessions().first()
        assertEquals(listOf(oldId), sessions.map { it.sessionId })
        assertEquals(GameResult.ABANDONED, sessions[0].result)
        val saved = checkNotNull(repository.savedGame(GameType.SUDOKU))
        assertTrue(saved.session.sessionId != oldId)
    }

    @Test
    fun unreadableSaveIsDroppedInsteadOfCrashing() = runTest(dispatcher) {
        val session = GameSession("broken", GameType.SUDOKU, "p", null, Difficulty.EASY, startedAt = 0)
        repository.saveProgress(session, payload = "not json", updatedAt = 1)

        val viewModel = sudokuViewModel(resume = true)
        assertEquals(GamePhase.SETUP, viewModel.uiState.value.phase)
        assertNull(repository.savedGame(GameType.SUDOKU))
    }

    @Test
    fun cryptogramIsSavedAndContinuedIndependentlyOfSudoku() = runTest(dispatcher) {
        val phrase = CryptogramText("t1", Difficulty.EASY, "Practice makes perfect.")
        val source = object : CryptogramTextSource {
            override suspend fun texts(language: Language) = listOf(phrase)
        }

        fun viewModel(resume: Boolean = false) = CryptogramViewModel(
            textSource = source,
            scoreCalculator = DefaultScoreCalculator(),
            repository = repository,
            persistenceScope = CoroutineScope(dispatcher),
            generator = CryptogramGenerator(Random(seed)),
            resumeSavedGame = resume,
        )

        sudokuViewModel().onStartGame()

        val first = viewModel()
        first.onLivesEnabledChanged(true)
        first.onStartGame()
        first.onAction(CryptogramAction.HintPosition)
        first.onAction(CryptogramAction.HintLetter)
        first.onScreenVisibilityChanged(false)
        val before = checkNotNull(first.uiState.value.game)
        assertEquals(2, before.hintsUsed)

        val second = viewModel(resume = true)
        assertEquals(before, second.uiState.value.game)
        assertEquals(Language.EN, second.uiState.value.gameLanguage)
        assertTrue(second.uiState.value.livesEnabled)

        assertNotNull(repository.savedGame(GameType.SUDOKU))
        assertEquals(2, repository.observeSavedGames().first().size)
    }
}
