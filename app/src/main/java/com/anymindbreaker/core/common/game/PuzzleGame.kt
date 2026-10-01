package com.anymindbreaker.core.common.game

/** Marker for events a game accepts from the UI. */
interface GameAction

/** Part of the game state shared by every game; games add their own fields. */
interface GameState {
    val mistakes: Int
    val hintsUsed: Int

    /** Remaining lives, or null when the game is played without lives. */
    val livesLeft: Int?
    val result: GameResult
}

/**
 * Common contract of every game. A new game implements it in its own feature package
 * and does not require changes to the existing games.
 */
interface PuzzleGame<S : GameState, A : GameAction> {
    val gameType: GameType

    fun start()

    fun handleAction(action: A)

    fun getState(): S

    fun isFinished(): Boolean = getState().result != GameResult.IN_PROGRESS

    fun calculateScore(): Int
}
