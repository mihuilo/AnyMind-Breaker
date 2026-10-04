package com.anymindbreaker.feature.sudoku.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anymindbreaker.R
import com.anymindbreaker.core.ui.components.rememberPopScale
import com.anymindbreaker.core.ui.components.rememberShakeOffset
import com.anymindbreaker.core.ui.theme.Spacing
import com.anymindbreaker.feature.sudoku.domain.SudokuState

private const val SIZE = 9

@Composable
fun SudokuBoard(
    state: SudokuState,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val thinLine = MaterialTheme.colorScheme.outlineVariant
    val thickLine = MaterialTheme.colorScheme.onSurface
    val selected = state.selected
    val selectedValue = selected?.let { state.values[it] }?.takeIf { it != 0 }

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.small)
            .drawWithContent {
                drawContent()
                val cell = size.width / SIZE
                for (i in 0..SIZE) {
                    val isBoxEdge = i % 3 == 0
                    val color = if (isBoxEdge) thickLine else thinLine
                    val stroke = (if (isBoxEdge) 2.dp else 1.dp).toPx()
                    val position = (cell * i).coerceIn(stroke / 2, size.width - stroke / 2)
                    drawLine(color, Offset(position, 0f), Offset(position, size.height), stroke)
                    drawLine(color, Offset(0f, position), Offset(size.width, position), stroke)
                }
            },
    ) {
        for (row in 0 until SIZE) {
            Row(modifier = Modifier.weight(1f)) {
                for (col in 0 until SIZE) {
                    val index = row * SIZE + col
                    val value = state.values[index]
                    SudokuCell(
                        value = value,
                        isGiven = state.given[index],
                        isWrong = index in state.wrong,
                        shake = index == selected && index in state.wrong,
                        mistakes = state.mistakes,
                        description = stringResource(
                            R.string.sudoku_cell_description,
                            row + 1,
                            col + 1,
                            if (value == 0) stringResource(R.string.cell_empty) else value.toString(),
                        ),
                        background = cellBackground(
                            isSelected = index == selected,
                            isWrong = index in state.wrong,
                            isSameValue = selectedValue != null && value == selectedValue,
                            isPeer = selected != null && isPeer(index, selected),
                        ),
                        onClick = { onCellClick(index) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("sudoku_cell_$index"),
                    )
                }
            }
        }
    }
}

private fun isPeer(a: Int, b: Int): Boolean =
    a / SIZE == b / SIZE || a % SIZE == b % SIZE ||
        (a / SIZE / 3 == b / SIZE / 3 && a % SIZE / 3 == b % SIZE / 3)

@Composable
private fun cellBackground(
    isSelected: Boolean,
    isWrong: Boolean,
    isSameValue: Boolean,
    isPeer: Boolean,
): Color = when {
    isSelected -> MaterialTheme.colorScheme.primaryContainer
    isWrong -> MaterialTheme.colorScheme.errorContainer
    isSameValue -> MaterialTheme.colorScheme.secondaryContainer
    isPeer -> MaterialTheme.colorScheme.surfaceContainerHigh
    else -> MaterialTheme.colorScheme.surface
}

@Composable
private fun SudokuCell(
    value: Int,
    isGiven: Boolean,
    isWrong: Boolean,
    shake: Boolean,
    mistakes: Int,
    description: String,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shakeOffset by rememberShakeOffset(mistakes, shake)
    val popScale by rememberPopScale(value.takeIf { it != 0 })
    Box(
        modifier = modifier
            .background(background)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (value != 0) {
            Text(
                text = value.toString(),
                fontSize = 24.sp,
                fontWeight = if (isGiven) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    isWrong -> MaterialTheme.colorScheme.error
                    isGiven -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.graphicsLayer {
                    translationX = shakeOffset.dp.toPx()
                    scaleX = popScale
                    scaleY = popScale
                },
            )
        }
    }
}

/**
 * Digits 1–9 with the number of cells each digit still has to fill underneath.
 * A digit that is already placed nine times disappears and leaves its place empty.
 */
@Composable
fun SudokuNumberPad(
    state: SudokuState,
    onDigit: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        for (digit in 1..SIZE) {
            val remaining = state.remaining(digit)
            if (remaining == 0) {
                Spacer(Modifier.weight(1f))
            } else {
                Surface(
                    onClick = { onDigit(digit) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(vertical = 2.dp)
                        .testTag("sudoku_digit_$digit"),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    shadowElevation = 2.dp,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = digit.toString(),
                            fontSize = 28.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = remaining.toString(),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("sudoku_digit_${digit}_remaining"),
                        )
                    }
                }
            }
        }
    }
}
