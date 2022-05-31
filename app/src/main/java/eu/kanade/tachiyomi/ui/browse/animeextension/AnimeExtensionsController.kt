package eu.kanade.tachiyomi.ui.browse.animeextension

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import eu.kanade.presentation.browse.AnimeExtensionScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.extension.model.AnimeExtension
import eu.kanade.tachiyomi.ui.base.controller.ComposeController
import eu.kanade.tachiyomi.ui.base.controller.pushController
import eu.kanade.tachiyomi.ui.browse.BrowseController
import eu.kanade.tachiyomi.ui.browse.extension.details.ExtensionDetailsController
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.appcompat.queryTextChanges

class AnimeExtensionsController : ComposeController<AnimeExtensionsPresenter>() {

    private var query = ""

    init {
        setHasOptionsMenu(true)
    }

    override fun getTitle() = applicationContext?.getString(R.string.label_extensions)

    override fun createPresenter() = AnimeExtensionsPresenter()

    @Composable
    override fun ComposeContent(nestedScrollInterop: NestedScrollConnection) {
        AnimeExtensionScreen(
            nestedScrollInterop = nestedScrollInterop,
            presenter = presenter,
            onLongClickItem = { extension ->
                when (extension) {
                    is AnimeExtension.Available -> presenter.installExtension(extension)
                    else -> presenter.uninstallExtension(extension.pkgName)
                }
            },
            onClickItemCancel = { extension ->
                presenter.cancelInstallUpdateExtension(extension)
            },
            onClickUpdateAll = {
                presenter.updateAllExtensions()
            },
            onLaunched = {
                val ctrl = parentController as BrowseController
                ctrl.setExtensionUpdateBadge()
                ctrl.extensionListUpdateRelay.call(true)
            },
            onInstallExtension = {
                presenter.installExtension(it)
            },
            onOpenExtension = {
                val controller = ExtensionDetailsController(it.pkgName)
                parentController!!.router.pushController(controller)
            },
            onTrustExtension = {
                presenter.trustSignature(it.signatureHash)
            },
            onUninstallExtension = {
                presenter.uninstallExtension(it.pkgName)
            },
            onUpdateExtension = {
                presenter.updateExtension(it)
            },
            onRefresh = {
                presenter.findAvailableExtensions()
            },
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> expandActionViewFromInteraction = true
            R.id.action_settings -> {
                parentController!!.router.pushController(AnimeExtensionFilterController())
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (type.isPush) {
            presenter.findAvailableExtensions()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.browse_extensions, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.maxWidth = Int.MAX_VALUE

        // Fixes problem with the overflow icon showing up in lieu of search
        searchItem.fixExpand(onExpand = { invalidateMenuOnExpand() })

        if (query.isNotEmpty()) {
            searchItem.expandActionView()
            searchView.setQuery(query, true)
            searchView.clearFocus()
        }

        searchView.queryTextChanges()
            .filter { router.backstack.lastOrNull()?.controller == this }
            .onEach {
                query = it.toString()
                presenter.search(query)
            }
            .launchIn(viewScope)
    }
}
