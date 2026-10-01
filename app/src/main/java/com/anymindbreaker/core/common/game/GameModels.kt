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
