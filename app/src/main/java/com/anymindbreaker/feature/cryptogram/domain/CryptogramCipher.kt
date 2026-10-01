package com.anymindbreaker.feature.cryptogram.domain

import com.anymindbreaker.core.common.game.Language
import java.util.Locale
import kotlin.random.Random

/** Alphabets are handled separately: a puzzle only substitutes letters of its own language. */
object CryptogramAlphabet {
    private const val RU = "АБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"
    private const val EN = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

    fun of(language: Language): String = when (language) {
        Language.RU -> RU
        Language.EN -> EN
    }

    /** Case is ignored, and Ё is treated as Е so the Russian alphabet has 32 letters. */
    fun normalize(text: String, language: Language): String {
        val upper = text.trim().uppercase(Locale.ROOT)
        return if (language == Language.RU) upper.replace('Ё', 'Е') else upper
    }
}

/** A monoalphabetic substitution: every plain letter maps to exactly one cipher letter. */
class SubstitutionKey(plainToCipher: Map<Char, Char>) {

    private val plainToCipher: Map<Char, Char> = plainToCipher.toMap()
    private val cipherToPlain: Map<Char, Char> = plainToCipher.entries.associate { (plain, cipher) -> cipher to plain }

    init {
        require(cipherToPlain.size == plainToCipher.size) { "Two plain letters share a cipher letter" }
    }

    /** Characters outside the key (spaces, punctuation, hyphens) are kept as they are. */
    fun encrypt(text: String): String = text.map { plainToCipher[it] ?: it }.joinToString("")

    fun decrypt(text: String): String = text.map { cipherToPlain[it] ?: it }.joinToString("")

    fun cipherOf(plain: Char): Char? = plainToCipher[plain]

    companion object {
        /** A random permutation of [alphabet] in which no letter stands for itself. */
        fun random(alphabet: String, random: Random): SubstitutionKey {
            val letters = alphabet.toList()
            var shuffled: List<Char>
            do {
                shuffled = letters.shuffled(random)
            } while (letters.indices.any { letters[it] == shuffled[it] })
            return SubstitutionKey(letters.zip(shuffled).toMap())
        }
    }
}
