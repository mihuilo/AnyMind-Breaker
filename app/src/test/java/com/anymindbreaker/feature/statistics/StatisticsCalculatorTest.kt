package com.anymindbreaker.feature.statistics

import com.anymindbreaker.core.common.game.Difficulty
import com.anymindbreaker.core.common.game.GameResult
import com.anymindbreaker.core.common.game.GameSession
import com.anymindbreaker.core.common.game.GameType
import com.anymindbreaker.core.common.game.InMemoryGameRepository
import com.anymindbreaker.core.common.game.Language
import com.anymindbreaker.core.common.game.UserStatistics
import com.anymindbreaker.feature.home.presentation.HomeViewModel
import com.anymindbreaker.feature.statistics.domain.Achievement
import com.anymindbreaker.feature.statistics.domain.GameSummary
import com.anymindbreaker.feature.statistics.domain.StatisticsCalculator
import com.anymindbreaker.feature.statistics.presentation.StatisticsViewModel
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private var nextId = 0

private fun session(
    type: GameType = GameType.SUDOKU,
    result: GameResult = GameResult.COMPLETED,
    seconds: Long = 300,
    mistakes: Int = 0,
    hints: Int = 0,
    entries: Int = 10,
    score: Int = 100,
    language: Language? = if (type == GameType.CRYPTOGRAM) Language.RU else null,
    finishedAt: Long = (nextId + 1) * 1000L,
) = GameSession(
    sessionId = "s${nextId++}",
    gameType = type,
    puzzleId = "p",
    language = language,
    difficulty = Difficulty.NORMAL,
    startedAt = 0,
    finishedAt = finishedAt,
    durationSeconds = seconds,
    mistakes = mistakes,
    hintsUsed = hints,
    entries = entries,
    score = score,
    result = result,
)

class StatisticsCalculatorTest {

    @Test
    fun emptyHistoryGivesEmptySummary() {
        assertEquals(GameSummary(), StatisticsCalculator.summarize(emptyList()))
    }

    @Test
    fun summaryCountsResultsTimesAndAccuracy() {
        val summary = StatisticsCalculator.summarize(
            listOf(
                session(seconds = 100, mistakes = 1, hints = 1, entries = 10, score = 200),
                session(seconds = 300, mistakes = 0, hints = 0, entries = 10, score = 300),
                session(result = GameResult.FAILED, seconds = 50, mistakes = 3, entries = 10, score = 0),
                session(result = GameResult.ABANDONED, seconds = 20, mistakes = 0, entries = 10, score = 0),
            ),
        )
        assertEquals(4, summary.played)
        assertEquals(2, summary.completed)
        assertEquals(1, summary.failed)
        assertEquals(50, summary.successPercent)
        // Time statistics only look at solved games.
        assertEquals(200L, summary.averageSeconds)
        assertEquals(100L, summary.bestSeconds)
        assertEquals(4, summary.mistakes)
        assertEquals(1, summary.hints)
        assertEquals(500L, summary.score)
        assertEquals(90, summary.accuracyPercent)
    }

    @Test
    fun unsolvedGamesHaveNoTimeStatistics() {
        val summary = StatisticsCalculator.summarize(listOf(session(result = GameResult.FAILED, entries = 0)))
        assertNull(summary.averageSeconds)
        assertNull(summary.bestSeconds)
        assertNull(summary.accuracyPercent)
        assertEquals(0, summary.successPercent)
    }

    @Test
    fun overviewSplitsByGameAndLanguage() {
        val sessions = listOf(
            session(GameType.SUDOKU),
            session(GameType.SUDOKU),
            session(GameType.CRYPTOGRAM, language = Language.RU),
            session(GameType.CRYPTOGRAM, language = Language.EN, mistakes = 5),
        )
        val overview = StatisticsCalculator.overview(sessions, UserStatistics(currentStreak = 4, bestStreak = 7))

        assertEquals(4, overview.overall.played)
        assertEquals(2, overview.byGame.getValue(GameType.SUDOKU).played)
        assertEquals(2, overview.byGame.getValue(GameType.CRYPTOGRAM).played)
        // Sudoku has no language, so it is not counted in either language.
        assertEquals(1, overview.byLanguage.getValue(Language.RU).played)
        assertEquals(1, overview.byLanguage.getValue(Language.EN).played)
        assertEquals(5, overview.byLanguage.getValue(Language.EN).mistakes)
        assertEquals(4, overview.currentStreak)
        assertEquals(7, overview.bestStreak)
    }

    @Test
    fun todayOnlyCountsSessionsFinishedToday() {
        val sessions = listOf(
            session(seconds = 600, mistakes = 2, entries = 20, finishedAt = 5_000),
            session(seconds = 120, mistakes = 0, entries = 20, finishedAt = 6_000),
            session(seconds = 900, mistakes = 10, entries = 10, finishedAt = 100),
        )
        val today = StatisticsCalculator.today(sessions, startOfDayMillis = 1_000)
        assertEquals(720, today.playSeconds)
        assertEquals(95, today.accuracyPercent)

        val nothing = StatisticsCalculator.today(sessions, startOfDayMillis = 10_000)
        assertEquals(0, nothing.playSeconds)
        assertNull(nothing.accuracyPercent)
    }

