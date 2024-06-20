package eu.kanade.presentation.more.settings.screen.browse

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.extension.anime.interactor.CreateAnimeExtensionRepo
import eu.kanade.domain.extension.anime.interactor.DeleteAnimeExtensionRepo
import eu.kanade.domain.extension.anime.interactor.GetAnimeExtensionRepos
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeExtensionReposScreenModel(
    private val getExtensionRepos: GetAnimeExtensionRepos = Injekt.get(),
    private val createExtensionRepo: CreateAnimeExtensionRepo = Injekt.get(),
    private val deleteExtensionRepo: DeleteAnimeExtensionRepo = Injekt.get(),
) : StateScreenModel<RepoScreenState>(RepoScreenState.Loading) {

    private val _events: Channel<RepoEvent> = Channel(Int.MAX_VALUE)
    val events = _events.receiveAsFlow()

    init {
        screenModelScope.launchIO {
            getExtensionRepos.subscribe()
                .collectLatest { repos ->
                    mutableState.update {
                        RepoScreenState.Success(
                            repos = repos.toImmutableSet(),
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
            when (createExtensionRepo.await(name)) {
                is CreateAnimeExtensionRepo.Result.InvalidUrl -> _events.send(RepoEvent.InvalidUrl)
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
            deleteExtensionRepo.await(repo)
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
