package com.anymindbreaker.feature.cryptogram.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anymindbreaker.core.common.game.Language
import com.anymindbreaker.core.ui.theme.Spacing
import com.anymindbreaker.feature.cryptogram.domain.CryptogramAlphabet
import com.anymindbreaker.feature.cryptogram.domain.CryptogramState

private val CELL_WIDTH = 22.dp

@Composable
private fun answerColor(state: CryptogramState, cipher: Char, isRevealed: Boolean): Color = when {
    cipher in state.wrong && !isRevealed -> MaterialTheme.colorScheme.error
    cipher in state.locked || isRevealed -> MaterialTheme.colorScheme.onSurface
    else -> MaterialTheme.colorScheme.primary
}

/** The encrypted text: every letter has its own answer field above the cipher letter. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CryptogramText(
    state: CryptogramState,
    language: Language,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val alphabet = CryptogramAlphabet.of(language)
    val text = state.cipherText
    val selectedCipher = state.selected?.let { text[it] }

    // Words are kept whole so a line never breaks in the middle of a word.
    val words = buildList {
        var start = 0
        for (i in 0..text.length) {
            if (i == text.length || text[i] == ' ') {
                if (i > start) add(start until i)
                start = i + 1
            }
        }
    }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        for (word in words) {
            Row {
                for (index in word) {
                    val cipher = text[index]
                    if (cipher in alphabet) {
                        LetterCell(
                            answer = state.answerAt(index),
                            cipher = cipher,
                            answerColor = answerColor(state, cipher, index in state.revealed),
                            background = when {
                                index == state.selected -> MaterialTheme.colorScheme.primaryContainer
                                cipher == selectedCipher -> MaterialTheme.colorScheme.secondaryContainer
                                else -> Color.Transparent
                            },
                            onClick = { onSelect(index) },
                            modifier = Modifier.testTag("crypto_cell_$index"),
                        )
                    } else {
                        SymbolCell(symbol = cipher)
                    }
                }
            }
        }
    }
}

@Composable
private fun LetterCell(
    answer: Char?,
    cipher: Char,
    answerColor: Color,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(CELL_WIDTH)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(background)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = answer?.toString() ?: " ",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = answerColor,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
                .height(1.5.dp)
                .background(MaterialTheme.colorScheme.outline),
        )
        Text(
            text = cipher.toString(),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Punctuation and other characters that are not encrypted. */
@Composable
private fun SymbolCell(symbol: Char) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = symbol.toString(),
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        Spacer(Modifier.height(1.5.dp))
        Text(text = " ", fontSize = 12.sp)
    }
}

/** Table of correspondences: every cipher letter of the text with the answer given so far. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CryptogramMappingTable(
    state: CryptogramState,
    language: Language,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val alphabet = CryptogramAlphabet.of(language)
    val text = state.cipherText
    val selectedCipher = state.selected?.let { text[it] }
    val letters = text.filter { it in alphabet }.toSortedSet()

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        for (cipher in letters) {
            Row(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (cipher == selectedCipher) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                    )
                    .clickable { onSelect(text.indexOf(cipher)) }
                    .padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$cipher → ",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = (state.guesses[cipher] ?: '·').toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = answerColor(state, cipher, isRevealed = false),
                )
            }
        }
    }
}

private fun keyboardRows(language: Language): List<String> = when (language) {
    Language.RU -> listOf("ЙЦУКЕНГШЩЗХ", "ФЫВАПРОЛДЖЭ", "ЯЧСМИТЬБЮЪ")
    Language.EN -> listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")
}

/** On-screen keyboard with the letters of the puzzle alphabet. */
@Composable
fun LetterKeyboard(
    language: Language,
    /** Letters that already belong to a solved correspondence and cannot be used again. */
    solvedLetters: Set<Char>,
    /** Letters currently placed as unconfirmed guesses. */
    guessedLetters: Set<Char>,
    onLetter: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = keyboardRows(language)
    val longest = rows.maxOf { it.length }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        for (row in rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                // Shorter rows are centred while keeping the same key width.
                val side = (longest - row.length) / 2f
                if (side > 0f) Spacer(Modifier.weight(side))
                for (letter in row) {
                    val solved = letter in solvedLetters
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                if (letter in guessedLetters) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                            )
                            .clickable(enabled = !solved) { onLetter(letter) }
                            .testTag("crypto_key_$letter"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = letter.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (solved) {
                                MaterialTheme.colorScheme.outlineVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
                if (side > 0f) Spacer(Modifier.weight(side))
            }
        }
    }
}
