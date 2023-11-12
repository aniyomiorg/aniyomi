package eu.kanade.tachiyomi.ui.browse.anime.migration.search

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.entries.anime.interactor.GetAnime
import tachiyomi.domain.entries.anime.model.Anime
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeMigrateSearchScreenDialogScreenModel(
    val animeId: Long,
    getAnime: GetAnime = Injekt.get(),
) : StateScreenModel<AnimeMigrateSearchScreenDialogScreenModel.State>(State()) {

    init {
        coroutineScope.launch {
            val anime = getAnime.await(animeId)!!

            mutableState.update {
                it.copy(anime = anime)
            }
        }
    }

    fun setDialog(dialog: Dialog?) {
        mutableState.update {
            it.copy(dialog = dialog)
        }
    }

    @Immutable
    data class State(
        val anime: Anime? = null,
        val dialog: Dialog? = null,
    )

    sealed class Dialog {
        data class Migrate(val anime: Anime) : Dialog()
    }
}
