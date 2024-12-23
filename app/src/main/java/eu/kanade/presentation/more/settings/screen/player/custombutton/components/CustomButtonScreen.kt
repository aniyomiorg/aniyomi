package eu.kanade.presentation.more.settings.screen.player.custombutton.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.more.settings.screen.player.custombutton.CustomButtonScreenState
import tachiyomi.domain.custombuttons.model.CustomButton
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.util.plus

@Composable
fun CustomButtonScreen(
    state: CustomButtonScreenState.Success,
    primaryCustomButtonId: Long,
    onClickCreate: () -> Unit,
    onClickPrimary: (CustomButton) -> Unit,
    onClickEdit: (CustomButton) -> Unit,
    onClickDelete: (CustomButton) -> Unit,
    onClickMoveUp: (CustomButton) -> Unit,
    onClickMoveDown: (CustomButton) -> Unit,
    navigateUp: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(MR.strings.pref_player_custom_button_header),
                navigateUp = navigateUp,
                // actions = {
                //     TODO: link to website
                // },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            CustomButtonFloatingActionButton(
                lazyListState = lazyListState,
                onCreate = onClickCreate,
            )
        },
    ) { paddingValues ->
        if (state.isEmpty) {
            EmptyScreen(
                stringRes = MR.strings.pref_player_custom_button_empty,
                modifier = Modifier.padding(paddingValues),
            )
            return@Scaffold
        }

        CustomButtonContent(
            customButtons = state.customButtons,
            primaryCustomButtonId = primaryCustomButtonId,
            lazyListState = lazyListState,
            paddingValues = paddingValues +
                topSmallPaddingValues +
                PaddingValues(horizontal = MaterialTheme.padding.medium),
            onClickPrimary = onClickPrimary,
            onClickEdit = onClickEdit,
            onClickDelete = onClickDelete,
            onMoveUp = onClickMoveUp,
            onMoveDown = onClickMoveDown,
        )
    }
}

@Composable
private fun CustomButtonContent(
    customButtons: List<CustomButton>,
    primaryCustomButtonId: Long,
    lazyListState: LazyListState,
    paddingValues: PaddingValues,
    onClickPrimary: (CustomButton) -> Unit,
    onClickEdit: (CustomButton) -> Unit,
    onClickDelete: (CustomButton) -> Unit,
    onMoveUp: (CustomButton) -> Unit,
    onMoveDown: (CustomButton) -> Unit,
) {
    LazyColumn(
        state = lazyListState,
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        itemsIndexed(
            items = customButtons,
            key = { _, customButton -> "customButton-${customButton.id}" },
        ) { index, customButton ->
            CustomButtonListItem(
                modifier = Modifier.animateItem(),
                customButton = customButton,
                canMoveUp = index != 0,
                canMoveDown = index != customButtons.lastIndex,
                isPrimary = customButton.id == primaryCustomButtonId,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onTogglePrimary = { onClickPrimary(customButton) },
                onEdit = { onClickEdit(customButton) },
                onDelete = { onClickDelete(customButton) },
            )
        }
    }
}
