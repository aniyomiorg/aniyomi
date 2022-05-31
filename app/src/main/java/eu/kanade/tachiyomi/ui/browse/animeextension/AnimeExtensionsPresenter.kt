package eu.kanade.tachiyomi.ui.browse.animeextension

import android.app.Application
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eu.kanade.domain.animeextension.interactor.GetAnimeExtensionUpdates
import eu.kanade.domain.animeextension.interactor.GetAnimeExtensions
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.extension.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.model.AnimeExtension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeExtensionsPresenter(
    private val extensionManager: AnimeExtensionManager = Injekt.get(),
    private val getExtensionUpdates: GetAnimeExtensionUpdates = Injekt.get(),
    private val getExtensions: GetAnimeExtensions = Injekt.get(),
) : BasePresenter<AnimeExtensionsController>() {

    private val _query: MutableStateFlow<String> = MutableStateFlow("")

    private var _currentDownloads = MutableStateFlow<Map<String, InstallStep>>(hashMapOf())

    private val _state: MutableStateFlow<ExtensionState> = MutableStateFlow(ExtensionState.Uninitialized)
    val state: StateFlow<ExtensionState> = _state.asStateFlow()

    var isRefreshing: Boolean by mutableStateOf(true)

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        extensionManager.findAvailableExtensions()

        val context = Injekt.get<Application>()
        val extensionMapper: (Map<String, InstallStep>) -> ((AnimeExtension) -> ExtensionUiModel) = { map ->
            {
                ExtensionUiModel.Item(it, map[it.pkgName] ?: InstallStep.Idle)
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
                            extension.name.contains(input, ignoreCase = true) ||
                                extension.pkgName.contains(input, ignoreCase = true)
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

        launchIO {
            combine(
                _query,
                getExtensions.subscribe(),
                getExtensionUpdates.subscribe(),
                _currentDownloads,
            ) { query, (installed, untrusted, available), updates, downloads ->
                isRefreshing = false

                val languagesWithExtensions = available
                    .filter(queryFilter(query))
                    .groupBy { LocaleHelper.getSourceDisplayName(it.lang, context) }
                    .toSortedMap()
                    .flatMap { (key, value) ->
                        listOf(
                            ExtensionUiModel.Header.Text(key),
                            *value.map(extensionMapper(downloads)).toTypedArray(),
                        )
                    }

                val items = mutableListOf<ExtensionUiModel>()

                val updates = updates.filter(queryFilter(query)).map(extensionMapper(downloads))
                if (updates.isNotEmpty()) {
                    items.add(ExtensionUiModel.Header.Resource(R.string.ext_updates_pending))
                    items.addAll(updates)
                }

                val installed = installed.filter(queryFilter(query)).map(extensionMapper(downloads))
                val untrusted = untrusted.filter(queryFilter(query)).map(extensionMapper(downloads))
                if (installed.isNotEmpty() || untrusted.isNotEmpty()) {
                    items.add(ExtensionUiModel.Header.Resource(R.string.ext_installed))
                    items.addAll(installed)
                    items.addAll(untrusted)
                }

                if (languagesWithExtensions.isNotEmpty()) {
                    items.addAll(languagesWithExtensions)
                }

                items
            }.collectLatest {
                _state.value = ExtensionState.Initialized(it)
            }
        }
    }

    fun search(query: String) {
        launchIO {
            _query.emit(query)
        }
    }

    fun updateAllExtensions() {
        launchIO {
            val state = _state.value
            if (state !is ExtensionState.Initialized) return@launchIO
            state.list.mapNotNull {
                if (it !is ExtensionUiModel.Item) return@mapNotNull null
                if (it.extension !is AnimeExtension.Installed) return@mapNotNull null
                if (it.extension.hasUpdate.not()) return@mapNotNull null
                it.extension
            }.forEach {
                updateExtension(it)
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
        _currentDownloads.update { map ->
            val map = map.toMutableMap()
            map.remove(extension.pkgName)
            map
        }
    }

    private fun addDownloadState(extension: AnimeExtension, installStep: InstallStep) {
        _currentDownloads.update { map ->
            val map = map.toMutableMap()
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
        isRefreshing = true
        extensionManager.findAvailableExtensions()
    }

    fun trustSignature(signatureHash: String) {
        extensionManager.trustSignature(signatureHash)
    }
}

sealed interface ExtensionUiModel {
    sealed interface Header : ExtensionUiModel {
        data class Resource(@StringRes val textRes: Int) : Header
        data class Text(val text: String) : Header
    }
    data class Item(
        val extension: AnimeExtension,
        val installStep: InstallStep,
    ) : ExtensionUiModel {

        fun key(): String {
            return when (extension) {
                is AnimeExtension.Installed ->
                    if (extension.hasUpdate) "update_${extension.pkgName}" else extension.pkgName
                else -> extension.pkgName
            }
        }
    }
}

sealed class ExtensionState {
    object Uninitialized : ExtensionState()
    data class Initialized(val list: List<ExtensionUiModel>) : ExtensionState()
}
