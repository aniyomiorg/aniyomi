package eu.kanade.tachiyomi.ui.browse.animeextension

import android.content.Context
import android.os.Bundle
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.extension.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.model.AnimeExtension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.system.LocaleHelper
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

private typealias AnimeExtensionTuple =
    Triple<List<AnimeExtension.Installed>, List<AnimeExtension.Untrusted>, List<AnimeExtension.Available>>

/**
 * Presenter of [AnimeExtensionController].
 */
open class AnimeExtensionPresenter(
    private val context: Context,
    private val extensionManager: AnimeExtensionManager = Injekt.get(),
    private val preferences: PreferencesHelper = Injekt.get()
) : BasePresenter<AnimeExtensionController>() {

    private var extensions = emptyList<AnimeExtensionItem>()

    private var currentDownloads = hashMapOf<String, InstallStep>()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        extensionManager.findAvailableExtensions()
        bindToExtensionsObservable()
    }

    private fun bindToExtensionsObservable(): Subscription {
        val installedObservable = extensionManager.getInstalledExtensionsObservable()
        val untrustedObservable = extensionManager.getUntrustedExtensionsObservable()
        val availableObservable = extensionManager.getAvailableExtensionsObservable()
            .startWith(emptyList<AnimeExtension.Available>())
        return Observable.combineLatest(installedObservable, untrustedObservable, availableObservable) { installed, untrusted, available -> Triple(installed, untrusted, available) }
            .debounce(500, TimeUnit.MILLISECONDS)
            .map(::toItems)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeLatestCache({ view, _ -> view.setExtensions(extensions) })
    }

    @Synchronized
    private fun toItems(tuple: AnimeExtensionTuple): List<AnimeExtensionItem> {
        val activeLangs = preferences.enabledLanguages().get()
        val showNsfwExtensions = preferences.showNsfwExtension().get()

        val (installed, untrusted, available) = tuple

        val items = mutableListOf<AnimeExtensionItem>()

        val updatesSorted = installed.filter { it.hasUpdate && (showNsfwExtensions || !it.isNsfw) }.sortedBy { it.name }
        val installedSorted = installed.filter { !it.hasUpdate && (showNsfwExtensions || !it.isNsfw) }.sortedWith(compareBy({ !it.isObsolete }, { it.name }))
        val untrustedSorted = untrusted.sortedBy { it.name }
        val availableSorted = available
            // Filter out already installed extensions and disabled languages
            .filter { avail ->
                installed.none { it.pkgName == avail.pkgName } &&
                    untrusted.none { it.pkgName == avail.pkgName } &&
                    (avail.lang in activeLangs || avail.lang == "all") &&
                    (showNsfwExtensions || !avail.isNsfw)
            }
            .sortedBy { it.name }

        if (updatesSorted.isNotEmpty()) {
            val header = AnimeExtensionGroupItem(context.getString(R.string.ext_updates_pending), updatesSorted.size, true)
            items += updatesSorted.map { extension ->
                AnimeExtensionItem(extension, header, currentDownloads[extension.pkgName])
            }
        }
        if (installedSorted.isNotEmpty() || untrustedSorted.isNotEmpty()) {
            val header = AnimeExtensionGroupItem(context.getString(R.string.ext_installed), installedSorted.size + untrustedSorted.size)

            items += installedSorted.map { extension ->
                AnimeExtensionItem(extension, header, currentDownloads[extension.pkgName])
            }

            items += untrustedSorted.map { extension ->
                AnimeExtensionItem(extension, header)
            }
        }
        if (availableSorted.isNotEmpty()) {
            val availableGroupedByLang = availableSorted
                .groupBy { LocaleHelper.getSourceDisplayName(it.lang, context) }
                .toSortedMap()

            availableGroupedByLang
                .forEach {
                    val header = AnimeExtensionGroupItem(it.key, it.value.size)
                    items += it.value.map { extension ->
                        AnimeExtensionItem(extension, header, currentDownloads[extension.pkgName])
                    }
                }
        }

        this.extensions = items
        return items
    }

    @Synchronized
    private fun updateInstallStep(extension: AnimeExtension, state: InstallStep): AnimeExtensionItem? {
        val extensions = extensions.toMutableList()
        val position = extensions.indexOfFirst { it.extension.pkgName == extension.pkgName }

        return if (position != -1) {
            val item = extensions[position].copy(installStep = state)
            extensions[position] = item

            this.extensions = extensions
            item
        } else {
            null
        }
    }

    fun installExtension(extension: AnimeExtension.Available) {
        extensionManager.installExtension(extension).subscribeToInstallUpdate(extension)
    }

    fun updateExtension(extension: AnimeExtension.Installed) {
        extensionManager.updateExtension(extension).subscribeToInstallUpdate(extension)
    }

    private fun Observable<InstallStep>.subscribeToInstallUpdate(extension: AnimeExtension) {
        this.doOnNext { currentDownloads[extension.pkgName] = it }
            .doOnUnsubscribe { currentDownloads.remove(extension.pkgName) }
            .map { state -> updateInstallStep(extension, state) }
            .subscribeWithView({ view, item ->
                if (item != null) {
                    view.downloadUpdate(item)
                }
            })
    }

    fun uninstallExtension(pkgName: String) {
        extensionManager.uninstallExtension(pkgName)
    }

    fun findAvailableExtensions() {
        extensionManager.findAvailableExtensions()
    }

    fun trustSignature(signatureHash: String) {
        extensionManager.trustSignature(signatureHash)
    }
}
