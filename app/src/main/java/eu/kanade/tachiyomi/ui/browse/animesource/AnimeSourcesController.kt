package eu.kanade.tachiyomi.ui.browse.animesource

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import eu.kanade.domain.animesource.model.AnimeSource
import eu.kanade.presentation.browse.AnimeSourcesScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.controller.SearchableComposeController
import eu.kanade.tachiyomi.ui.base.controller.pushController
import eu.kanade.tachiyomi.ui.base.controller.requestPermissionsSafe
import eu.kanade.tachiyomi.ui.browse.animesource.browse.BrowseAnimeSourceController
import eu.kanade.tachiyomi.ui.browse.animesource.globalsearch.GlobalAnimeSearchController
import eu.kanade.tachiyomi.ui.browse.animesource.latest.LatestUpdatesController
import eu.kanade.tachiyomi.ui.main.MainActivity
import uy.kohesive.injekt.injectLazy

class AnimeSourcesController : SearchableComposeController<AnimeSourcesPresenter>() {

    private val preferences: PreferencesHelper by injectLazy()

    init {
        setHasOptionsMenu(true)
    }

    override fun getTitle(): String? = resources?.getString(R.string.label_animesources)

    override fun createPresenter(): AnimeSourcesPresenter =
        AnimeSourcesPresenter()

    @Composable
    override fun ComposeContent(nestedScrollInterop: NestedScrollConnection) {
        AnimeSourcesScreen(
            nestedScrollInterop = nestedScrollInterop,
            presenter = presenter,
            onClickItem = { source ->
                openSource(source, BrowseAnimeSourceController(source))
            },
            onClickDisable = { source ->
                presenter.toggleSource(source)
            },
            onClickLatest = { source ->
                openSource(source, LatestUpdatesController(source))
            },
            onClickPin = { source ->
                presenter.togglePin(source)
            },
        )
        LaunchedEffect(Unit) {
            (activity as? MainActivity)?.ready = true
        }
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        requestPermissionsSafe(arrayOf(WRITE_EXTERNAL_STORAGE), 301)
    }

    /**
     * Opens a catalogue with the given controller.
     */
    private fun openSource(source: AnimeSource, controller: BrowseAnimeSourceController) {
        if (!preferences.incognitoMode().get()) {
            preferences.lastUsedSource().set(source.id)
        }
        parentController!!.router.pushController(controller)
    }

    /**
     * Called when an option menu item has been selected by the user.
     *
     * @param item The selected item.
     * @return True if this event has been consumed, false if it has not.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // Initialize option to open catalogue settings.
            R.id.action_settings -> {
                parentController!!.router.pushController(AnimeSourceFilterController())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        createOptionsMenu(
            menu,
            inflater,
            R.menu.browse_sources,
            R.id.action_search,
            R.string.action_global_search_hint,
            false, // GlobalSearch handles the searching here
        )
    }

    override fun onSearchViewQueryTextSubmit(query: String?) {
        parentController!!.router.pushController(GlobalAnimeSearchController(query))
    }
}
