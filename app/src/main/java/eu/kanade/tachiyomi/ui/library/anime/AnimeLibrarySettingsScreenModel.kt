package eu.kanade.tachiyomi.ui.library.anime

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.category.anime.interactor.SetDisplayModeForAnimeCategory
import eu.kanade.domain.category.anime.interactor.SetSortModeForAnimeCategory
import eu.kanade.domain.library.service.LibraryPreferences
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.util.preference.toggle
import eu.kanade.tachiyomi.widget.TriState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import tachiyomi.core.preference.Preference
import tachiyomi.core.preference.getAndSet
import tachiyomi.core.util.lang.launchIO
import tachiyomi.domain.category.anime.interactor.GetAnimeCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.anime.model.AnimeLibrarySort
import tachiyomi.domain.library.model.LibraryDisplayMode
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeLibrarySettingsScreenModel(
    val preferences: BasePreferences = Injekt.get(),
    val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val getCategories: GetAnimeCategories = Injekt.get(),
    private val setDisplayModeForCategory: SetDisplayModeForAnimeCategory = Injekt.get(),
    private val setSortModeForCategory: SetSortModeForAnimeCategory = Injekt.get(),
    trackManager: TrackManager = Injekt.get(),
) : StateScreenModel<AnimeLibrarySettingsScreenModel.State>(State()) {

    val trackServices = trackManager.services.filter { service -> service.isLogged }

    init {
        coroutineScope.launchIO {
            getCategories.subscribe()
                .collectLatest {
                    mutableState.update { state ->
                        state.copy(
                            categories = it,
                        )
                    }
                }
        }
    }

    fun togglePreference(preference: (LibraryPreferences) -> Preference<Boolean>) {
        preference(libraryPreferences).toggle()
    }

    fun toggleFilter(preference: (LibraryPreferences) -> Preference<Int>) {
        preference(libraryPreferences).getAndSet {
            when (it) {
                TriState.DISABLED.value -> TriState.ENABLED_IS.value
                TriState.ENABLED_IS.value -> TriState.ENABLED_NOT.value
                TriState.ENABLED_NOT.value -> TriState.DISABLED.value
                else -> throw IllegalStateException("Unknown TriStateGroup state: $this")
            }
        }
    }

    fun toggleTracker(id: Int) {
        toggleFilter { libraryPreferences.filterTrackedAnime(id) }
    }

    fun setDisplayMode(category: Category, mode: LibraryDisplayMode) {
        coroutineScope.launchIO {
            setDisplayModeForCategory.await(category, mode)
        }
    }

    fun setSort(category: Category, mode: AnimeLibrarySort.Type, direction: AnimeLibrarySort.Direction) {
        coroutineScope.launchIO {
            setSortModeForCategory.await(category, mode, direction)
        }
    }

    @Immutable
    data class State(
        val categories: List<Category> = emptyList(),
    )
}
