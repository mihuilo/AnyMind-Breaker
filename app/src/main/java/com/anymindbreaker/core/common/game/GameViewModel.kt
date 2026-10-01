package com.anymindbreaker.core.common.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

enum class GamePhase {
    SETUP,
    LOADING,
    PLAYING,
    FINISHED,
}

/**
 * Behaviour shared by every game screen: holding the running game, forwarding UI events to it,
 * driving the timer and keeping the session and the unfinished game in the repository.
 * A game's ViewModel only decides how a game is created, serialized and rendered.
 */
abstract class GameViewModel<S : GameState, A : GameAction>(
    private val gameType: GameType,
    private val tickAction: A,
    private val repository: GameRepository,
    /** Outlives the ViewModel so the last save is not lost when the screen is closed. */
    persistenceScope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private var game: PuzzleGame<S, A>? = null
    private var session: GameSession? = null
    private var timerJob: Job? = null
    private var screenVisible = false

    // Writes go through one queue so they reach the database in the order they were made.
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    private val _savedGameAvailable = MutableStateFlow(false)

    /** True while there is an unfinished game the player has not yet been asked about. */
    val savedGameAvailable: StateFlow<Boolean> = _savedGameAvailable.asStateFlow()

    init {
        persistenceScope.launch {
            for (write in writes) write()
        }
    }

    /** Publishes the current phase to the screen. [state] is null outside of a game. */
    protected abstract fun render(phase: GamePhase, state: S?, score: Int)

    /** Serializes the running game so it can be continued after the app is closed. */
    protected abstract fun snapshot(state: S): String

    /** Rebuilds a game from [snapshot] output, or returns null if it cannot be read. */
    protected abstract fun restore(payload: String): PuzzleGame<S, A>?

    /**
     * Looks for an unfinished game. Subclasses call this at the end of their own initialization,
     * because restoring needs their fields.
     */
    protected fun checkSavedGame(resumeImmediately: Boolean) {
        viewModelScope.launch {
            val saved = repository.savedGame(gameType) ?: return@launch
            if (game != null) return@launch
            if (resumeImmediately) resume(saved) else _savedGameAvailable.value = true
        }
    }

    protected fun startNewGame(newGame: PuzzleGame<S, A>, puzzle: Puzzle) {
        val newSession = GameSession(
            sessionId = UUID.randomUUID().toString(),
            gameType = gameType,
            puzzleId = puzzle.id,
            language = puzzle.language,
            difficulty = puzzle.difficulty,
            startedAt = clock(),
        )
        _savedGameAvailable.value = false
        val now = clock()
        enqueue {
            // A new game replaces the unfinished one, which is recorded as abandoned.
            repository.savedGame(gameType)?.let { old ->
                repository.finishSession(old.session.copy(result = GameResult.ABANDONED, finishedAt = now))
            }
        }
        game = newGame
        session = newSession
        persist()
        publish(newGame)
        updateTimer()
    }

    protected fun showLoading() {
        game = null
        session = null
        updateTimer()
        render(GamePhase.LOADING, null, 0)
    }

    fun onResumeSavedGame() {
        _savedGameAvailable.value = false
        viewModelScope.launch {
            val saved = repository.savedGame(gameType) ?: return@launch
            if (game == null) resume(saved)
        }
    }

    /** The player chose to start over; the save is replaced when the new game begins. */
    fun onDismissSavedGame() {
        _savedGameAvailable.value = false
    }

    /** Leaves the current game and returns to the setup step. */
    fun onNewGame() {
        game = null
        session = null
        updateTimer()
        render(GamePhase.SETUP, null, 0)
    }

    fun onAction(action: A) {
        val current = game ?: return
        current.handleAction(action)
        publish(current)
        if (current.isFinished()) {
            finish(current)
            updateTimer()
        } else if (action != tickAction) {
            persist()
        }
    }

    /** The timer only runs while the game screen is on screen. */
    fun onScreenVisibilityChanged(visible: Boolean) {
        screenVisible = visible
        // Timer ticks are not saved one by one, so the elapsed time is stored when the screen is left.
        if (!visible) persist()
        updateTimer()
    }

    override fun onCleared() {
        // The queue is drained by the persistence scope after the ViewModel is gone.
        writes.close()
    }

    private fun resume(saved: SavedGame) {
        val restored = restore(saved.payload)
        if (restored == null || restored.isFinished()) {
            val abandoned = saved.session.copy(result = GameResult.ABANDONED, finishedAt = clock())
            enqueue { repository.finishSession(abandoned) }
            return
        }
        game = restored
        session = saved.session
        publish(restored)
        updateTimer()
    }

    private fun GameSession.withProgress(current: PuzzleGame<S, A>): GameSession {
        val state = current.getState()
        return copy(
            durationSeconds = state.elapsedSeconds,
            mistakes = state.mistakes,
            hintsUsed = state.hintsUsed,
            entries = state.entries,
            score = current.calculateScore(),
            result = state.result,
        )
    }

    private fun persist() {
        val current = game ?: return
        if (current.isFinished()) return
        val updated = session?.withProgress(current) ?: return
        session = updated
        val payload = snapshot(current.getState())
        val now = clock()
        enqueue { repository.saveProgress(updated, payload, now) }
    }

    private fun finish(current: PuzzleGame<S, A>) {
        val finished = session?.withProgress(current)?.copy(finishedAt = clock()) ?: return
        session = null
        enqueue { repository.finishSession(finished) }
    }

    private fun enqueue(write: suspend () -> Unit) {
        writes.trySend(write)
    }

    private fun publish(current: PuzzleGame<S, A>) {
        val phase = if (current.isFinished()) GamePhase.FINISHED else GamePhase.PLAYING
        render(phase, current.getState(), current.calculateScore())
    }

    private fun updateTimer() {
        val shouldRun = screenVisible && game?.isFinished() == false
        if (shouldRun && timerJob?.isActive != true) {
            timerJob = viewModelScope.launch {
                while (isActive) {
                    delay(1000)
                    onAction(tickAction)
                }
            }
        } else if (!shouldRun) {
            timerJob?.cancel()
            timerJob = null
        }
    }
}
