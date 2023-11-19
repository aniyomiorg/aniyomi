package eu.kanade.tachiyomi.ui.deeplink.anime

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.domain.entries.anime.model.toDomainAnime
import eu.kanade.tachiyomi.animesource.online.ResolvableAnimeSource
import kotlinx.coroutines.flow.update
import tachiyomi.core.util.lang.launchIO
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DeepLinkAnimeScreenModel(
    query: String = "",
    private val sourceManager: AnimeSourceManager = Injekt.get(),
) : StateScreenModel<DeepLinkAnimeScreenModel.State>(State.Loading) {

    init {
        coroutineScope.launchIO {
            val anime = sourceManager.getCatalogueSources()
                .filterIsInstance<ResolvableAnimeSource>()
                .filter { it.canResolveUri(query) }
                .firstNotNullOfOrNull { it.getAnime(query)?.toDomainAnime(it.id) }

            mutableState.update {
                if (anime == null) {
                    State.NoResults
                } else {
                    State.Result(anime)
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
        data class Result(val anime: Anime) : State
    }
}
