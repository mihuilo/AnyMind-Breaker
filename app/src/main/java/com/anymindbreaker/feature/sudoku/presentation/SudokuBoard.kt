package com.anymindbreaker.feature.sudoku.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(background)
            .clickable(onClick = onClick),
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
            )
        }
    }
}

/** Digits 1–9. A digit that is already placed nine times is dimmed and disabled. */
@Composable
fun SudokuNumberPad(
    values: List<Int>,
    onDigit: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        for (digit in 1..SIZE) {
            val exhausted = values.count { it == digit } >= SIZE
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(enabled = !exhausted) { onDigit(digit) }
                    .testTag("sudoku_digit_$digit"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = digit.toString(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (exhausted) {
                        MaterialTheme.colorScheme.outlineVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
        }
    }
}
