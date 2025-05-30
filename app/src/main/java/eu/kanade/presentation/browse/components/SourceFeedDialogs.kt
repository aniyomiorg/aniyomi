package eu.kanade.presentation.browse.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.presentation.components.TabbedDialogPaddings
import eu.kanade.presentation.more.settings.LocalPreferenceMinHeight
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import tachiyomi.domain.source.anime.model.FeedSavedSearch
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun SourceFeedAddDialog(
    onDismissRequest: () -> Unit,
    name: String,
    addFeed: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = addFeed) {
                Text(text = stringResource(MR.strings.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        title = {
            Text(text = stringResource(TLMR.strings.feed))
        },
        text = {
            Text(text = stringResource(TLMR.strings.feed_add, name))
        },
    )
}

@Composable
fun SourceFeedDeleteDialog(
    onDismissRequest: () -> Unit,
    deleteFeed: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = deleteFeed) {
                Text(text = stringResource(MR.strings.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        title = {
            Text(text = stringResource(TLMR.strings.feed))
        },
        text = {
            Text(text = stringResource(TLMR.strings.feed_delete))
        },
    )
}

// KMK -->
private val PaddingSize = 16.dp

private val ButtonPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
private val TitlePadding = PaddingValues(bottom = 16.dp, top = 8.dp)

@Composable
fun FeedActionsDialog(
    feed: FeedSavedSearch,
    title: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onDismissRequest: () -> Unit,
    onClickDelete: (FeedSavedSearch) -> Unit,
    onMoveUp: (FeedSavedSearch) -> Unit,
    onMoveDown: (FeedSavedSearch) -> Unit,
    modifier: Modifier = Modifier,
) {
    val minHeight = LocalPreferenceMinHeight.current

    AdaptiveSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier
                .padding(
                    vertical = TabbedDialogPaddings.Vertical,
                    horizontal = TabbedDialogPaddings.Horizontal,
                )
                .fillMaxWidth(),
        ) {
            Text(
                modifier = Modifier.padding(TitlePadding),
                text = title,
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(Modifier.height(PaddingSize))

            if (canMoveUp) {
                TextPreferenceWidget(
                    title = stringResource(TLMR.strings.action_move_up),
                    icon = Icons.Outlined.ArrowUpward,
                    onPreferenceClick = {
                        onDismissRequest()
                        onMoveUp(feed)
                    },
                )

                HorizontalDivider()
            }

            if (canMoveDown) {
                TextPreferenceWidget(
                    title = stringResource(TLMR.strings.action_move_down),
                    icon = Icons.Outlined.ArrowDownward,
                    onPreferenceClick = {
                        onDismissRequest()
                        onMoveDown(feed)
                    },
                )

                HorizontalDivider()
            }

            TextPreferenceWidget(
                title = stringResource(MR.strings.action_delete),
                icon = Icons.Outlined.Delete,
                onPreferenceClick = {
                    onDismissRequest()
                    onClickDelete(feed)
                },
            )

            Row(
                modifier = Modifier
                    .sizeIn(minHeight = minHeight)
                    .clickable { onDismissRequest.invoke() }
                    .padding(ButtonPadding)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                OutlinedButton(onClick = onDismissRequest, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier
                            .padding(vertical = 8.dp),
                        text = stringResource(MR.strings.action_cancel),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun FeedActionsDialogPreview() {
    FeedActionsDialog(
        feed = FeedSavedSearch(
            id = 1,
            source = 1,
            savedSearch = null,
            global = false,
            feedOrder = 0,
        ),
        title = "Feed 1",
        canMoveUp = true,
        canMoveDown = true,
        onDismissRequest = { },
        onClickDelete = { },
        onMoveUp = { },
        onMoveDown = { },
    )
}

@Composable
fun FeedSortAlphabeticallyDialog(
    onDismissRequest: () -> Unit,
    onSort: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                onSort()
                onDismissRequest()
            }) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        title = {
            Text(text = stringResource(TLMR.strings.action_sort_feed))
        },
        text = {
            Text(text = stringResource(TLMR.strings.sort_feed_confirmation))
        },
    )
}
// KMK <--
