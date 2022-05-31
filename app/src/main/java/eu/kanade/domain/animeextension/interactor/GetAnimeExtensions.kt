package eu.kanade.domain.animeextension.interactor

import eu.kanade.core.util.asFlow
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.extension.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.model.AnimeExtension
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

typealias ExtensionSegregation = Triple<List<AnimeExtension.Installed>, List<AnimeExtension.Untrusted>, List<AnimeExtension.Available>>

class GetAnimeExtensions(
    private val preferences: PreferencesHelper,
    private val extensionManager: AnimeExtensionManager,
) {

    fun subscribe(): Flow<ExtensionSegregation> {
        val showNsfwSources = preferences.showNsfwSource().get()

        return combine(
            preferences.enabledLanguages().asFlow(),
            extensionManager.getInstalledExtensionsObservable().asFlow(),
            extensionManager.getUntrustedExtensionsObservable().asFlow(),
            extensionManager.getAvailableExtensionsObservable().asFlow(),
        ) { _activeLanguages, _installed, _untrusted, _available ->

            val installed = _installed
                .filter { it.hasUpdate.not() && (showNsfwSources || it.isNsfw.not()) }
                .sortedWith(
                    compareBy<AnimeExtension.Installed> { it.isObsolete.not() }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name },
                )

            val untrusted = _untrusted
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

            val available = _available
                .filter { extension ->
                    _installed.none { it.pkgName == extension.pkgName } &&
                        _untrusted.none { it.pkgName == extension.pkgName } &&
                        extension.lang in _activeLanguages &&
                        (showNsfwSources || extension.isNsfw.not())
                }

            Triple(installed, untrusted, available)
        }
    }
}
