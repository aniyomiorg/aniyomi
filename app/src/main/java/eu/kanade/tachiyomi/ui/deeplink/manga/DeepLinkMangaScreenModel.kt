package eu.kanade.tachiyomi.ui.deeplink.manga

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.domain.entries.manga.model.toDomainManga
import eu.kanade.tachiyomi.source.online.ResolvableMangaSource
import kotlinx.coroutines.flow.update
import tachiyomi.core.util.lang.launchIO
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.source.manga.service.MangaSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DeepLinkMangaScreenModel(
    query: String = "",
    private val sourceManager: MangaSourceManager = Injekt.get(),
) : StateScreenModel<DeepLinkMangaScreenModel.State>(State.Loading) {

    init {
        coroutineScope.launchIO {
            val manga = sourceManager.getCatalogueSources()
                .filterIsInstance<ResolvableMangaSource>()
                .filter { it.canResolveUri(query) }
                .firstNotNullOfOrNull { it.getManga(query)?.toDomainManga(it.id) }

            mutableState.update {
                if (manga == null) {
                    State.NoResults
                } else {
                    State.Result(manga)
                }
            }
        }
    }

    sealed interface State {
        @Immutable
        data object Loading : State

        @Immutable
        data object NoResults : State

        @Immutable
        data class Result(val manga: Manga) : State
    }
}
