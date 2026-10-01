package com.anymindbreaker.core.common.game

/** Base description of a task. Each game extends it with its own data. */
interface Puzzle {
    val id: String
    val gameType: GameType

    /** Content language, or null for language-independent games such as Sudoku. */
    val language: Language?
    val difficulty: Difficulty
    val createdAt: Long
    val metadata: Map<String, String>
}
