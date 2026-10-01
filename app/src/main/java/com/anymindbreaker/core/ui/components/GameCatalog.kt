package com.anymindbreaker.core.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.anymindbreaker.R
import com.anymindbreaker.core.common.game.GameType
import com.anymindbreaker.core.ui.theme.Spacing

// How each game is presented in lists. A new game adds its entries here and nowhere else.

@StringRes
fun GameType.titleRes(): Int = when (this) {
    GameType.CRYPTOGRAM -> R.string.game_cryptogram
    GameType.SUDOKU -> R.string.game_sudoku
}

@StringRes
fun GameType.descriptionRes(): Int = when (this) {
    GameType.CRYPTOGRAM -> R.string.game_cryptogram_description
    GameType.SUDOKU -> R.string.game_sudoku_description
}

@DrawableRes
fun GameType.iconRes(): Int = when (this) {
    GameType.CRYPTOGRAM -> R.drawable.ic_cryptogram
    GameType.SUDOKU -> R.drawable.ic_sudoku
}

/** Cards of all available games. */
@Composable
fun GameList(
    onOpenGame: (GameType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        for (game in GameType.entries) {
            GameCard(
                title = stringResource(game.titleRes()),
                description = stringResource(game.descriptionRes()),
                iconRes = game.iconRes(),
                onClick = { onOpenGame(game) },
                modifier = Modifier.testTag("game_card_${game.name}"),
            )
        }
    }
}
