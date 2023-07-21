package eu.kanade.tachiyomi.ui.browse.anime.extension

import android.app.Application
import androidx.annotation.StringRes
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.domain.extension.anime.interactor.GetAnimeExtensionsByType
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.components.SEARCH_DEBOUNCE_MILLIS
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.extension.InstallStep
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import rx.Observable
import tachiyomi.core.util.lang.launchIO
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Duration.Companion.seconds

class AnimeExtensionsScreenModel(
    preferences: SourcePreferences = Injekt.get(),
    private val extensionManager: AnimeExtensionManager = Injekt.get(),
    private val getExtensions: GetAnimeExtensionsByType = Injekt.get(),
) : StateScreenModel<AnimeExtensionsState>(AnimeExtensionsState()) {

    private var _currentDownloads = MutableStateFlow<Map<String, InstallStep>>(hashMapOf())

    init {
        val context = Injekt.get<Application>()
        val extensionMapper: (Map<String, InstallStep>) -> ((AnimeExtension) -> AnimeExtensionUiModel.Item) = { map ->
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

        coroutineScope.launchIO {
            combine(
                state.map { it.searchQuery }.distinctUntilChanged().debounce(SEARCH_DEBOUNCE_MILLIS),
                _currentDownloads,
                getExtensions.subscribe(),
            ) { query, downloads, (_updates, _installed, _available, _untrusted) ->
                val searchQuery = query ?: ""

                val itemsGroups: ItemGroups = mutableMapOf()

                val updates = _updates.filter(queryFilter(searchQuery)).map(extensionMapper(downloads))
                if (updates.isNotEmpty()) {
                    itemsGroups[AnimeExtensionUiModel.Header.Resource(R.string.ext_updates_pending)] = updates
                }

                val installed = _installed.filter(queryFilter(searchQuery)).map(extensionMapper(downloads))
                val untrusted = _untrusted.filter(queryFilter(searchQuery)).map(extensionMapper(downloads))
                if (installed.isNotEmpty() || untrusted.isNotEmpty()) {
                    itemsGroups[AnimeExtensionUiModel.Header.Resource(R.string.ext_installed)] = installed + untrusted
                }

                val languagesWithExtensions = _available
                    .filter(queryFilter(searchQuery))
                    .groupBy { it.lang }
                    .toSortedMap(LocaleHelper.comparator)
                    .map { (lang, exts) ->
                        AnimeExtensionUiModel.Header.Text(LocaleHelper.getSourceDisplayName(lang, context)) to exts.map(extensionMapper(downloads))
                    }

                if (languagesWithExtensions.isNotEmpty()) {
                    itemsGroups.putAll(languagesWithExtensions)
                }

                itemsGroups
            }
                .collectLatest {
                    mutableState.update { state ->
                        state.copy(
                            isLoading = false,
                            items = it,
                        )
                    }
                }
        }
        coroutineScope.launchIO { findAvailableExtensions() }

        preferences.animeExtensionUpdatesCount().changes()
            .onEach { mutableState.update { state -> state.copy(updates = it) } }
            .launchIn(coroutineScope)
    }

    fun search(query: String?) {
        mutableState.update {
            it.copy(searchQuery = query)
        }
    }

    fun updateAllExtensions() {
        coroutineScope.launchIO {
            with(state.value) {
                if (isEmpty) return@launchIO
                items.values
                    .flatten()
                    .mapNotNull {
                        when {
                            it !is AnimeExtensionUiModel.Item -> null
                            it.extension !is AnimeExtension.Installed -> null
                            !it.extension.hasUpdate -> null
                            else -> it.extension
                        }
                    }
                    .forEach(::updateExtension)
            }
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
        coroutineScope.launchIO {
            mutableState.update { it.copy(isRefreshing = true) }
            extensionManager.findAvailableExtensions()

            // Fake slower refresh so it doesn't seem like it's not doing anything
            delay(1.seconds)

            mutableState.update { it.copy(isRefreshing = false) }
        }
    }

    fun trustSignature(signatureHash: String) {
        extensionManager.trustSignature(signatureHash)
    }
}

data class AnimeExtensionsState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val items: ItemGroups = mutableMapOf(),
    val updates: Int = 0,
    val searchQuery: String? = null,
) {
    val isEmpty = items.isEmpty()
}

typealias ItemGroups = MutableMap<AnimeExtensionUiModel.Header, List<AnimeExtensionUiModel.Item>>

object AnimeExtensionUiModel {
    sealed interface Header {
        data class Resource(@StringRes val textRes: Int) : Header
        data class Text(val text: String) : Header
    }
    data class Item(
        val extension: AnimeExtension,
        val installStep: InstallStep,
    )
}
