package com.anymindbreaker.feature.statistics

import com.anymindbreaker.core.common.game.Difficulty
import com.anymindbreaker.core.common.game.GameResult
import com.anymindbreaker.core.common.game.GameSession
import com.anymindbreaker.core.common.game.GameType
import com.anymindbreaker.core.common.game.InMemoryGameRepository
import com.anymindbreaker.core.common.game.Language
import com.anymindbreaker.feature.home.presentation.HomeViewModel
import com.anymindbreaker.feature.statistics.domain.Achievement
import com.anymindbreaker.feature.statistics.domain.GameSummary
import com.anymindbreaker.feature.statistics.domain.Highlights
import com.anymindbreaker.feature.statistics.domain.StatisticsCalculator
import com.anymindbreaker.feature.statistics.domain.StatisticsFilter
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
    difficulty: Difficulty = Difficulty.NORMAL,
    finishedAt: Long? = (nextId + 1) * 1000L,
    livesEnabled: Boolean = true,
) = GameSession(
    livesEnabled = livesEnabled,
    sessionId = "s${nextId++}",
    gameType = type,
    puzzleId = "p",
    language = language,
    difficulty = difficulty,
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
        assertEquals(Highlights(), StatisticsCalculator.highlights(emptyList(), recentSinceMillis = 0))
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
        val sessions = listOf(session(result = GameResult.FAILED, entries = 0))
        val summary = StatisticsCalculator.summarize(sessions)
        assertNull(summary.averageSeconds)
        assertNull(summary.bestSeconds)
        assertNull(summary.accuracyPercent)
        assertEquals(0, summary.successPercent)
        assertNull(StatisticsCalculator.highlights(sessions, 0).bestSeconds)
    }

    @Test
    fun highlightsCountWinsFlawlessWinsAndBestTime() {
        val highlights = StatisticsCalculator.highlights(
            listOf(
                session(seconds = 120, mistakes = 0),
                session(seconds = 38, mistakes = 2),
                session(seconds = 90, mistakes = 0),
                session(result = GameResult.FAILED, seconds = 10, mistakes = 0),
            ),
            recentSinceMillis = 0,
        )
        assertEquals(3, highlights.wins)
        // A lost game without mistakes is not a flawless win.
        assertEquals(2, highlights.flawlessWins)
        assertEquals(38L, highlights.bestSeconds)
        assertEquals(258L, highlights.playSeconds)
    }

    @Test
    fun gainsOnlyCountRecentWins() {
        val highlights = StatisticsCalculator.highlights(
            listOf(
                session(mistakes = 0, finishedAt = 100),
                session(mistakes = 1, finishedAt = 5_000),
                session(mistakes = 0, finishedAt = 6_000),
                session(result = GameResult.FAILED, finishedAt = 7_000),
            ),
            recentSinceMillis = 1_000,
        )
        assertEquals(3, highlights.wins)
        assertEquals(2, highlights.recentWins)
        assertEquals(2, highlights.flawlessWins)
        assertEquals(1, highlights.recentFlawlessWins)
    }

    @Test
    fun streaksFollowTheOrderGamesWereFinished() {
        // Given out of order on purpose: wins at 1–3, a loss at 4, wins at 5–6, an abandoned game at 7.
        val sessions = listOf(
            session(finishedAt = 5),
            session(result = GameResult.FAILED, finishedAt = 4),
            session(finishedAt = 1),
            session(finishedAt = 2),
            session(finishedAt = 3),
            session(result = GameResult.ABANDONED, finishedAt = 7),
            session(finishedAt = 6),
        )
        val highlights = StatisticsCalculator.highlights(sessions, 0)
        assertEquals(3, highlights.bestStreak)
        // The abandoned game does not break the current run.
        assertEquals(2, highlights.currentStreak)
    }

    @Test
    fun gameWithoutLivesResetsTheStreakAndDoesNotCountTowardsIt() {
        val sessions = listOf(
            session(finishedAt = 1),
            session(finishedAt = 2),
            session(finishedAt = 3),
            // Won, but without lives.
            session(finishedAt = 4, livesEnabled = false),
            session(finishedAt = 5),
        )
        val highlights = StatisticsCalculator.highlights(sessions, 0)
        assertEquals(1, highlights.currentStreak)
        // The record set before stays.
        assertEquals(3, highlights.bestStreak)
        // It is still a win.
        assertEquals(5, highlights.wins)
    }

    @Test
    fun startingGameWithoutLivesAlreadyResetsTheStreak() {
        val finished = listOf(session(finishedAt = 1_000), session(finishedAt = 2_000))
        fun unfinished(livesEnabled: Boolean) = session(
            result = GameResult.IN_PROGRESS,
            finishedAt = null,
            livesEnabled = livesEnabled,
        ).copy(startedAt = 3_000)

        val withLives = StatisticsCalculator.highlights(finished, 0, listOf(unfinished(livesEnabled = true)))
        assertEquals(2, withLives.currentStreak)

        val withoutLives = StatisticsCalculator.highlights(finished, 0, listOf(unfinished(livesEnabled = false)))
        assertEquals(0, withoutLives.currentStreak)
        assertEquals(2, withoutLives.bestStreak)
        // A game in progress is not counted as played.
        assertEquals(2, withoutLives.wins)
    }

    @Test
    fun filterSelectsGameDifficultyAndLanguage() {
        val sudokuHard = session(GameType.SUDOKU, difficulty = Difficulty.HARD)
        val cryptoRu = session(GameType.CRYPTOGRAM, language = Language.RU, difficulty = Difficulty.EASY)
        val cryptoEn = session(GameType.CRYPTOGRAM, language = Language.EN, difficulty = Difficulty.HARD)
        val all = listOf(sudokuHard, cryptoRu, cryptoEn)

        fun select(filter: StatisticsFilter) = all.filter(filter::matches)

        assertEquals(listOf(sudokuHard), select(StatisticsFilter(GameType.SUDOKU)))
        assertEquals(listOf(cryptoRu, cryptoEn), select(StatisticsFilter(GameType.CRYPTOGRAM)))
        assertEquals(listOf(cryptoEn), select(StatisticsFilter(GameType.CRYPTOGRAM, Difficulty.HARD)))
        assertEquals(listOf(cryptoRu), select(StatisticsFilter(GameType.CRYPTOGRAM, language = Language.RU)))
        assertEquals(
            emptyList<GameSession>(),
            select(StatisticsFilter(GameType.CRYPTOGRAM, Difficulty.HARD, Language.RU)),
        )
        assertEquals(emptyList<GameSession>(), select(StatisticsFilter(GameType.SUDOKU, Difficulty.EASY)))
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
    fun statisticsFollowFinishedSessionsOfTheSelectedGame() = runTest(dispatcher) {
        val repository = InMemoryGameRepository()
        val viewModel = StatisticsViewModel(repository, dispatcher, now = { 100_000 })
        viewModel.onGameSelected(GameType.SUDOKU)

        assertEquals(0, viewModel.uiState.first { !it.loading }.highlights.wins)

        repository.finishSession(session(GameType.CRYPTOGRAM, seconds = 40, finishedAt = 10))
        repository.finishSession(session(GameType.SUDOKU, seconds = 200, finishedAt = 20))
        repository.finishSession(session(GameType.SUDOKU, result = GameResult.FAILED, finishedAt = 30))

        val sudoku = viewModel.uiState.first { it.history.size == 3 }
        assertEquals(1, sudoku.highlights.wins)
        assertEquals(1, sudoku.highlights.bestStreak)
        assertEquals(0, sudoku.highlights.currentStreak)
        assertEquals(200L, sudoku.highlights.bestSeconds)
        assertEquals(2, sudoku.summary.played)
        // History and achievements cover every game, whatever the filter.
        assertEquals(GameType.SUDOKU, sudoku.history.first().gameType)
        assertTrue(Achievement.UNDER_A_MINUTE in sudoku.achievements)

        viewModel.onGameSelected(GameType.CRYPTOGRAM)
        val cryptogram = viewModel.uiState.first { it.filter.gameType == GameType.CRYPTOGRAM }
        assertEquals(1, cryptogram.highlights.wins)
        assertEquals(40L, cryptogram.highlights.bestSeconds)
    }

    @Test
    fun difficultyAndLanguageNarrowTheStatistics() = runTest(dispatcher) {
        val repository = InMemoryGameRepository()
        repository.finishSession(session(GameType.CRYPTOGRAM, language = Language.RU, difficulty = Difficulty.EASY))
        repository.finishSession(session(GameType.CRYPTOGRAM, language = Language.EN, difficulty = Difficulty.EASY))
        repository.finishSession(session(GameType.CRYPTOGRAM, language = Language.EN, difficulty = Difficulty.HARD))
        val viewModel = StatisticsViewModel(repository, dispatcher)
        viewModel.onGameSelected(GameType.CRYPTOGRAM)

        assertEquals(3, viewModel.uiState.first { !it.loading }.highlights.wins)

        viewModel.onDifficultySelected(Difficulty.EASY)
        assertEquals(2, viewModel.uiState.first { it.filter.difficulty == Difficulty.EASY }.highlights.wins)

        viewModel.onLanguageSelected(Language.EN)
        assertEquals(1, viewModel.uiState.first { it.filter.language == Language.EN }.highlights.wins)

        // Choosing another game clears the narrower filters.
        viewModel.onGameSelected(GameType.SUDOKU)
        val sudoku = viewModel.uiState.first { it.filter.gameType == GameType.SUDOKU }
        assertEquals(StatisticsFilter(GameType.SUDOKU), sudoku.filter)
        assertEquals(0, sudoku.highlights.wins)
    }

    @Test
    fun unfinishedGameWithoutLivesResetsTheShownStreak() = runTest(dispatcher) {
        val repository = InMemoryGameRepository()
        repository.finishSession(session(GameType.SUDOKU, finishedAt = 10))
        repository.finishSession(session(GameType.SUDOKU, finishedAt = 20))
        val viewModel = StatisticsViewModel(repository, dispatcher)
        viewModel.onGameSelected(GameType.SUDOKU)
        assertEquals(2, viewModel.uiState.first { !it.loading }.highlights.currentStreak)

        val started = session(GameType.SUDOKU, result = GameResult.IN_PROGRESS, finishedAt = null, livesEnabled = false)
        repository.saveProgress(started.copy(startedAt = 30), "payload", updatedAt = 30)

        val highlights = viewModel.uiState.first { it.highlights.currentStreak == 0 }.highlights
        assertEquals(2, highlights.bestStreak)
    }

    @Test
    fun gainCoversTheLastSevenDays() = runTest(dispatcher) {
        val day = 24 * 60 * 60 * 1000L
        val repository = InMemoryGameRepository()
        repository.finishSession(session(GameType.SUDOKU, finishedAt = 2 * day))
        repository.finishSession(session(GameType.SUDOKU, finishedAt = 9 * day))
        val viewModel = StatisticsViewModel(repository, dispatcher, now = { 10 * day })
        viewModel.onGameSelected(GameType.SUDOKU)

        val highlights = viewModel.uiState.first { !it.loading }.highlights
        assertEquals(2, highlights.wins)
        assertEquals(1, highlights.recentWins)
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
