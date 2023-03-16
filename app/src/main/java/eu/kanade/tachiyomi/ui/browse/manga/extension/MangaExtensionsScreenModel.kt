package eu.kanade.tachiyomi.ui.browse.manga.extension

import android.app.Application
import androidx.annotation.StringRes
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.domain.extension.manga.interactor.GetMangaExtensionsByType
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.extension.InstallStep
import eu.kanade.tachiyomi.extension.manga.MangaExtensionManager
import eu.kanade.tachiyomi.extension.manga.model.MangaExtension
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Duration.Companion.seconds

class MangaExtensionsScreenModel(
    preferences: SourcePreferences = Injekt.get(),
    private val extensionManager: MangaExtensionManager = Injekt.get(),
    private val getExtensions: GetMangaExtensionsByType = Injekt.get(),
) : StateScreenModel<MangaExtensionsState>(MangaExtensionsState()) {

    private val _query: MutableStateFlow<String?> = MutableStateFlow(null)
    val query: StateFlow<String?> = _query.asStateFlow()

    private var _currentDownloads = MutableStateFlow<Map<String, InstallStep>>(hashMapOf())

    init {
        val context = Injekt.get<Application>()
        val extensionMapper: (Map<String, InstallStep>) -> ((MangaExtension) -> MangaExtensionUiModel) = { map ->
            {
                MangaExtensionUiModel.Item(it, map[it.pkgName] ?: InstallStep.Idle)
            }
        }
        val queryFilter: (String) -> ((MangaExtension) -> Boolean) = { query ->
            filter@{ extension ->
                if (query.isEmpty()) return@filter true
                query.split(",").any { _input ->
                    val input = _input.trim()
                    if (input.isEmpty()) return@any false
                    when (extension) {
                        is MangaExtension.Available -> {
                            extension.sources.any {
                                it.name.contains(input, ignoreCase = true) ||
                                    it.baseUrl.contains(input, ignoreCase = true) ||
                                    it.id == input.toLongOrNull()
                            } || extension.name.contains(input, ignoreCase = true)
                        }
                        is MangaExtension.Installed -> {
                            extension.sources.any {
                                it.name.contains(input, ignoreCase = true) ||
                                    it.id == input.toLongOrNull() ||
                                    if (it is HttpSource) { it.baseUrl.contains(input, ignoreCase = true) } else false
                            } || extension.name.contains(input, ignoreCase = true)
                        }
                        is MangaExtension.Untrusted -> extension.name.contains(input, ignoreCase = true)
                    }
                }
            }
        }

        coroutineScope.launchIO {
            combine(
                _query,
                _currentDownloads,
                getExtensions.subscribe(),
            ) { query, downloads, (_updates, _installed, _available, _untrusted) ->
                val searchQuery = query ?: ""

                val languagesWithExtensions = _available
                    .filter(queryFilter(searchQuery))
                    .groupBy { it.lang }
                    .toSortedMap(LocaleHelper.comparator)
                    .flatMap { (lang, exts) ->
                        listOf(
                            MangaExtensionUiModel.Header.Text(LocaleHelper.getSourceDisplayName(lang, context)),
                            *exts.map(extensionMapper(downloads)).toTypedArray(),
                        )
                    }

                val items = mutableListOf<MangaExtensionUiModel>()

                val updates = _updates.filter(queryFilter(searchQuery)).map(extensionMapper(downloads))
                if (updates.isNotEmpty()) {
                    items.add(MangaExtensionUiModel.Header.Resource(R.string.ext_updates_pending))
                    items.addAll(updates)
                }

                val installed = _installed.filter(queryFilter(searchQuery)).map(extensionMapper(downloads))
                val untrusted = _untrusted.filter(queryFilter(searchQuery)).map(extensionMapper(downloads))
                if (installed.isNotEmpty() || untrusted.isNotEmpty()) {
                    items.add(MangaExtensionUiModel.Header.Resource(R.string.ext_installed))
                    items.addAll(installed)
                    items.addAll(untrusted)
                }

                if (languagesWithExtensions.isNotEmpty()) {
                    items.addAll(languagesWithExtensions)
                }

                items
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

        preferences.mangaExtensionUpdatesCount().changes()
            .onEach { mutableState.update { state -> state.copy(updates = it) } }
            .launchIn(coroutineScope)
    }

    fun search(query: String?) {
        coroutineScope.launchIO {
            _query.emit(query)
        }
    }

    fun updateAllExtensions() {
        coroutineScope.launchIO {
            with(state.value) {
                if (isEmpty) return@launchIO
                items
                    .mapNotNull {
                        when {
                            it !is MangaExtensionUiModel.Item -> null
                            it.extension !is MangaExtension.Installed -> null
                            !it.extension.hasUpdate -> null
                            else -> it.extension
                        }
                    }
                    .forEach { updateExtension(it) }
            }
        }
    }

    fun installExtension(extension: MangaExtension.Available) {
        extensionManager.installExtension(extension).subscribeToInstallUpdate(extension)
    }

    fun updateExtension(extension: MangaExtension.Installed) {
        extensionManager.updateExtension(extension).subscribeToInstallUpdate(extension)
    }

    fun cancelInstallUpdateExtension(extension: MangaExtension) {
        extensionManager.cancelInstallUpdateExtension(extension)
    }

    private fun removeDownloadState(extension: MangaExtension) {
        _currentDownloads.update { _map ->
            val map = _map.toMutableMap()
            map.remove(extension.pkgName)
            map
        }
    }

    private fun addDownloadState(extension: MangaExtension, installStep: InstallStep) {
        _currentDownloads.update { _map ->
            val map = _map.toMutableMap()
            map[extension.pkgName] = installStep
            map
        }
    }

    private fun Observable<InstallStep>.subscribeToInstallUpdate(extension: MangaExtension) {
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

data class MangaExtensionsState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val items: List<MangaExtensionUiModel> = emptyList(),
    val updates: Int = 0,
) {
    val isEmpty = items.isEmpty()
}

sealed interface MangaExtensionUiModel {
    sealed interface Header : MangaExtensionUiModel {
        data class Resource(@StringRes val textRes: Int) : Header
        data class Text(val text: String) : Header
    }

    data class Item(
        val extension: MangaExtension,
        val installStep: InstallStep,
    ) : MangaExtensionUiModel
}
