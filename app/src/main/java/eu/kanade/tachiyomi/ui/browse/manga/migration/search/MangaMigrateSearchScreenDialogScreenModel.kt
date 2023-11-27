package eu.kanade.tachiyomi.ui.browse.manga.migration.search

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.entries.manga.interactor.GetManga
import tachiyomi.domain.entries.manga.model.Manga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaMigrateSearchScreenDialogScreenModel(
    val mangaId: Long,
    getManga: GetManga = Injekt.get(),
) : StateScreenModel<MangaMigrateSearchScreenDialogScreenModel.State>(State()) {

    init {
        screenModelScope.launch {
            val manga = getManga.await(mangaId)!!

            mutableState.update {
                it.copy(manga = manga)
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
        val manga: Manga? = null,
        val dialog: Dialog? = null,
    )

    sealed interface Dialog {
        data class Migrate(val manga: Manga) : Dialog
    }
}
