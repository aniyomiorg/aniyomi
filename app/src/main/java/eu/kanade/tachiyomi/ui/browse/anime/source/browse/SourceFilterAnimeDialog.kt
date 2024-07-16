package eu.kanade.tachiyomi.ui.browse.anime.source.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import tachiyomi.core.common.preference.TriState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.CollapsibleBox
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.SelectItem
import tachiyomi.presentation.core.components.SortItem
import tachiyomi.presentation.core.components.TextItem
import tachiyomi.presentation.core.components.TriStateItem
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun SourceFilterAnimeDialog(
    onDismissRequest: () -> Unit,
    filters: AnimeFilterList,
    onReset: () -> Unit,
    onFilter: () -> Unit,
    onUpdate: (AnimeFilterList) -> Unit,
) {
    val updateFilters = { onUpdate(filters) }

    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        LazyColumn {
            stickyHeader {
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .padding(8.dp),
                ) {
                    TextButton(onClick = onReset) {
                        Text(
                            text = stringResource(MR.strings.action_reset),
                            style = LocalTextStyle.current.copy(
                                color = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(onClick = {
                        onFilter()
                        onDismissRequest()
                    }) {
                        Text(stringResource(MR.strings.action_filter))
                    }
                }
                HorizontalDivider()
            }

            items(filters) {
                FilterItem(it, updateFilters)
            }
        }
    }
}

@Composable
private fun FilterItem(filter: AnimeFilter<*>, onUpdate: () -> Unit) {
    when (filter) {
        is AnimeFilter.Header -> {
            HeadingItem(filter.name)
        }
        is AnimeFilter.Separator -> {
            HorizontalDivider()
        }
        is AnimeFilter.CheckBox -> {
            CheckboxItem(
                label = filter.name,
                checked = filter.state,
            ) {
                filter.state = !filter.state
                onUpdate()
            }
        }
        is AnimeFilter.TriState -> {
            TriStateItem(
                label = filter.name,
                state = filter.state.toTriStateFilter(),
            ) {
                filter.state = filter.state.toTriStateFilter().next().toTriStateInt()
                onUpdate()
            }
        }
        is AnimeFilter.Text -> {
            TextItem(
                label = filter.name,
                value = filter.state,
            ) {
                filter.state = it
                onUpdate()
            }
        }
        is AnimeFilter.Select<*> -> {
            SelectItem(
                label = filter.name,
                options = filter.values,
                selectedIndex = filter.state,
                onSelect = {
                    filter.state = it
                    onUpdate()
                },
            )
        }
        is AnimeFilter.Sort -> {
            CollapsibleBox(
                heading = filter.name,
            ) {
                Column {
                    filter.values.mapIndexed { index, item ->
                        SortItem(
                            label = item,
                            sortDescending = filter.state?.ascending?.not()
                                ?.takeIf { index == filter.state?.index },
                        ) {
                            val ascending = if (index == filter.state?.index) {
                                !filter.state!!.ascending
                            } else {
                                filter.state!!.ascending
                            }
                            filter.state = AnimeFilter.Sort.Selection(
                                index = index,
                                ascending = ascending,
                            )
                            onUpdate()
                        }
                    }
                }
            }
        }
        is AnimeFilter.Group<*> -> {
            CollapsibleBox(
                heading = filter.name,
            ) {
                Column {
                    filter.state
                        .filterIsInstance<AnimeFilter<*>>()
                        .map { FilterItem(filter = it, onUpdate = onUpdate) }
                }
            }
        }
    }
}

private fun Int.toTriStateFilter(): TriState {
    return when (this) {
        AnimeFilter.TriState.STATE_IGNORE -> TriState.DISABLED
        AnimeFilter.TriState.STATE_INCLUDE -> TriState.ENABLED_IS
        AnimeFilter.TriState.STATE_EXCLUDE -> TriState.ENABLED_NOT
        else -> throw IllegalStateException("Unknown TriState state: $this")
    }
}

private fun TriState.toTriStateInt(): Int {
    return when (this) {
        TriState.DISABLED -> AnimeFilter.TriState.STATE_IGNORE
        TriState.ENABLED_IS -> AnimeFilter.TriState.STATE_INCLUDE
        TriState.ENABLED_NOT -> AnimeFilter.TriState.STATE_EXCLUDE
    }
}
