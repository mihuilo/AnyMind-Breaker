package com.anymindbreaker.core.common.game

enum class GameType {
    CRYPTOGRAM,
    SUDOKU,
}

enum class Language {
    RU,
    EN,
}

enum class Difficulty(val level: Int) {
    EASY(1),
    NORMAL(2),
    HARD(3),
    EXPERT(4),
}

enum class GameResult {
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    ABANDONED,
}

/** Lives in a game played with lives: the third mistake ends the game. */
const val DEFAULT_LIVES = 3
