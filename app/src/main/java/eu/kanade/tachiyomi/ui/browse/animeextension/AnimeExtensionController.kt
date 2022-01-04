package eu.kanade.tachiyomi.ui.browse.animeextension

import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import dev.chrisbanes.insetter.applyInsetter
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.databinding.ExtensionControllerBinding
import eu.kanade.tachiyomi.extension.model.AnimeExtension
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.browse.BrowseController
import eu.kanade.tachiyomi.ui.browse.animeextension.details.AnimeExtensionDetailsController
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.appcompat.queryTextChanges
import reactivecircus.flowbinding.swiperefreshlayout.refreshes

/**
 * Controller to manage the catalogues available in the app.
 */
open class AnimeExtensionController :
    NucleusController<ExtensionControllerBinding, AnimeExtensionPresenter>(),
    AnimeExtensionAdapter.OnButtonClickListener,
    FlexibleAdapter.OnItemClickListener,
    FlexibleAdapter.OnItemLongClickListener,
    AnimeExtensionTrustDialog.Listener {

    /**
     * Adapter containing the list of manga from the catalogue.
     */
    private var adapter: FlexibleAdapter<IFlexible<*>>? = null

    private var extensions: List<AnimeExtensionItem> = emptyList()

    private var query = ""

    init {
        setHasOptionsMenu(true)
    }

    override fun getTitle(): String? {
        return applicationContext?.getString(R.string.label_animeextensions)
    }

    override fun createPresenter(): AnimeExtensionPresenter {
        return AnimeExtensionPresenter(activity!!)
    }

    override fun createBinding(inflater: LayoutInflater) =
        ExtensionControllerBinding.inflate(inflater)

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        binding.recycler.applyInsetter {
            type(navigationBars = true) {
                padding()
            }
        }

        binding.swipeRefresh.isRefreshing = true
        binding.swipeRefresh.refreshes()
            .onEach { presenter.findAvailableExtensions() }
            .launchIn(viewScope)

        // Initialize adapter, scroll listener and recycler views
        adapter = AnimeExtensionAdapter(this)
        // Create recycler and set adapter.
        binding.recycler.layoutManager = LinearLayoutManager(view.context)
        binding.recycler.adapter = adapter
        adapter?.fastScroller = binding.fastScroller
    }

    override fun onDestroyView(view: View) {
        adapter = null
        super.onDestroyView(view)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> expandActionViewFromInteraction = true
            R.id.action_settings -> {
                parentController!!.router.pushController(
                    AnimeExtensionFilterController().withFadeTransaction()
                )
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

    override fun onButtonClick(position: Int) {
        val extension = (adapter?.getItem(position) as? AnimeExtensionItem)?.extension ?: return
        when (extension) {
            is AnimeExtension.Available -> presenter.installExtension(extension)
            is AnimeExtension.Untrusted -> openTrustDialog(extension)
            is AnimeExtension.Installed -> {
                if (!extension.hasUpdate) {
                    openDetails(extension)
                } else {
                    presenter.updateExtension(extension)
                }
            }
        }
    }

    override fun onCancelButtonClick(position: Int) {
        val extension = (adapter?.getItem(position) as? AnimeExtensionItem)?.extension ?: return
        presenter.cancelInstallUpdateExtension(extension)
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
                updateExtensionsList()
            }
            .launchIn(viewScope)
    }

    override fun onItemClick(view: View, position: Int): Boolean {
        val extension = (adapter?.getItem(position) as? AnimeExtensionItem)?.extension ?: return false
        when (extension) {
            is AnimeExtension.Available -> presenter.installExtension(extension)
            is AnimeExtension.Untrusted -> openTrustDialog(extension)
            is AnimeExtension.Installed -> openDetails(extension)
        }
        return false
    }

    override fun onItemLongClick(position: Int) {
        val extension = (adapter?.getItem(position) as? AnimeExtensionItem)?.extension ?: return
        if (extension is AnimeExtension.Installed || extension is AnimeExtension.Untrusted) {
            uninstallExtension(extension.pkgName)
        }
    }

    private fun openDetails(extension: AnimeExtension.Installed) {
        val controller = AnimeExtensionDetailsController(extension.pkgName)
        parentController!!.router.pushController(controller.withFadeTransaction())
    }

    private fun openTrustDialog(extension: AnimeExtension.Untrusted) {
        AnimeExtensionTrustDialog(this, extension.signatureHash, extension.pkgName)
            .showDialog(router)
    }

    fun setExtensions(extensions: List<AnimeExtensionItem>) {
        binding.swipeRefresh.isRefreshing = false
        this.extensions = extensions
        updateExtensionsList()

        // Update badge on parent controller tab
        val ctrl = parentController as BrowseController
        ctrl.setAnimeExtensionUpdateBadge()
        ctrl.extensionListUpdateRelay.call(true)
    }

    private fun updateExtensionsList() {
        if (query.isNotBlank()) {
            val queries = query.split(",")
            adapter?.updateDataSet(
                extensions.filter {
                    queries.any { query ->
                        when (it.extension) {
                            is AnimeExtension.Installed -> {
                                it.extension.sources.any {
                                    it.name.contains(query, ignoreCase = true) ||
                                        it.id == query.toLongOrNull() ||
                                        if (it is AnimeHttpSource) { it.baseUrl.contains(query, ignoreCase = true) } else false
                                } || it.extension.name.contains(query, ignoreCase = true)
                            }
                            is AnimeExtension.Untrusted, is AnimeExtension.Available -> {
                                it.extension.name.contains(query, ignoreCase = true)
                            }
                        }
                    }
                }
            )
        } else {
            adapter?.updateDataSet(extensions)
        }
    }

    fun downloadUpdate(item: AnimeExtensionItem) {
        adapter?.updateItem(item, item.installStep)
    }

    override fun trustSignature(signatureHash: String) {
        presenter.trustSignature(signatureHash)
    }

    override fun uninstallExtension(pkgName: String) {
        presenter.uninstallExtension(pkgName)
    }
}
