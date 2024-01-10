package eu.kanade.presentation.more.settings.screen.browse

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.source.anime.interactor.CreateAnimeSourceRepo
import eu.kanade.domain.source.anime.interactor.DeleteAnimeSourceRepo
import eu.kanade.domain.source.anime.interactor.GetAnimeSourceRepos
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import tachiyomi.core.util.lang.launchIO
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeExtensionReposScreenModel(
    private val getSourceRepos: GetAnimeSourceRepos = Injekt.get(),
    private val createSourceRepo: CreateAnimeSourceRepo = Injekt.get(),
    private val deleteSourceRepo: DeleteAnimeSourceRepo = Injekt.get(),
) : StateScreenModel<RepoScreenState>(RepoScreenState.Loading) {

    private val _events: Channel<RepoEvent> = Channel(Int.MAX_VALUE)
    val events = _events.receiveAsFlow()

    init {
        screenModelScope.launchIO {
            getSourceRepos.subscribe()
                .collectLatest { repos ->
                    mutableState.update {
                        RepoScreenState.Success(
                            repos = repos.toImmutableList(),
                        )
                    }
                }
        }
    }

    /**
     * Creates and adds a new repo to the database.
     *
     * @param name The name of the repo to create.
     */
    fun createRepo(name: String) {
        screenModelScope.launchIO {
            when (createSourceRepo.await(name)) {
                is CreateAnimeSourceRepo.Result.InvalidUrl -> _events.send(RepoEvent.InvalidUrl)
                else -> {}
            }
        }
    }

    /**
     * Deletes the given repo from the database.
     *
     * @param repo The repo to delete.
     */
    fun deleteRepo(repo: String) {
        screenModelScope.launchIO {
            deleteSourceRepo.await(repo)
        }
    }

    fun showDialog(dialog: RepoDialog) {
        mutableState.update {
            when (it) {
                RepoScreenState.Loading -> it
                is RepoScreenState.Success -> it.copy(dialog = dialog)
            }
        }
    }

    fun dismissDialog() {
        mutableState.update {
            when (it) {
                RepoScreenState.Loading -> it
                is RepoScreenState.Success -> it.copy(dialog = null)
            }
        }
    }
}
