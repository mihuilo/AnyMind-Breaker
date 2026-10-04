package com.anymindbreaker.feature.cryptogram.domain

import com.anymindbreaker.core.common.game.Language
import java.util.Locale
import kotlin.random.Random

/** Alphabets are handled separately: a puzzle only encrypts letters of its own language. */
object CryptogramAlphabet {
    private const val RU = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"
    private const val EN = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

    fun of(language: Language): String = when (language) {
        Language.RU -> RU
        Language.EN -> EN
    }

    /** Case is ignored: the text is trimmed and upper-cased. */
    fun normalize(text: String, language: Language): String = text.trim().uppercase(Locale.ROOT)
}

/**
 * The cipher of one game: every letter of the alphabet gets its own number from 1 to the
 * alphabet size, in an order that is random for each game.
 */
class NumberKey(letterToNumber: Map<Char, Int>) {

    private val letterToNumber: Map<Char, Int> = letterToNumber.toMap()
    private val numberToLetter: Map<Int, Char> = letterToNumber.entries.associate { (letter, number) -> number to letter }

    init {
        require(numberToLetter.size == letterToNumber.size) { "Two letters share a number" }
        require(NOT_A_LETTER !in numberToLetter) { "$NOT_A_LETTER is reserved for characters that are not encrypted" }
    }

    /** One number per character; spaces, punctuation and other characters give [NOT_A_LETTER]. */
    fun encode(text: String): List<Int> = text.map { letterToNumber[it] ?: NOT_A_LETTER }

    /** Restores the text. Characters that were not encrypted are taken from [template]. */
    fun decode(codes: List<Int>, template: String): String =
        codes.mapIndexed { index, code -> numberToLetter[code] ?: template[index] }.joinToString("")

    fun numberOf(letter: Char): Int? = letterToNumber[letter]

    companion object {
        const val NOT_A_LETTER = 0

        fun random(alphabet: String, random: Random): NumberKey =
            NumberKey(alphabet.toList().zip((1..alphabet.length).shuffled(random)).toMap())
    }
}

/** The text with every encrypted letter hidden; what is left is shown to the player as is. */
fun cryptogramTemplate(text: String, alphabet: String): String =
    text.map { if (it in alphabet) HIDDEN_LETTER else it }.joinToString("")

const val HIDDEN_LETTER = '_'
