package eu.kanade.tachiyomi.ui.entries.anime.notes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.entries.anime.AnimeNotesScreen
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.entries.anime.interactor.UpdateAnimeNotes
import tachiyomi.domain.entries.anime.model.Anime
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeNotesScreen(
    private val anime: Anime,
) : Screen() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val screenModel = rememberScreenModel { Model(anime) }
        val state by screenModel.state.collectAsState()

        AnimeNotesScreen(
            state = state,
            navigateUp = navigator::pop,
            onUpdate = screenModel::updateNotes,
        )
    }

    private class Model(
        private val anime: Anime,
        private val updateAnimeNotes: UpdateAnimeNotes = Injekt.get(),
    ) : StateScreenModel<State>(State(anime, anime.notes)) {

        fun updateNotes(content: String) {
            if (content == state.value.notes) return

            mutableState.update {
                it.copy(notes = content)
            }

            screenModelScope.launchNonCancellable {
                updateAnimeNotes(anime.id, content)
            }
        }
    }

    @Immutable
    data class State(
        val anime: Anime,
        val notes: String,
    )
}
