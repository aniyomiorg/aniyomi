package eu.kanade.tachiyomi.ui.browse.animeextension

import android.app.Application
import androidx.annotation.StringRes
import eu.kanade.domain.animeextension.interactor.GetAnimeExtensionsByType
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.animebrowse.AnimeExtensionState
import eu.kanade.presentation.animebrowse.AnimeExtensionsState
import eu.kanade.presentation.animebrowse.AnimeExtensionsStateImpl
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animeextension.AnimeExtensionManager
import eu.kanade.tachiyomi.animeextension.model.AnimeExtension
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeExtensionsPresenter(
    private val presenterScope: CoroutineScope,
    private val state: AnimeExtensionsStateImpl = AnimeExtensionState() as AnimeExtensionsStateImpl,
    private val preferences: SourcePreferences = Injekt.get(),
    private val extensionManager: AnimeExtensionManager = Injekt.get(),
    private val getExtensions: GetAnimeExtensionsByType = Injekt.get(),
) : AnimeExtensionsState by state {

    private val _query: MutableStateFlow<String?> = MutableStateFlow(null)
    val query: StateFlow<String?> = _query.asStateFlow()

    private var _currentDownloads = MutableStateFlow<Map<String, InstallStep>>(hashMapOf())

    fun onCreate() {
        val context = Injekt.get<Application>()
        val extensionMapper: (Map<String, InstallStep>) -> ((AnimeExtension) -> AnimeExtensionUiModel) = { map ->
            {
                AnimeExtensionUiModel.Item(it, map[it.pkgName] ?: InstallStep.Idle)
            }
        }
        val queryFilter: (String) -> ((AnimeExtension) -> Boolean) = { query ->
            filter@{ extension ->
                if (query.isEmpty()) return@filter true
                query.split(",").any { _input ->
                    val input = _input.trim()
                    if (input.isEmpty()) return@any false
                    when (extension) {
                        is AnimeExtension.Available -> {
                            extension.sources.any {
                                it.name.contains(input, ignoreCase = true) ||
                                    it.baseUrl.contains(input, ignoreCase = true) ||
                                    it.id == input.toLongOrNull()
                            } || extension.name.contains(input, ignoreCase = true)
                        }
                        is AnimeExtension.Installed -> {
                            extension.sources.any {
                                it.name.contains(input, ignoreCase = true) ||
                                    it.id == input.toLongOrNull() ||
                                    if (it is AnimeHttpSource) { it.baseUrl.contains(input, ignoreCase = true) } else false
                            } || extension.name.contains(input, ignoreCase = true)
                        }
                        is AnimeExtension.Untrusted -> extension.name.contains(input, ignoreCase = true)
                    }
                }
            }
        }

        presenterScope.launchIO {
            combine(
                _query,
                _currentDownloads,
                getExtensions.subscribe(),
            ) { query, downloads, (_updates, _installed, _available, _untrusted) ->
                val searchQuery = query ?: ""

                val languagesWithExtensions = _available
                    .filter(queryFilter(searchQuery))
                    .groupBy { LocaleHelper.getSourceDisplayName(it.lang, context) }
                    .toSortedMap()
                    .flatMap { (lang, exts) ->
                        listOf(
                            AnimeExtensionUiModel.Header.Text(lang),
                            *exts.map(extensionMapper(downloads)).toTypedArray(),
                        )
                    }

                val items = mutableListOf<AnimeExtensionUiModel>()

                val updates = _updates.filter(queryFilter(searchQuery)).map(extensionMapper(downloads))
                if (updates.isNotEmpty()) {
                    items.add(AnimeExtensionUiModel.Header.Resource(R.string.ext_updates_pending))
                    items.addAll(updates)
                }

                val installed = _installed.filter(queryFilter(searchQuery)).map(extensionMapper(downloads))
                val untrusted = _untrusted.filter(queryFilter(searchQuery)).map(extensionMapper(downloads))
                if (installed.isNotEmpty() || untrusted.isNotEmpty()) {
                    items.add(AnimeExtensionUiModel.Header.Resource(R.string.ext_installed))
                    items.addAll(installed)
                    items.addAll(untrusted)
                }

                if (languagesWithExtensions.isNotEmpty()) {
                    items.addAll(languagesWithExtensions)
                }

                items
            }
                .onStart { delay(500) } // Defer to avoid crashing on initial render
                .collectLatest {
                    state.isLoading = false
                    state.items = it
                }
        }
        presenterScope.launchIO { findAvailableExtensions() }

        preferences.animeextensionUpdatesCount().changes()
            .onEach { state.updates = it }
            .launchIn(presenterScope)
    }

    fun search(query: String?) {
        presenterScope.launchIO {
            _query.emit(query)
        }
    }

    fun updateAllExtensions() {
        presenterScope.launchIO {
            if (state.isEmpty) return@launchIO
            state.items
                .mapNotNull {
                    when {
                        it !is AnimeExtensionUiModel.Item -> null
                        it.extension !is AnimeExtension.Installed -> null
                        !it.extension.hasUpdate -> null
                        else -> it.extension
                    }
                }
                .forEach { updateExtension(it) }
        }
    }

    fun installExtension(extension: AnimeExtension.Available) {
        extensionManager.installExtension(extension).subscribeToInstallUpdate(extension)
    }

    fun updateExtension(extension: AnimeExtension.Installed) {
        extensionManager.updateExtension(extension).subscribeToInstallUpdate(extension)
    }

    fun cancelInstallUpdateExtension(extension: AnimeExtension) {
        extensionManager.cancelInstallUpdateExtension(extension)
    }

    private fun removeDownloadState(extension: AnimeExtension) {
        _currentDownloads.update { _map ->
            val map = _map.toMutableMap()
            map.remove(extension.pkgName)
            map
        }
    }

    private fun addDownloadState(extension: AnimeExtension, installStep: InstallStep) {
        _currentDownloads.update { _map ->
            val map = _map.toMutableMap()
            map[extension.pkgName] = installStep
            map
        }
    }

    private fun Observable<InstallStep>.subscribeToInstallUpdate(extension: AnimeExtension) {
        this
            .doOnUnsubscribe { removeDownloadState(extension) }
            .subscribe(
                { installStep -> addDownloadState(extension, installStep) },
                { removeDownloadState(extension) },
            )
    }

    fun uninstallExtension(pkgName: String) {
        extensionManager.uninstallExtension(pkgName)
    }

    fun findAvailableExtensions() {
        presenterScope.launchIO {
            state.isRefreshing = true
            extensionManager.findAvailableExtensions()
            state.isRefreshing = false
        }
    }

    fun trustSignature(signatureHash: String) {
        extensionManager.trustSignature(signatureHash)
    }
}

sealed interface AnimeExtensionUiModel {
    sealed interface Header : AnimeExtensionUiModel {
        data class Resource(@StringRes val textRes: Int) : Header
        data class Text(val text: String) : Header
    }
    data class Item(
        val extension: AnimeExtension,
        val installStep: InstallStep,
    ) : AnimeExtensionUiModel
}
