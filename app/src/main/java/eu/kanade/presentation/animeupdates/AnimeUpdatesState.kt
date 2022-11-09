package eu.kanade.presentation.animeupdates

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eu.kanade.core.util.insertSeparators
import eu.kanade.tachiyomi.ui.animeupdates.AnimeUpdatesItem
import eu.kanade.tachiyomi.ui.animeupdates.AnimeUpdatesPresenter
import eu.kanade.tachiyomi.util.lang.toDateKey
import java.util.Date

@Stable
interface AnimeUpdatesState {
    val isLoading: Boolean
    val items: List<AnimeUpdatesItem>
    val selected: List<AnimeUpdatesItem>
    val selectionMode: Boolean
    val uiModels: List<AnimeUpdatesUiModel>
    var dialog: AnimeUpdatesPresenter.Dialog?
}
fun AnimeUpdatesState(): AnimeUpdatesState = AnimeUpdatesStateImpl()
class AnimeUpdatesStateImpl : AnimeUpdatesState {
    override var isLoading: Boolean by mutableStateOf(true)
    override var items: List<AnimeUpdatesItem> by mutableStateOf(emptyList())
    override val selected: List<AnimeUpdatesItem> by derivedStateOf {
        items.filter { it.selected }
    }
    override val selectionMode: Boolean by derivedStateOf { selected.isNotEmpty() }
    override val uiModels: List<AnimeUpdatesUiModel> by derivedStateOf {
        items.toUpdateUiModel()
    }
    override var dialog: AnimeUpdatesPresenter.Dialog? by mutableStateOf(null)
}

fun List<AnimeUpdatesItem>.toUpdateUiModel(): List<AnimeUpdatesUiModel> {
    return this.map {
        AnimeUpdatesUiModel.Item(it)
    }
        .insertSeparators { before, after ->
            val beforeDate = before?.item?.update?.dateFetch?.toDateKey() ?: Date(0)
            val afterDate = after?.item?.update?.dateFetch?.toDateKey() ?: Date(0)
            when {
                beforeDate.time != afterDate.time && afterDate.time != 0L ->
                    AnimeUpdatesUiModel.Header(afterDate)
                // Return null to avoid adding a separator between two items.
                else -> null
            }
        }
}
