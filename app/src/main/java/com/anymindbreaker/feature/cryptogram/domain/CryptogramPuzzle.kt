package com.anymindbreaker.feature.cryptogram.domain

import com.anymindbreaker.core.common.game.Difficulty
import com.anymindbreaker.core.common.game.GameType
import com.anymindbreaker.core.common.game.Language
import com.anymindbreaker.core.common.game.Puzzle
import kotlinx.serialization.Serializable
import kotlin.random.Random

/** A correspondence shown to the player before the game starts. */
@Serializable
data class CryptogramHint(val code: Int, val plain: Char)

@Serializable
data class CryptogramPuzzle(
    override val id: String,
    override val language: Language,
    override val difficulty: Difficulty,
    /** Normalized plain text. */
    val text: String,
    /** The number of each character of [text]; [NumberKey.NOT_A_LETTER] where nothing is encrypted. */
    val codes: List<Int>,
    val hints: List<CryptogramHint>,
    override val createdAt: Long = 0,
    override val metadata: Map<String, String> = emptyMap(),
) : Puzzle {
    override val gameType: GameType get() = GameType.CRYPTOGRAM
}

/** Checks a puzzle before it is given to the player. An empty result means the puzzle is correct. */
object CryptogramValidator {

    fun validate(puzzle: CryptogramPuzzle): List<String> {
        val problems = mutableListOf<String>()
        val alphabet = CryptogramAlphabet.of(puzzle.language)
        val text = puzzle.text
        val codes = puzzle.codes

        if (text != CryptogramAlphabet.normalize(text, puzzle.language)) problems += "text is not normalized"
        if (text.none { it in alphabet }) problems += "text has no letters of the puzzle alphabet"
        if (text.length != codes.size) {
            problems += "text and codes differ in length"
            return problems
        }

        val codeToLetter = mutableMapOf<Int, Char>()
        val letterToCode = mutableMapOf<Char, Int>()
        for (i in text.indices) {
            val letter = text[i]
            val code = codes[i]
            if (letter !in alphabet) {
                if (code != NumberKey.NOT_A_LETTER) problems += "character '$letter' at $i must not be encrypted"
                continue
            }
            if (code !in 1..alphabet.length) {
                problems += "number $code at $i is outside 1..${alphabet.length}"
                continue
            }
            val knownLetter = codeToLetter.getOrPut(code) { letter }
            if (knownLetter != letter) problems += "number $code stands for both '$knownLetter' and '$letter'"
            val knownCode = letterToCode.getOrPut(letter) { code }
            if (knownCode != code) problems += "letter '$letter' is encrypted as both $knownCode and $code"
        }

        for (hint in puzzle.hints) {
            if (codeToLetter[hint.code] != hint.plain) problems += "hint ${hint.code} → ${hint.plain} is wrong"
        }
        if (puzzle.hints.map { it.code }.distinct().size != puzzle.hints.size) problems += "duplicate hints"
        if (puzzle.hints.size >= codeToLetter.size && codeToLetter.isNotEmpty()) problems += "hints reveal the whole text"

        return problems.distinct()
    }
}

/** Turns a plain phrase into a puzzle: normalizes, picks a key, encrypts, chooses hints and validates. */
class CryptogramGenerator(private val random: Random = Random.Default) {

    fun generate(
        id: String,
        text: String,
        language: Language,
        difficulty: Difficulty,
        createdAt: Long = 0,
    ): CryptogramPuzzle {
        val alphabet = CryptogramAlphabet.of(language)
        val plain = CryptogramAlphabet.normalize(text, language)
        val key = NumberKey.random(alphabet, random)
        val letters = plain.filter { it in alphabet }.toSet()

        val hints = letters.shuffled(random)
            .take(hintCount(difficulty, letters.size))
            .map { CryptogramHint(code = checkNotNull(key.numberOf(it)), plain = it) }
            .sortedBy { it.code }

        val puzzle = CryptogramPuzzle(
            id = id,
            language = language,
            difficulty = difficulty,
            text = plain,
            codes = key.encode(plain),
            hints = hints,
            createdAt = createdAt,
        )
        val problems = CryptogramValidator.validate(puzzle)
        require(problems.isEmpty()) { "Invalid cryptogram $id: $problems" }
        return puzzle
    }

    /** Easier levels open a larger share of the letters; at least one letter always stays hidden. */
    private fun hintCount(difficulty: Difficulty, distinctLetters: Int): Int {
        val count = when (difficulty) {
            Difficulty.EASY -> maxOf(3, distinctLetters * 30 / 100)
            Difficulty.NORMAL -> maxOf(2, distinctLetters * 20 / 100)
            Difficulty.HARD -> maxOf(1, distinctLetters * 10 / 100)
            Difficulty.EXPERT -> 1
        }
        return count.coerceIn(0, (distinctLetters - 1).coerceAtLeast(0))
    }
}
