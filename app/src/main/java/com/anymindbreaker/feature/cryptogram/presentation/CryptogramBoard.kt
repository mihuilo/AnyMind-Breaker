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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anymindbreaker.R
import com.anymindbreaker.core.common.game.Language
import com.anymindbreaker.core.ui.components.rememberPopScale
import com.anymindbreaker.core.ui.components.rememberShakeOffset
import com.anymindbreaker.core.ui.theme.Spacing
import com.anymindbreaker.feature.cryptogram.domain.CryptogramState

private val CELL_WIDTH = 22.dp

@Composable
private fun answerColor(state: CryptogramState, code: Int, isRevealed: Boolean): Color = when {
    code in state.wrong && !isRevealed -> MaterialTheme.colorScheme.error
    code in state.locked || isRevealed -> MaterialTheme.colorScheme.onSurface
    else -> MaterialTheme.colorScheme.primary
}

/** The encrypted text: every letter has its own answer field above the number that hides it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CryptogramText(
    state: CryptogramState,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val template = state.template
    val selectedCode = state.selected?.let { state.codes[it] }

    // Words are kept whole so a line never breaks in the middle of a word.
    val words = buildList {
        var start = 0
        for (i in 0..template.length) {
            if (i == template.length || template[i] == ' ') {
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
                    val code = state.codes[index]
                    if (state.isLetter(index)) {
                        LetterCell(
                            answer = state.answerAt(index),
                            code = code,
                            answerColor = answerColor(state, code, index in state.revealed),
                            shake = code == selectedCode && code in state.wrong,
                            mistakes = state.mistakes,
                            background = when {
                                index == state.selected -> MaterialTheme.colorScheme.primaryContainer
                                code == selectedCode -> MaterialTheme.colorScheme.secondaryContainer
                                else -> Color.Transparent
                            },
                            onClick = { onSelect(index) },
                            modifier = Modifier.testTag("crypto_cell_$index"),
                        )
                    } else {
                        SymbolCell(symbol = template[index])
                    }
                }
            }
        }
    }
}

@Composable
private fun LetterCell(
    answer: Char?,
    code: Int,
    answerColor: Color,
    shake: Boolean,
    mistakes: Int,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shakeOffset by rememberShakeOffset(mistakes, shake)
    val popScale by rememberPopScale(answer)
    val description = stringResource(
        R.string.cryptogram_cell_description,
        code,
        answer?.toString() ?: stringResource(R.string.cell_empty),
    )
    Column(
        modifier = modifier
            .width(CELL_WIDTH)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(background)
            .clickable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = answer?.toString() ?: " ",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = answerColor,
            modifier = Modifier.graphicsLayer {
                translationX = shakeOffset.dp.toPx()
                scaleX = popScale
                scaleY = popScale
            },
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
                .height(1.5.dp)
                .background(MaterialTheme.colorScheme.outline),
        )
        Text(
            text = code.toString(),
            fontSize = 12.sp,
            maxLines = 1,
            softWrap = false,
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

/** Table of correspondences: every number of the text with the letter given so far. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CryptogramMappingTable(
    state: CryptogramState,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedCode = state.selected?.let { state.codes[it] }
    val numbers = state.codes.indices.filter(state::isLetter).map { state.codes[it] }.toSortedSet()

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        for (number in numbers) {
            Row(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (number == selectedCode) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                    )
                    .clickable { onSelect(state.codes.indexOf(number)) }
                    .padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$number → ",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = (state.guesses[number] ?: '·').toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = answerColor(state, number, isRevealed = false),
                )
            }
        }
    }
}

private fun keyboardRows(language: Language): List<String> = when (language) {
    Language.RU -> listOf("ЙЦУКЕНГШЩЗХ", "ФЫВАПРОЛДЖЭ", "ЯЧСМИТЬБЮЪЁ")
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
