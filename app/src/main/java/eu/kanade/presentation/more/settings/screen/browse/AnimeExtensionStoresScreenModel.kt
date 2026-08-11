package eu.kanade.presentation.more.settings.screen.browse

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import mihon.domain.extension.anime.interactor.AddAnimeExtensionStore
import mihon.domain.extension.anime.interactor.GetAnimeExtensionStores
import mihon.domain.extension.anime.interactor.RemoveAnimeExtensionStore
import mihon.domain.extension.anime.interactor.UpdateAnimeExtensionStores
import mihon.domain.extension.anime.model.AnimeExtensionStore
import tachiyomi.core.common.util.lang.launchIO
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeExtensionStoresScreenModel(
    private val getExtensionStores: GetAnimeExtensionStores = Injekt.get(),
    private val addExtensionStore: AddAnimeExtensionStore = Injekt.get(),
    private val removeExtensionStore: RemoveAnimeExtensionStore = Injekt.get(),
    private val updateExtensionStores: UpdateAnimeExtensionStores = Injekt.get(),
    private val extensionManager: AnimeExtensionManager = Injekt.get(),
) : StateScreenModel<AnimeExtensionStoreScreenState>(AnimeExtensionStoreScreenState.Loading) {

    private inline fun updateSuccessState(
        func: (AnimeExtensionStoreScreenState.Success) -> AnimeExtensionStoreScreenState.Success,
    ) {
        mutableState.update {
            when (it) {
                AnimeExtensionStoreScreenState.Loading -> it
                is AnimeExtensionStoreScreenState.Success -> func(it)
            }
        }
    }

    init {
        screenModelScope.launchIO {
            getExtensionStores.subscribe()
                .collectLatest { stores ->
                    mutableState.update {
                        when (it) {
                            AnimeExtensionStoreScreenState.Loading -> AnimeExtensionStoreScreenState.Success(
                                stores = stores,
                            )
                            is AnimeExtensionStoreScreenState.Success -> it.copy(stores = stores)
                        }
                    }
                }
        }
    }

    /**
     * Creates and adds a new repo to the database.
     *
     * @param baseUrl The baseUrl of the repo to create.
     */
    fun createRepo(baseUrl: String) {
        screenModelScope.launchIO {
            updateSuccessState {
                it.copy(
                    dialog = when (it.dialog) {
                        is AnimeExtensionStoreDialog.Create -> it.dialog.copy(processing = true)
                        is AnimeExtensionStoreDialog.Confirm -> it.dialog.copy(processing = true)
                        else -> it.dialog
                    },
                )
            }
            addExtensionStore(baseUrl)
                .onSuccess {
                    extensionManager.findAvailableExtensions()
                    dismissDialog()
                }
                .onFailure { throwable ->
                    updateSuccessState {
                        it.copy(
                            dialog = when (it.dialog) {
                                is AnimeExtensionStoreDialog.Create -> it.dialog.copy(
                                    processing = false,
                                    errorMessage = throwable.message ?: "unknown error",
                                )
                                is AnimeExtensionStoreDialog.Confirm -> it.dialog.copy(
                                    processing = false,
                                    errorMessage = throwable.message ?: "unknown error",
                                )
                                else -> it.dialog
                            },
                        )
                    }
                }
        }
    }

    /**
     * Refreshes information for each repository.
     */
    fun refreshRepos() {
        val status = state.value

        if (status is AnimeExtensionStoreScreenState.Success) {
            screenModelScope.launchIO {
                updateExtensionStores()
            }
        }
    }

    /**
     * Deletes the given repo from the database
     */
    fun deleteRepo(baseUrl: String) {
        screenModelScope.launchIO {
            removeExtensionStore(baseUrl)
            extensionManager.findAvailableExtensions()
        }
    }

    fun addFromDeeplink(storeIndexUrl: String) {
        updateSuccessState { state ->
            state.copy(
                dialog = AnimeExtensionStoreDialog.Confirm(
                    url = storeIndexUrl,
                    alreadyExists = state.stores.any { it.indexUrl == storeIndexUrl },
                ),
            )
        }
    }

    fun showDialog(dialog: AnimeExtensionStoreDialog) {
        updateSuccessState { state ->
            state.copy(dialog = dialog)
        }
    }

    fun dismissDialog() {
        updateSuccessState {
            it.copy(dialog = null)
        }
    }
}

sealed class AnimeExtensionStoreDialog {
    data class Create(val processing: Boolean = false, val errorMessage: String? = null) : AnimeExtensionStoreDialog()
    data class Delete(val store: AnimeExtensionStore) : AnimeExtensionStoreDialog()
    data class Confirm(
        val url: String,
        val alreadyExists: Boolean = false,
        val processing: Boolean = false,
        val errorMessage: String? = null,
    ) : AnimeExtensionStoreDialog()
}

sealed class AnimeExtensionStoreScreenState {

    @Immutable
    data object Loading : AnimeExtensionStoreScreenState()

    @Immutable
    data class Success(
        val stores: List<AnimeExtensionStore>,
        val dialog: AnimeExtensionStoreDialog? = null,
    ) : AnimeExtensionStoreScreenState() {

        val isEmpty: Boolean
            get() = stores.isEmpty()
    }
}
