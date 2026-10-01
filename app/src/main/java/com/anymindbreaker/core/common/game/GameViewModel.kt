package com.anymindbreaker.core.common.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class GamePhase {
    SETUP,
    LOADING,
    PLAYING,
    FINISHED,
}

/**
 * Behaviour shared by every game screen: holding the running game, forwarding UI events to it
 * and driving the timer. A game's ViewModel only decides how a game is created and rendered.
 */
abstract class GameViewModel<S : GameState, A : GameAction>(
    private val tickAction: A,
) : ViewModel() {

    private var game: PuzzleGame<S, A>? = null
    private var timerJob: Job? = null
    private var screenVisible = false

    /** Publishes the current phase to the screen. [state] is null outside of a game. */
    protected abstract fun render(phase: GamePhase, state: S?, score: Int)

    protected fun attach(newGame: PuzzleGame<S, A>) {
        game = newGame
        publish(newGame)
        updateTimer()
    }

    protected fun showLoading() {
        game = null
        updateTimer()
        render(GamePhase.LOADING, null, 0)
    }

    /** Leaves the current game and returns to the setup step. */
    fun onNewGame() {
        game = null
        updateTimer()
        render(GamePhase.SETUP, null, 0)
    }

    fun onAction(action: A) {
        val current = game ?: return
        current.handleAction(action)
        publish(current)
        if (current.isFinished()) updateTimer()
    }

    /** The timer only runs while the game screen is on screen. */
    fun onScreenVisibilityChanged(visible: Boolean) {
        screenVisible = visible
        updateTimer()
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
