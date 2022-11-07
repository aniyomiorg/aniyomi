package eu.kanade.presentation.animebrowse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.domain.animesource.interactor.GetRemoteAnime
import eu.kanade.domain.animesource.model.AnimeSource
import eu.kanade.domain.animesource.model.Pin
import eu.kanade.presentation.animebrowse.components.BaseAnimeSourceItem
import eu.kanade.presentation.components.EmptyScreen
import eu.kanade.presentation.components.LoadingScreen
import eu.kanade.presentation.components.ScrollbarLazyColumn
import eu.kanade.presentation.theme.header
import eu.kanade.presentation.util.horizontalPadding
import eu.kanade.presentation.util.plus
import eu.kanade.presentation.util.topPaddingValues
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.LocalAnimeSource
import eu.kanade.tachiyomi.ui.browse.animesource.AnimeSourcesPresenter
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AnimeSourcesScreen(
    presenter: AnimeSourcesPresenter,
    contentPadding: PaddingValues,
    onClickItem: (AnimeSource, String) -> Unit,
    onClickDisable: (AnimeSource) -> Unit,
    onClickPin: (AnimeSource) -> Unit,
) {
    val context = LocalContext.current
    when {
        presenter.isLoading -> LoadingScreen()
        presenter.isEmpty -> EmptyScreen(
            textResource = R.string.source_empty_screen,
            modifier = Modifier.padding(contentPadding),
        )
        else -> {
            AnimeSourceList(
                state = presenter,
                contentPadding = contentPadding,
                onClickItem = onClickItem,
                onClickDisable = onClickDisable,
                onClickPin = onClickPin,
            )
        }
    }
    LaunchedEffect(Unit) {
        presenter.events.collectLatest { event ->
            when (event) {
                AnimeSourcesPresenter.Event.FailedFetchingSources -> {
                    context.toast(R.string.internal_error)
                }
            }
        }
    }
}

@Composable
private fun AnimeSourceList(
    state: AnimeSourcesState,
    contentPadding: PaddingValues,
    onClickItem: (AnimeSource, String) -> Unit,
    onClickDisable: (AnimeSource) -> Unit,
    onClickPin: (AnimeSource) -> Unit,
) {
    ScrollbarLazyColumn(
        contentPadding = contentPadding + topPaddingValues,
    ) {
        items(
            items = state.items,
            contentType = {
                when (it) {
                    is AnimeSourceUiModel.Header -> "header"
                    is AnimeSourceUiModel.Item -> "item"
                }
            },
            key = {
                when (it) {
                    is AnimeSourceUiModel.Header -> it.hashCode()
                    is AnimeSourceUiModel.Item -> "source-${it.source.key()}"
                }
            },
        ) { model ->
            when (model) {
                is AnimeSourceUiModel.Header -> {
                    AnimeSourceHeader(
                        modifier = Modifier.animateItemPlacement(),
                        language = model.language,
                    )
                }
                is AnimeSourceUiModel.Item -> AnimeSourceItem(
                    modifier = Modifier.animateItemPlacement(),
                    source = model.source,
                    onClickItem = onClickItem,
                    onLongClickItem = { state.dialog = AnimeSourcesPresenter.Dialog(it) },
                    onClickPin = onClickPin,
                )
            }
        }
    }

    if (state.dialog != null) {
        val source = state.dialog!!.source
        AnimeSourceOptionsDialog(
            source = source,
            onClickPin = {
                onClickPin(source)
                state.dialog = null
            },
            onClickDisable = {
                onClickDisable(source)
                state.dialog = null
            },
            onDismiss = { state.dialog = null },
        )
    }
}

@Composable
private fun AnimeSourceHeader(
    modifier: Modifier = Modifier,
    language: String,
) {
    val context = LocalContext.current
    Text(
        text = LocaleHelper.getSourceDisplayName(language, context),
        modifier = modifier
            .padding(horizontal = horizontalPadding, vertical = 8.dp),
        style = MaterialTheme.typography.header,
    )
}

@Composable
private fun AnimeSourceItem(
    modifier: Modifier = Modifier,
    source: AnimeSource,
    onClickItem: (AnimeSource, String) -> Unit,
    onLongClickItem: (AnimeSource) -> Unit,
    onClickPin: (AnimeSource) -> Unit,
) {
    BaseAnimeSourceItem(
        modifier = modifier,
        source = source,
        onClickItem = { onClickItem(source, GetRemoteAnime.QUERY_POPULAR) },
        onLongClickItem = { onLongClickItem(source) },
        action = {
            if (source.supportsLatest) {
                TextButton(onClick = { onClickItem(source, GetRemoteAnime.QUERY_LATEST) }) {
                    Text(
                        text = stringResource(id = R.string.latest),
                        style = LocalTextStyle.current.copy(
                            color = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
            AnimeSourcePinButton(
                isPinned = Pin.Pinned in source.pin,
                onClick = { onClickPin(source) },
            )
        },
    )
}

@Composable
private fun AnimeSourcePinButton(
    isPinned: Boolean,
    onClick: () -> Unit,
) {
    val icon = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin
    val tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
    val description = if (isPinned) R.string.action_unpin else R.string.action_pin
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            tint = tint,
            contentDescription = stringResource(description),
        )
    }
}

@Composable
private fun AnimeSourceOptionsDialog(
    source: AnimeSource,
    onClickPin: () -> Unit,
    onClickDisable: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(text = source.visualName)
        },
        text = {
            Column {
                val textId = if (Pin.Pinned in source.pin) R.string.action_unpin else R.string.action_pin
                Text(
                    text = stringResource(textId),
                    modifier = Modifier
                        .clickable(onClick = onClickPin)
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                )
                if (source.id != LocalAnimeSource.ID) {
                    Text(
                        text = stringResource(id = R.string.action_disable),
                        modifier = Modifier
                            .clickable(onClick = onClickDisable)
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                    )
                }
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {},
    )
}

sealed class AnimeSourceUiModel {
    data class Item(val source: AnimeSource) : AnimeSourceUiModel()
    data class Header(val language: String) : AnimeSourceUiModel()
}
