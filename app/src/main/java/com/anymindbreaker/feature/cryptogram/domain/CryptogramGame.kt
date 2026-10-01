package com.anymindbreaker.feature.cryptogram.domain

import com.anymindbreaker.core.common.game.Difficulty
import com.anymindbreaker.core.common.game.GameAction
import com.anymindbreaker.core.common.game.GameResult
import com.anymindbreaker.core.common.game.GameState
import com.anymindbreaker.core.common.game.GameType
import com.anymindbreaker.core.common.game.PuzzleGame
import com.anymindbreaker.core.common.game.ScoreCalculator
import com.anymindbreaker.core.common.game.ScoreInput
import kotlinx.serialization.Serializable

@Serializable
data class CryptogramState(
    val cipherText: String,
    /** Cipher letter → the plain letter the player assigned to it. */
    val guesses: Map<Char, Char> = emptyMap(),
    /** Cipher letters whose answer is known to be right and can no longer be changed. */
    val locked: Set<Char> = emptySet(),
    /** Cipher letters whose current guess is wrong. */
    val wrong: Set<Char> = emptySet(),
    /** Single positions opened by a hint, without revealing the whole correspondence. */
    val revealed: Map<Int, Char> = emptyMap(),
    /** Index of the selected character of the text. */
    val selected: Int? = null,
    val elapsedSeconds: Long = 0,
    /** Number of letters the player has entered, used to compute accuracy. */
    val entries: Int = 0,
    override val mistakes: Int = 0,
    override val hintsUsed: Int = 0,
    override val livesLeft: Int? = null,
    override val result: GameResult = GameResult.IN_PROGRESS,
) : GameState {

    /** The letter shown in the answer field at [index], or null when the field is empty. */
    fun answerAt(index: Int): Char? = revealed[index] ?: guesses[cipherText[index]]
}

sealed interface CryptogramAction : GameAction {
    data class SelectPosition(val index: Int) : CryptogramAction
    data class InputLetter(val letter: Char) : CryptogramAction
    data object Erase : CryptogramAction

    /** Opens one correspondence everywhere in the text. */
    data object HintLetter : CryptogramAction

    /** Opens every letter of one word. */
    data object HintWord : CryptogramAction

    /** Opens a single position of the text. */
    data object HintPosition : CryptogramAction
    data object Tick : CryptogramAction
}

