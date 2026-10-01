package com.anymindbreaker.feature.cryptogram

import com.anymindbreaker.core.common.game.DefaultScoreCalculator
import com.anymindbreaker.core.common.game.Difficulty
import com.anymindbreaker.core.common.game.GameResult
import com.anymindbreaker.core.common.game.Language
import com.anymindbreaker.feature.cryptogram.data.cryptogramAssetPath
import com.anymindbreaker.feature.cryptogram.data.parseCryptogramTexts
import com.anymindbreaker.feature.cryptogram.domain.CryptogramAction
import com.anymindbreaker.feature.cryptogram.domain.CryptogramAlphabet
import com.anymindbreaker.feature.cryptogram.domain.CryptogramGame
import com.anymindbreaker.feature.cryptogram.domain.CryptogramGenerator
import com.anymindbreaker.feature.cryptogram.domain.CryptogramHint
import com.anymindbreaker.feature.cryptogram.domain.CryptogramPuzzle
import com.anymindbreaker.feature.cryptogram.domain.CryptogramValidator
import com.anymindbreaker.feature.cryptogram.domain.SubstitutionKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class CryptogramCipherTest {

    private val ru = CryptogramAlphabet.of(Language.RU)
    private val en = CryptogramAlphabet.of(Language.EN)

    @Test
    fun alphabetsHaveExpectedSizeAndNoRepeats() {
        assertEquals(32, ru.toSet().size)
        assertEquals(32, ru.length)
        assertEquals(26, en.toSet().size)
        assertEquals(26, en.length)
    }

    @Test
    fun normalizeUppercasesAndFoldsYo() {
        assertEquals("ЕЛКА И ЕЖ", CryptogramAlphabet.normalize(" ёлка и Ёж ", Language.RU))
        assertEquals("HELLO, WORLD", CryptogramAlphabet.normalize("Hello, World", Language.EN))
    }

    @Test
    fun decryptRestoresEncryptedText() {
        repeat(50) { seed ->
            val key = SubstitutionKey.random(ru, Random(seed))
            val text = "ВОЛШЕБНИК, СТРАННЫЙ КОТ — 42!"
            assertEquals(text, key.decrypt(key.encrypt(text)))
            assertEquals(text, key.encrypt(key.decrypt(text)))
        }
    }

    @Test
    fun keyIsOneToOneAndHasNoFixedLetters() {
        repeat(50) { seed ->
            val key = SubstitutionKey.random(en, Random(seed))
            val encrypted = en.map { checkNotNull(key.cipherOf(it)) }
            assertEquals("different letters get different cipher letters", en.length, encrypted.toSet().size)
            assertTrue("cipher letters stay inside the alphabet", encrypted.all { it in en })
            assertTrue("no letter stands for itself", en.indices.none { en[it] == encrypted[it] })
        }
    }

    @Test
    fun sameLetterIsAlwaysEncryptedTheSameWay() {
        val key = SubstitutionKey.random(ru, Random(3))
        val encrypted = key.encrypt("КОТ КОТ ТОК")
        assertEquals(encrypted.substring(0, 3), encrypted.substring(4, 7))
        assertEquals(encrypted[0], encrypted[10])
    }

    @Test
    fun spacesPunctuationAndForeignLettersArePreserved() {
        val key = SubstitutionKey.random(ru, Random(1))
        val encrypted = key.encrypt("ДА-НЕТ, OK? 7!")
        assertEquals("ДА-НЕТ, OK? 7!".length, encrypted.length)
        for (i in encrypted.indices) {
            val original = "ДА-НЕТ, OK? 7!"[i]
            if (original !in ru) assertEquals(original, encrypted[i])
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun keyRejectsTwoLettersWithSameCipher() {
        SubstitutionKey(mapOf('A' to 'X', 'B' to 'X'))
    }
}

class CryptogramGeneratorTest {

    private fun generate(text: String, language: Language, difficulty: Difficulty, seed: Int = 1) =
        CryptogramGenerator(Random(seed)).generate("test", text, language, difficulty)

    @Test
    fun generatedPuzzlePassesValidation() {
        for (difficulty in Difficulty.entries) {
            repeat(10) { seed ->
                val puzzle = generate("Волшебник — странный кот, ёж!", Language.RU, difficulty, seed)
                assertEquals(emptyList<String>(), CryptogramValidator.validate(puzzle))
                assertEquals("ВОЛШЕБНИК — СТРАННЫЙ КОТ, ЕЖ!", puzzle.text)
                assertNotEquals(puzzle.text, puzzle.cipherText)
            }
        }
    }

    @Test
    fun easierLevelsGetMoreHints() {
        val text = "The quick brown fox jumps over the lazy dog"
        val counts = Difficulty.entries.map { generate(text, Language.EN, it).hints.size }
        assertEquals(counts.sortedDescending(), counts)
        assertTrue(counts.first() > counts.last())
        assertEquals(1, counts.last())
    }

    @Test
    fun hintsNeverRevealTheWholeText() {
        val puzzle = generate("Да", Language.RU, Difficulty.EASY)
        assertEquals(1, puzzle.hints.size)
    }

    @Test
    fun hintsAreCorrectCorrespondences() {
        val puzzle = generate("Practice makes perfect.", Language.EN, Difficulty.EASY)
        for (hint in puzzle.hints) {
            val index = puzzle.cipherText.indexOf(hint.cipher)
            assertEquals(hint.plain, puzzle.text[index])
        }
    }

    @Test
    fun validatorReportsBrokenPuzzles() {
        val good = generate("Кот и кит", Language.RU, Difficulty.HARD)
        fun problems(puzzle: CryptogramPuzzle) = CryptogramValidator.validate(puzzle)

        assertTrue(problems(good.copy(cipherText = good.cipherText.dropLast(1))).isNotEmpty())
        assertTrue(problems(good.copy(cipherText = good.cipherText.replace(' ', 'А'))).isNotEmpty())
        assertTrue(problems(good.copy(hints = listOf(CryptogramHint(good.cipherText[0], 'Я')))).isNotEmpty())
        assertTrue(problems(good.copy(text = good.text.lowercase())).isNotEmpty())

        // К is encrypted in two different ways.
        val conflicting = good.cipherText.toCharArray().also { it[6] = if (it[0] == 'Б') 'В' else 'Б' }
        assertTrue(problems(good.copy(cipherText = String(conflicting))).isNotEmpty())
    }
}

class CryptogramContentTest {

    private fun load(language: Language) =
        parseCryptogramTexts(File("src/main/assets/" + cryptogramAssetPath(language)).readText())

    @Test
    fun everyBundledPhraseProducesValidPuzzle() {
        for (language in Language.entries) {
            val texts = load(language)
            val alphabet = CryptogramAlphabet.of(language)
            assertEquals("ids are unique", texts.size, texts.map { it.id }.toSet().size)
            for (text in texts) {
                val puzzle = CryptogramGenerator(Random(1)).generate(text.id, text.text, language, text.difficulty)
                assertEquals(text.id, emptyList<String>(), CryptogramValidator.validate(puzzle))
                val foreign = puzzle.text.filter { it.isLetter() && it !in alphabet }
                assertEquals("${text.id} has letters outside the alphabet", "", foreign)
                val longestWord = puzzle.text.split(' ').maxOf { word -> word.count { it in alphabet } }
                assertTrue("${text.id} has a word too long for the screen", longestWord <= 14)
            }
        }
    }

    @Test
    fun everyLanguageHasPhrasesForEveryDifficulty() {
        for (language in Language.entries) {
            val byDifficulty = load(language).groupBy { it.difficulty }
            for (difficulty in Difficulty.entries) {
                assertTrue("$language $difficulty", (byDifficulty[difficulty]?.size ?: 0) >= 5)
            }
        }
    }

    @Test
    fun harderPhrasesAreLonger() {
        for (language in Language.entries) {
            val average = load(language).groupBy { it.difficulty }
                .mapValues { (_, texts) -> texts.map { it.text.length }.average() }
            val ordered = Difficulty.entries.map { average.getValue(it) }
            assertEquals(ordered.sorted(), ordered)
        }
    }
}

class CryptogramGameTest {

    // КОТ И ТОК → cipher letters: К→А, О→Б, Т→В, И→Г
    private val puzzle = CryptogramPuzzle(
        id = "test",
        language = Language.RU,
        difficulty = Difficulty.EASY,
        text = "КОТ И ТОК",
        cipherText = "АБВ Г ВБА",
        hints = listOf(CryptogramHint(cipher = 'Г', plain = 'И')),
    )

    private fun game(lives: Int? = null) = CryptogramGame(puzzle, lives, DefaultScoreCalculator()).also { it.start() }

    private fun CryptogramGame.enter(position: Int, letter: Char) {
        handleAction(CryptogramAction.SelectPosition(position))
        handleAction(CryptogramAction.InputLetter(letter))
    }

    @Test
    fun startShowsInitialHintsAndSelectsFirstOpenLetter() {
        val state = game().getState()
        assertEquals('И', state.answerAt(4))
        assertTrue('Г' in state.locked)
        assertEquals(0, state.selected)
        assertNull(state.answerAt(0))
    }

    @Test
    fun correctLetterAppearsEverywhereAndMovesSelection() {
        val game = game()
        game.enter(0, 'К')
        val state = game.getState()
        assertEquals('К', state.answerAt(0))
        assertEquals('К', state.answerAt(8))
        assertEquals(0, state.mistakes)
        assertEquals(1, state.selected)
    }

    @Test
    fun wrongLetterIsMarkedAndCounted() {
        val game = game()
        game.enter(0, 'Я')
        val state = game.getState()
        assertEquals('Я', state.answerAt(0))
        assertEquals(setOf('А'), state.wrong)
        assertEquals(1, state.mistakes)
        assertEquals(0, state.selected)

        game.handleAction(CryptogramAction.InputLetter('К'))
        assertTrue(game.getState().wrong.isEmpty())
        assertEquals(1, game.getState().mistakes)
    }

    @Test
    fun plainLetterBelongsToOnlyOneCipherLetter() {
        val game = game()
        game.enter(0, 'Т')
        game.enter(1, 'Т')
        val state = game.getState()
        assertNull(state.answerAt(0))
        assertEquals('Т', state.answerAt(1))
        assertEquals(setOf('Б'), state.wrong)
    }

    @Test
    fun solvedAndHintedLettersCannotBeChanged() {
        val game = game()
        game.enter(0, 'К')
        game.enter(0, 'Я')
        game.enter(4, 'Я')
        game.handleAction(CryptogramAction.SelectPosition(0))
        game.handleAction(CryptogramAction.Erase)
        val state = game.getState()
        assertEquals('К', state.answerAt(0))
        assertEquals('И', state.answerAt(4))
        assertEquals(0, state.mistakes)
    }

    @Test
    fun letterOfSolvedCorrespondenceCannotBeReused() {
        val game = game()
        game.enter(0, 'К')
        game.enter(1, 'К')
        assertNull(game.getState().answerAt(1))
        assertEquals(0, game.getState().mistakes)
    }

    @Test
    fun eraseRemovesWrongGuess() {
        val game = game()
        game.enter(0, 'Я')
        game.handleAction(CryptogramAction.Erase)
        assertNull(game.getState().answerAt(0))
        assertTrue(game.getState().wrong.isEmpty())
    }

    @Test
    fun charactersOutsideTheAlphabetAreIgnored() {
        val game = game()
        game.handleAction(CryptogramAction.SelectPosition(3))
        assertEquals(0, game.getState().selected)
        game.handleAction(CryptogramAction.InputLetter('Q'))
        assertEquals(0, game.getState().entries)
    }

    @Test
    fun solvingAllLettersCompletesTheGame() {
        val game = game()
        game.enter(0, 'К')
        game.enter(1, 'О')
        assertFalse(game.isFinished())
        game.enter(2, 'Т')
        assertEquals(GameResult.COMPLETED, game.getState().result)
        assertTrue(game.calculateScore() > 0)
    }

    @Test
    fun losingAllLivesFailsTheGame() {
        val game = game(lives = 2)
        game.enter(0, 'Я')
        assertEquals(1, game.getState().livesLeft)
        assertFalse(game.isFinished())
        game.enter(0, 'Ю')
        assertEquals(0, game.getState().livesLeft)
        assertEquals(GameResult.FAILED, game.getState().result)
        assertEquals(0, game.calculateScore())

        game.enter(0, 'К')
        assertEquals('Ю', game.getState().answerAt(0))
    }

    @Test
    fun letterHintOpensCorrespondenceEverywhere() {
        val game = game()
        game.enter(0, 'Я')
        game.handleAction(CryptogramAction.HintLetter)
        val state = game.getState()
        assertEquals('К', state.answerAt(0))
        assertEquals('К', state.answerAt(8))
        assertTrue(state.wrong.isEmpty())
        assertEquals(1, state.hintsUsed)
    }

    @Test
    fun letterHintTakesLetterAwayFromWrongGuess() {
        val game = game()
        game.enter(1, 'К')
        game.handleAction(CryptogramAction.SelectPosition(0))
        game.handleAction(CryptogramAction.HintLetter)
        val state = game.getState()
        assertEquals('К', state.answerAt(0))
        assertNull(state.answerAt(1))
        assertTrue(state.wrong.isEmpty())
    }

    @Test
    fun wordHintSolvesWholeWordAndCountsOnce() {
        val game = game()
        game.handleAction(CryptogramAction.SelectPosition(1))
        game.handleAction(CryptogramAction.HintWord)
        val state = game.getState()
        assertEquals("КОТ", (0..2).map { state.answerAt(it) }.joinToString(""))
        assertEquals(1, state.hintsUsed)
        assertEquals(GameResult.COMPLETED, state.result)
    }

    @Test
    fun positionHintOpensOnlyOnePlace() {
        val game = game()
        game.handleAction(CryptogramAction.SelectPosition(8))
        game.handleAction(CryptogramAction.HintPosition)
        val state = game.getState()
        assertEquals('К', state.answerAt(8))
        assertNull(state.answerAt(0))
        assertEquals(1, state.hintsUsed)
        assertFalse(game.isFinished())
    }

    @Test
    fun hintsLowerTheScore() {
        val clean = game().also {
            it.enter(0, 'К')
            it.enter(1, 'О')
            it.enter(2, 'Т')
        }
        val hinted = game().also {
            it.enter(0, 'К')
            it.enter(1, 'О')
            it.handleAction(CryptogramAction.HintLetter)
        }
        assertTrue(hinted.isFinished())
        assertTrue(hinted.calculateScore() < clean.calculateScore())
    }

    @Test
    fun tickAdvancesTimer() {
        val game = game()
        repeat(5) { game.handleAction(CryptogramAction.Tick) }
        assertEquals(5, game.getState().elapsedSeconds)
    }
}