    @Test
    fun noAchievementsWithoutWins() {
        val sessions = listOf(session(result = GameResult.FAILED), session(result = GameResult.ABANDONED))
        assertTrue(StatisticsCalculator.achievements(sessions).isEmpty())
    }

    @Test
    fun firstWinAndSpeedAchievements() {
        val slow = StatisticsCalculator.achievements(listOf(session(seconds = 60, mistakes = 1)))
        assertEquals(setOf(Achievement.FIRST_WIN), slow)

        val fast = StatisticsCalculator.achievements(listOf(session(seconds = 59, mistakes = 1)))
        assertEquals(setOf(Achievement.FIRST_WIN, Achievement.UNDER_A_MINUTE), fast)
    }

    @Test
    fun cryptogramWithoutHintsIsOnlyForCryptograms() {
        val sudoku = StatisticsCalculator.achievements(listOf(session(GameType.SUDOKU, hints = 0)))
        assertFalse(Achievement.CRYPTOGRAM_NO_HINTS in sudoku)

        val hinted = StatisticsCalculator.achievements(listOf(session(GameType.CRYPTOGRAM, hints = 1)))
        assertFalse(Achievement.CRYPTOGRAM_NO_HINTS in hinted)

        val clean = StatisticsCalculator.achievements(listOf(session(GameType.CRYPTOGRAM, hints = 0)))
        assertTrue(Achievement.CRYPTOGRAM_NO_HINTS in clean)
    }

    @Test
    fun countingAchievementsNeedEnoughGames() {
        val nine = List(9) { session(mistakes = 0) }
        val achievementsAtNine = StatisticsCalculator.achievements(nine)
        assertFalse(Achievement.SOLVED_10 in achievementsAtNine)
        assertFalse(Achievement.FLAWLESS_10 in achievementsAtNine)
        assertFalse(Achievement.SUDOKU_STREAK_10 in achievementsAtNine)

        val ten = nine + session(mistakes = 0)
        val achievementsAtTen = StatisticsCalculator.achievements(ten)
        assertTrue(Achievement.SOLVED_10 in achievementsAtTen)
        assertTrue(Achievement.FLAWLESS_10 in achievementsAtTen)
        assertTrue(Achievement.SUDOKU_STREAK_10 in achievementsAtTen)
        assertFalse(Achievement.SOLVED_100 in achievementsAtTen)

        assertTrue(Achievement.SOLVED_100 in StatisticsCalculator.achievements(List(100) { session() }))
    }

    @Test
    fun lostSudokuBreaksSudokuStreakButCryptogramDoesNot() {
        val broken = List(5) { session() } + session(result = GameResult.FAILED) + List(5) { session() }
        assertFalse(Achievement.SUDOKU_STREAK_10 in StatisticsCalculator.achievements(broken))

        val mixed = List(5) { session() } +
            session(GameType.CRYPTOGRAM, result = GameResult.FAILED) +
            List(5) { session() }
        assertTrue(Achievement.SUDOKU_STREAK_10 in StatisticsCalculator.achievements(mixed))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun statisticsFollowFinishedSessions() = runTest(dispatcher) {
        val repository = InMemoryGameRepository()
        val viewModel = StatisticsViewModel(repository, dispatcher)

        assertEquals(0, viewModel.uiState.first { !it.loading }.overview.overall.played)

        repository.finishSession(session(GameType.CRYPTOGRAM, seconds = 40, finishedAt = 10))
        repository.finishSession(session(GameType.SUDOKU, result = GameResult.FAILED, finishedAt = 20))

        val state = viewModel.uiState.first { it.history.size == 2 }
        assertEquals(1, state.overview.overall.completed)
        assertEquals(0, state.overview.currentStreak)
        assertEquals(1, state.overview.bestStreak)
        assertEquals(GameType.SUDOKU, state.history.first().gameType)
        assertTrue(Achievement.UNDER_A_MINUTE in state.achievements)
    }

    @Test
    fun homeShowsTodayAndTheLatestUnfinishedGame() = runTest(dispatcher) {
        val repository = InMemoryGameRepository()
        val viewModel = HomeViewModel(repository, startOfToday = { 1_000 })

        repository.finishSession(session(seconds = 180, mistakes = 1, entries = 10, finishedAt = 2_000))
        repository.finishSession(session(seconds = 600, finishedAt = 500))
        val unfinished = session(GameType.CRYPTOGRAM, result = GameResult.IN_PROGRESS)
        repository.saveProgress(unfinished, "payload", updatedAt = 3_000)

        val state = viewModel.uiState.first { it.continueGame != null }
        assertEquals(GameType.CRYPTOGRAM, state.continueGame)
        assertEquals(180, state.today.playSeconds)
        assertEquals(90, state.today.accuracyPercent)
    }
}