class CryptogramGame(
    private val puzzle: CryptogramPuzzle,
    /** Number of lives, or null to play without lives. */
    private val lives: Int?,
    private val scoreCalculator: ScoreCalculator,
    initialState: CryptogramState? = null,
) : PuzzleGame<CryptogramState, CryptogramAction> {

    override val gameType: GameType = GameType.CRYPTOGRAM

    private val alphabet = CryptogramAlphabet.of(puzzle.language)
    private val cipher = puzzle.cipherText
    private val solution: Map<Char, Char> =
        cipher.indices.filter { cipher[it] in alphabet }.associate { cipher[it] to puzzle.text[it] }

    private var state = initialState ?: newState()

    override fun start() {
        state = newState()
    }

    override fun getState(): CryptogramState = state

    override fun handleAction(action: CryptogramAction) {
        if (isFinished()) return
        state = when (action) {
            is CryptogramAction.SelectPosition -> select(action.index)
            is CryptogramAction.InputLetter -> input(action.letter)
            CryptogramAction.Erase -> erase()
            CryptogramAction.HintLetter -> hintLetter()
            CryptogramAction.HintWord -> hintWord()
            CryptogramAction.HintPosition -> hintPosition()
            CryptogramAction.Tick -> state.copy(elapsedSeconds = state.elapsedSeconds + 1)
        }
    }

    override fun calculateScore(): Int = scoreCalculator.calculate(
        ScoreInput(
            difficulty = puzzle.difficulty,
            result = state.result,
            durationSeconds = state.elapsedSeconds,
            targetSeconds = targetSeconds(puzzle.difficulty),
            mistakes = state.mistakes,
            hintsUsed = state.hintsUsed,
            livesLeft = state.livesLeft,
        ),
    )

    private fun newState(): CryptogramState {
        val hints = puzzle.hints.associate { it.cipher to it.plain }
        val initial = CryptogramState(
            cipherText = cipher,
            guesses = hints,
            locked = hints.keys,
            livesLeft = lives,
        )
        return initial.copy(selected = nextOpenPosition(initial, from = -1))
    }

    private fun isLetter(index: Int) = index in cipher.indices && cipher[index] in alphabet

    /** A position the player still has to solve. */
    private fun isOpen(s: CryptogramState, index: Int) =
        isLetter(index) && index !in s.revealed && cipher[index] !in s.locked

    private fun nextOpenPosition(s: CryptogramState, from: Int): Int? {
        for (step in 1..cipher.length) {
            val index = (from + step).mod(cipher.length)
            if (isOpen(s, index)) return index
        }
        return null
    }

    private fun isSolved(s: CryptogramState) =
        cipher.indices.all { !isLetter(it) || s.answerAt(it) == puzzle.text[it] }

    private fun finishIfSolved(s: CryptogramState) =
        if (isSolved(s)) s.copy(result = GameResult.COMPLETED) else s

    private fun select(index: Int) = if (isLetter(index)) state.copy(selected = index) else state

    private fun input(letter: Char): CryptogramState {
        val position = state.selected?.takeIf { isOpen(state, it) } ?: return state
        val target = cipher[position]
        if (letter !in alphabet || state.guesses[target] == letter) return state

        // A plain letter can stand for only one cipher letter, so it is taken from its previous owner.
        val previousOwner = state.guesses.entries.firstOrNull { it.value == letter }?.key
        if (previousOwner != null && previousOwner in state.locked) return state

        val guesses = state.guesses.toMutableMap()
        if (previousOwner != null) guesses.remove(previousOwner)
        guesses[target] = letter
        val wrongWithoutMoved = if (previousOwner != null) state.wrong - previousOwner else state.wrong

        if (solution[target] == letter) {
            val next = state.copy(
                guesses = guesses,
                locked = state.locked + target,
                wrong = wrongWithoutMoved - target,
                entries = state.entries + 1,
            )
            return finishIfSolved(next.copy(selected = nextOpenPosition(next, position) ?: position))
        }

        val livesLeft = state.livesLeft?.let { it - 1 }
        return state.copy(
            guesses = guesses,
            wrong = wrongWithoutMoved + target,
            entries = state.entries + 1,
            mistakes = state.mistakes + 1,
            livesLeft = livesLeft,
            result = if (livesLeft != null && livesLeft <= 0) GameResult.FAILED else GameResult.IN_PROGRESS,
        )
    }

    private fun erase(): CryptogramState {
        val position = state.selected?.takeIf { isOpen(state, it) } ?: return state
        val target = cipher[position]
        if (target !in state.guesses) return state
        return state.copy(guesses = state.guesses - target, wrong = state.wrong - target)
    }

    private fun withSolvedLetters(s: CryptogramState, letters: Collection<Char>): CryptogramState {
        val guesses = s.guesses.toMutableMap()
        for (letter in letters) {
            val plain = solution.getValue(letter)
            // Drop a wrong guess that currently occupies this plain letter.
            guesses.entries.removeAll { it.value == plain && it.key != letter }
            guesses[letter] = plain
        }
        return s.copy(
            guesses = guesses,
            locked = s.locked + letters,
            wrong = s.wrong.filter { it in guesses && it !in letters }.toSet(),
        )
    }

    private fun hintTarget(): Int? = state.selected?.takeIf { isOpen(state, it) } ?: nextOpenPosition(state, -1)

    private fun afterHint(s: CryptogramState, position: Int): CryptogramState {
        val used = s.copy(hintsUsed = s.hintsUsed + 1)
        val selected = if (isOpen(used, position)) position else nextOpenPosition(used, position) ?: position
        return finishIfSolved(used.copy(selected = selected))
    }

    private fun hintLetter(): CryptogramState {
        val position = hintTarget() ?: return state
        return afterHint(withSolvedLetters(state, listOf(cipher[position])), position)
    }

    private fun hintWord(): CryptogramState {
        val position = hintTarget() ?: return state
        var start = position
        while (isLetter(start - 1)) start--
        var end = position
        while (isLetter(end + 1)) end++
        val letters = (start..end).map { cipher[it] }.filter { it !in state.locked }.distinct()
        return afterHint(withSolvedLetters(state, letters), position)
    }

    private fun hintPosition(): CryptogramState {
        val position = hintTarget() ?: return state
        return afterHint(state.copy(revealed = state.revealed + (position to puzzle.text[position])), position)
    }

    private fun targetSeconds(difficulty: Difficulty): Long = when (difficulty) {
        Difficulty.EASY -> 2 * 60L
        Difficulty.NORMAL -> 5 * 60L
        Difficulty.HARD -> 10 * 60L
        Difficulty.EXPERT -> 15 * 60L
    }
}
