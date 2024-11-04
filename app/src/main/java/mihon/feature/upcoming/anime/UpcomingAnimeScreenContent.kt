package mihon.feature.upcoming.anime

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.core.common.Constants
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.coroutines.launch
import mihon.feature.upcoming.anime.components.UpcomingItem
import mihon.feature.upcoming.components.calendar.Calendar
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.TwoPanelBox
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun UpcomingAnimeScreenContent(
    state: UpcomingAnimeScreenModel.State,
    setSelectedYearMonth: (YearMonth) -> Unit,
    onClickUpcoming: (anime: Anime) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val onClickDay: (LocalDate, Int) -> Unit = { date, offset ->
        state.headerIndexes[date]?.let {
            scope.launch {
                listState.animateScrollToItem(it + offset)
            }
        }
    }
    Scaffold(
        topBar = { UpcomingToolbar() },
        modifier = modifier,
    ) { paddingValues ->
        if (isTabletUi()) {
            UpcomingAnimeScreenLargeImpl(
                listState = listState,
                items = state.items,
                events = state.events,
                paddingValues = paddingValues,
                selectedYearMonth = state.selectedYearMonth,
                setSelectedYearMonth = setSelectedYearMonth,
                onClickDay = { onClickDay(it, 0) },
                onClickUpcoming = onClickUpcoming,
            )
        } else {
            UpcomingAnimeScreenSmallImpl(
                listState = listState,
                items = state.items,
                events = state.events,
                paddingValues = paddingValues,
                selectedYearMonth = state.selectedYearMonth,
                setSelectedYearMonth = setSelectedYearMonth,
                onClickDay = { onClickDay(it, 1) },
                onClickUpcoming = onClickUpcoming,
            )
        }
    }
}

@Composable
private fun UpcomingToolbar() {
    val navigator = LocalNavigator.currentOrThrow
    val uriHandler = LocalUriHandler.current

    AppBar(
        title = stringResource(MR.strings.label_upcoming),
        navigateUp = navigator::pop,
        actions = {
            IconButton(onClick = { uriHandler.openUri(Constants.URL_HELP_UPCOMING) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                    contentDescription = stringResource(MR.strings.upcoming_guide),
                )
            }
        },
    )
}

@Composable
private fun DateHeading(
    date: LocalDate,
    animeCount: Int,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = relativeDateText(date),
            modifier = Modifier
                .padding(MaterialTheme.padding.small)
                .padding(start = MaterialTheme.padding.small),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
        )
        Badge(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Text("$animeCount")
        }
    }
}

@Composable
private fun UpcomingAnimeScreenSmallImpl(
    listState: LazyListState,
    items: ImmutableList<UpcomingAnimeUIModel>,
    events: ImmutableMap<LocalDate, Int>,
    paddingValues: PaddingValues,
    selectedYearMonth: YearMonth,
    setSelectedYearMonth: (YearMonth) -> Unit,
    onClickDay: (LocalDate) -> Unit,
    onClickUpcoming: (anime: Anime) -> Unit,
) {
    FastScrollLazyColumn(
        contentPadding = paddingValues,
        state = listState,
    ) {
        item(key = "upcoming-calendar") {
            Calendar(
                selectedYearMonth = selectedYearMonth,
                events = events,
                setSelectedYearMonth = setSelectedYearMonth,
                onClickDay = onClickDay,
            )
        }
        items(
            items = items,
            key = { "upcoming-${it.hashCode()}" },
            contentType = {
                when (it) {
                    is UpcomingAnimeUIModel.Header -> "header"
                    is UpcomingAnimeUIModel.Item -> "item"
                }
            },
        ) { item ->
            when (item) {
                is UpcomingAnimeUIModel.Item -> {
                    UpcomingItem(
                        upcoming = item.anime,
                        onClick = { onClickUpcoming(item.anime) },
                    )
                }
                is UpcomingAnimeUIModel.Header -> {
                    DateHeading(
                        date = item.date,
                        animeCount = item.animeCount,
                    )
                }
            }
        }
    }
}

@Composable
private fun UpcomingAnimeScreenLargeImpl(
    listState: LazyListState,
    items: ImmutableList<UpcomingAnimeUIModel>,
    events: ImmutableMap<LocalDate, Int>,
    paddingValues: PaddingValues,
    selectedYearMonth: YearMonth,
    setSelectedYearMonth: (YearMonth) -> Unit,
    onClickDay: (LocalDate) -> Unit,
    onClickUpcoming: (anime: Anime) -> Unit,
) {
    TwoPanelBox(
        modifier = Modifier.padding(paddingValues),
        startContent = {
            Calendar(
                selectedYearMonth = selectedYearMonth,
                events = events,
                setSelectedYearMonth = setSelectedYearMonth,
                onClickDay = onClickDay,
            )
        },
        endContent = {
            FastScrollLazyColumn(state = listState) {
                items(
                    items = items,
                    key = { "upcoming-${it.hashCode()}" },
                    contentType = {
                        when (it) {
                            is UpcomingAnimeUIModel.Header -> "header"
                            is UpcomingAnimeUIModel.Item -> "item"
                        }
                    },
                ) { item ->
                    when (item) {
                        is UpcomingAnimeUIModel.Item -> {
                            UpcomingItem(
                                upcoming = item.anime,
                                onClick = { onClickUpcoming(item.anime) },
                            )
                        }
                        is UpcomingAnimeUIModel.Header -> {
                            DateHeading(
                                date = item.date,
                                animeCount = item.animeCount,
                            )
                        }
                    }
                }
            }
        },
    )
}
