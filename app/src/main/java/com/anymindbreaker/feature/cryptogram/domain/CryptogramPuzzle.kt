package com.anymindbreaker.feature.cryptogram.domain

import com.anymindbreaker.core.common.game.Difficulty
import com.anymindbreaker.core.common.game.GameType
import com.anymindbreaker.core.common.game.Language
import com.anymindbreaker.core.common.game.Puzzle
import kotlinx.serialization.Serializable
import kotlin.random.Random

/** A correspondence shown to the player before the game starts. */
@Serializable
data class CryptogramHint(val cipher: Char, val plain: Char)

@Serializable
data class CryptogramPuzzle(
    override val id: String,
    override val language: Language,
    override val difficulty: Difficulty,
    /** Normalized plain text. */
    val text: String,
    val cipherText: String,
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
        val cipher = puzzle.cipherText

        if (text != CryptogramAlphabet.normalize(text, puzzle.language)) problems += "text is not normalized"
        if (text.none { it in alphabet }) problems += "text has no letters of the puzzle alphabet"
        if (text.length != cipher.length) {
            problems += "text and cipher text differ in length"
            return problems
        }

        val cipherToPlain = mutableMapOf<Char, Char>()
        val plainToCipher = mutableMapOf<Char, Char>()
        for (i in text.indices) {
            val plain = text[i]
            val encrypted = cipher[i]
            if (plain !in alphabet) {
                if (encrypted != plain) problems += "character '$plain' at $i is not preserved"
                continue
            }
            if (encrypted !in alphabet) {
                problems += "cipher character '$encrypted' at $i is not a letter"
                continue
            }
            val knownPlain = cipherToPlain.getOrPut(encrypted) { plain }
            if (knownPlain != plain) problems += "cipher letter '$encrypted' stands for both '$knownPlain' and '$plain'"
            val knownCipher = plainToCipher.getOrPut(plain) { encrypted }
            if (knownCipher != encrypted) problems += "plain letter '$plain' is encrypted as both '$knownCipher' and '$encrypted'"
        }

        for (hint in puzzle.hints) {
            if (cipherToPlain[hint.cipher] != hint.plain) problems += "hint ${hint.cipher} → ${hint.plain} is wrong"
        }
        if (puzzle.hints.map { it.cipher }.distinct().size != puzzle.hints.size) problems += "duplicate hints"
        if (puzzle.hints.size >= cipherToPlain.size && cipherToPlain.isNotEmpty()) problems += "hints reveal the whole text"

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
        val key = SubstitutionKey.random(alphabet, random)
        val letters = plain.filter { it in alphabet }.toSet()

        val hints = letters.shuffled(random)
            .take(hintCount(difficulty, letters.size))
            .map { CryptogramHint(cipher = checkNotNull(key.cipherOf(it)), plain = it) }
            .sortedBy { it.cipher }

        val puzzle = CryptogramPuzzle(
            id = id,
            language = language,
            difficulty = difficulty,
            text = plain,
            cipherText = key.encrypt(plain),
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
