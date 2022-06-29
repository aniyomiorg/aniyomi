package eu.kanade.presentation.browse

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.domain.animesource.model.AnimeSource
import eu.kanade.domain.animesource.model.Pin
import eu.kanade.presentation.browse.components.BaseAnimeSourceItem
import eu.kanade.presentation.components.EmptyScreen
import eu.kanade.presentation.components.LoadingScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.LocalAnimeSource
import eu.kanade.tachiyomi.ui.browse.animesource.AnimeSourceState
import eu.kanade.tachiyomi.ui.browse.animesource.AnimeSourcesPresenter

@Composable
fun AnimeSourcesScreen(
    nestedScrollInterop: NestedScrollConnection,
    presenter: AnimeSourcesPresenter,
    onClickItem: (AnimeSource) -> Unit,
    onClickDisable: (AnimeSource) -> Unit,
    onClickLatest: (AnimeSource) -> Unit,
    onClickPin: (AnimeSource) -> Unit,
) {
    val state by presenter.state.collectAsState()

    when (state) {
        is AnimeSourceState.Loading -> LoadingScreen()
        is AnimeSourceState.Error -> Text(text = (state as AnimeSourceState.Error).error.message!!)
        is AnimeSourceState.Success -> AnimeSourceList(
            nestedScrollConnection = nestedScrollInterop,
            list = (state as AnimeSourceState.Success).uiModels,
            onClickItem = onClickItem,
            onClickDisable = onClickDisable,
            onClickLatest = onClickLatest,
            onClickPin = onClickPin,
        )
    }
}

@Composable
fun AnimeSourceList(
    nestedScrollConnection: NestedScrollConnection,
    list: List<AnimeSourceUiModel>,
    onClickItem: (AnimeSource) -> Unit,
    onClickDisable: (AnimeSource) -> Unit,
    onClickLatest: (AnimeSource) -> Unit,
    onClickPin: (AnimeSource) -> Unit,
) {
    if (list.isEmpty()) {
        EmptyScreen(textResource = R.string.source_empty_screen)
        return
    }

    val (sourceState, setSourceState) = remember { mutableStateOf<AnimeSource?>(null) }
    LazyColumn(
        modifier = Modifier
            .nestedScroll(nestedScrollConnection),
        contentPadding = WindowInsets.navigationBars.asPaddingValues(),
    ) {
        items(
            items = list,
            contentType = {
                when (it) {
                    is AnimeSourceUiModel.Header -> "header"
                    is AnimeSourceUiModel.Item -> "item"
                }
            },
            key = {
                when (it) {
                    is AnimeSourceUiModel.Header -> it.hashCode()
                    is AnimeSourceUiModel.Item -> it.source.key()
                }
            },
        ) { model ->
            when (model) {
                is AnimeSourceUiModel.Header -> {
                    SourceHeader(
                        modifier = Modifier.animateItemPlacement(),
                        language = model.language,
                    )
                }
                is AnimeSourceUiModel.Item -> AnimeSourceItem(
                    modifier = Modifier.animateItemPlacement(),
                    source = model.source,
                    onClickItem = onClickItem,
                    onLongClickItem = {
                        setSourceState(it)
                    },
                    onClickLatest = onClickLatest,
                    onClickPin = onClickPin,
                )
            }
        }
    }

    if (sourceState != null) {
        AnimeSourceOptionsDialog(
            source = sourceState,
            onClickPin = {
                onClickPin(sourceState)
                setSourceState(null)
            },
            onClickDisable = {
                onClickDisable(sourceState)
                setSourceState(null)
            },
            onDismiss = { setSourceState(null) },
        )
    }
}

@Composable
fun AnimeSourceItem(
    modifier: Modifier = Modifier,
    source: AnimeSource,
    onClickItem: (AnimeSource) -> Unit,
    onLongClickItem: (AnimeSource) -> Unit,
    onClickLatest: (AnimeSource) -> Unit,
    onClickPin: (AnimeSource) -> Unit,
) {
    BaseAnimeSourceItem(
        modifier = modifier,
        source = source,
        onClickItem = { onClickItem(source) },
        onLongClickItem = { onLongClickItem(source) },
        action = { source ->
            if (source.supportsLatest) {
                TextButton(onClick = { onClickLatest(source) }) {
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
fun AnimeSourceIcon(
    source: AnimeSource,
) {
    val icon = source.icon
    val modifier = Modifier
        .height(40.dp)
        .aspectRatio(1f)
    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = "",
            modifier = modifier,
        )
    } else {
        Image(
            painter = painterResource(id = R.mipmap.ic_local_source),
            contentDescription = "",
            modifier = modifier,
        )
    }
}

@Composable
fun AnimeSourcePinButton(
    isPinned: Boolean,
    onClick: () -> Unit,
) {
    val icon = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin
    val tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = "",
            tint = tint,
        )
    }
}

@Composable
fun AnimeSourceOptionsDialog(
    source: AnimeSource,
    onClickPin: () -> Unit,
    onClickDisable: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(text = source.nameWithLanguage)
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
