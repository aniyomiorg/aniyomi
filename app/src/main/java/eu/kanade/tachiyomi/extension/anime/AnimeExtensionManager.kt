package eu.kanade.tachiyomi.extension.anime

import android.content.Context
import android.graphics.drawable.Drawable
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.extension.InstallStep
import eu.kanade.tachiyomi.extension.anime.api.AnimeExtensionGithubApi
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import eu.kanade.tachiyomi.extension.anime.model.AnimeLoadResult
import eu.kanade.tachiyomi.extension.anime.model.AvailableAnimeSources
import eu.kanade.tachiyomi.extension.anime.util.AnimeExtensionInstallReceiver
import eu.kanade.tachiyomi.extension.anime.util.AnimeExtensionInstaller
import eu.kanade.tachiyomi.extension.anime.util.AnimeExtensionLoader
import eu.kanade.tachiyomi.util.preference.plusAssign
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import logcat.LogPriority
import rx.Observable
import tachiyomi.core.util.lang.launchNow
import tachiyomi.core.util.lang.withUIContext
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.source.anime.model.AnimeSourceData
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Locale

/**
 * The manager of anime extensions installed as another apk which extend the available sources. It handles
 * the retrieval of remotely available anime extensions as well as installing, updating and removing them.
 * To avoid malicious distribution, every anime extension must be signed and it will only be loaded if its
 * signature is trusted, otherwise the user will be prompted with a warning to trust it before being
 * loaded.
 *
 * @param context The application context.
 * @param preferences The application preferences.
 */
class AnimeExtensionManager(
    private val context: Context,
    private val preferences: SourcePreferences = Injekt.get(),
) {

    var isInitialized = false
        private set

    /**
     * API where all the available anime extensions can be found.
     */
    private val api = AnimeExtensionGithubApi()

    /**
     * The installer which installs, updates and uninstalls the anime extensions.
     */
    private val installer by lazy { AnimeExtensionInstaller(context) }

    private val iconMap = mutableMapOf<String, Drawable>()

    private val _installedAnimeExtensionsFlow = MutableStateFlow(emptyList<AnimeExtension.Installed>())
    val installedExtensionsFlow = _installedAnimeExtensionsFlow.asStateFlow()

    private var subLanguagesEnabledOnFirstRun = preferences.enabledLanguages().isSet()

    fun getAppIconForSource(sourceId: Long): Drawable? {
        val pkgName = _installedAnimeExtensionsFlow.value.find { ext -> ext.sources.any { it.id == sourceId } }?.pkgName
        if (pkgName != null) {
            return iconMap[pkgName] ?: iconMap.getOrPut(pkgName) { context.packageManager.getApplicationIcon(pkgName) }
        }
        return null
    }

    private val _availableAnimeExtensionsFlow = MutableStateFlow(emptyList<AnimeExtension.Available>())
    val availableExtensionsFlow = _availableAnimeExtensionsFlow.asStateFlow()

    private var availableAnimeExtensionsSourcesData: Map<Long, AnimeSourceData> = emptyMap()

    private fun setupAvailableAnimeExtensionsSourcesDataMap(animeextensions: List<AnimeExtension.Available>) {
        if (animeextensions.isEmpty()) return
        availableAnimeExtensionsSourcesData = animeextensions
            .flatMap { ext -> ext.sources.map { it.toAnimeSourceData() } }
            .associateBy { it.id }
    }

    fun getSourceData(id: Long) = availableAnimeExtensionsSourcesData[id]

    private val _untrustedAnimeExtensionsFlow = MutableStateFlow(emptyList<AnimeExtension.Untrusted>())
    val untrustedExtensionsFlow = _untrustedAnimeExtensionsFlow.asStateFlow()

    init {
        initAnimeExtensions()
        AnimeExtensionInstallReceiver(AnimeInstallationListener()).register(context)
    }

    /**
     * Loads and registers the installed animeextensions.
     */
    private fun initAnimeExtensions() {
        val animeextensions = AnimeExtensionLoader.loadExtensions(context)

        _installedAnimeExtensionsFlow.value = animeextensions
            .filterIsInstance<AnimeLoadResult.Success>()
            .map { it.extension }

        _untrustedAnimeExtensionsFlow.value = animeextensions
            .filterIsInstance<AnimeLoadResult.Untrusted>()
            .map { it.extension }

        isInitialized = true
    }

    /**
     * Finds the available anime extensions in the [api] and updates [availableExtensions].
     */
    suspend fun findAvailableExtensions() {
        val extensions: List<AnimeExtension.Available> = try {
            api.findExtensions()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            withUIContext { context.toast(R.string.extension_api_error) }
            emptyList()
        }

        enableAdditionalSubLanguages(extensions)

        _availableAnimeExtensionsFlow.value = extensions
        updatedInstalledAnimeExtensionsStatuses(extensions)
        setupAvailableAnimeExtensionsSourcesDataMap(extensions)
    }

    /**
     * Enables the additional sub-languages in the app first run. This addresses
     * the issue where users still need to enable some specific languages even when
     * the device language is inside that major group. As an example, if a user
     * has a zh device language, the app will also enable zh-Hans and zh-Hant.
     *
     * If the user have already changed the enabledLanguages preference value once,
     * the new languages will not be added to respect the user enabled choices.
     */
    private fun enableAdditionalSubLanguages(animeextensions: List<AnimeExtension.Available>) {
        if (subLanguagesEnabledOnFirstRun || animeextensions.isEmpty()) {
            return
        }

        // Use the source lang as some aren't present on the animeextension level.
        val availableLanguages = animeextensions
            .flatMap(AnimeExtension.Available::sources)
            .distinctBy(AvailableAnimeSources::lang)
            .map(AvailableAnimeSources::lang)

        val deviceLanguage = Locale.getDefault().language
        val defaultLanguages = preferences.enabledLanguages().defaultValue()
        val languagesToEnable = availableLanguages.filter {
            it != deviceLanguage && it.startsWith(deviceLanguage)
        }

        preferences.enabledLanguages().set(defaultLanguages + languagesToEnable)
        subLanguagesEnabledOnFirstRun = true
    }

    /**
     * Sets the update field of the installed animeextensions with the given [availableAnimeExtensions].
     *
     * @param availableAnimeExtensions The list of animeextensions given by the [api].
     */
    private fun updatedInstalledAnimeExtensionsStatuses(availableAnimeExtensions: List<AnimeExtension.Available>) {
        if (availableAnimeExtensions.isEmpty()) {
            preferences.animeExtensionUpdatesCount().set(0)
            return
        }

        val mutInstalledAnimeExtensions = _installedAnimeExtensionsFlow.value.toMutableList()
        var changed = false

        for ((index, installedExt) in mutInstalledAnimeExtensions.withIndex()) {
            val pkgName = installedExt.pkgName
            val availableExt = availableAnimeExtensions.find { it.pkgName == pkgName }

            if (!installedExt.isUnofficial && availableExt == null && !installedExt.isObsolete) {
                mutInstalledAnimeExtensions[index] = installedExt.copy(isObsolete = true)
                changed = true
            } else if (availableExt != null) {
                val hasUpdate = installedExt.updateExists(availableExt)

                if (installedExt.hasUpdate != hasUpdate) {
                    mutInstalledAnimeExtensions[index] = installedExt.copy(hasUpdate = hasUpdate)
                    changed = true
                }
            }
        }
        if (changed) {
            _installedAnimeExtensionsFlow.value = mutInstalledAnimeExtensions
        }
        updatePendingUpdatesCount()
    }

    /**
     * Returns an observable of the installation process for the given anime extension. It will complete
     * once the anime extension is installed or throws an error. The process will be canceled if
     * unsubscribed before its completion.
     *
     * @param extension The anime extension to be installed.
     */
    fun installExtension(extension: AnimeExtension.Available): Observable<InstallStep> {
        return installer.downloadAndInstall(api.getApkUrl(extension), extension)
    }

    /**
     * Returns an observable of the installation process for the given anime extension. It will complete
     * once the anime extension is updated or throws an error. The process will be canceled if
     * unsubscribed before its completion.
     *
     * @param extension The anime extension to be updated.
     */
    fun updateExtension(extension: AnimeExtension.Installed): Observable<InstallStep> {
        val availableExt = _availableAnimeExtensionsFlow.value.find { it.pkgName == extension.pkgName }
            ?: return Observable.empty()
        return installExtension(availableExt)
    }

    fun cancelInstallUpdateExtension(extension: AnimeExtension) {
        installer.cancelInstall(extension.pkgName)
    }

    /**
     * Sets to "installing" status of an anime extension installation.
     *
     * @param downloadId The id of the download.
     */
    fun setInstalling(downloadId: Long) {
        installer.updateInstallStep(downloadId, InstallStep.Installing)
    }

    fun updateInstallStep(downloadId: Long, step: InstallStep) {
        installer.updateInstallStep(downloadId, step)
    }

    /**
     * Uninstalls the anime extension that matches the given package name.
     *
     * @param pkgName The package name of the application to uninstall.
     */
    fun uninstallExtension(pkgName: String) {
        installer.uninstallApk(pkgName)
    }

    /**
     * Adds the given signature to the list of trusted signatures. It also loads in background the
     * anime extensions that match this signature.
     *
     * @param signature The signature to whitelist.
     */
    fun trustSignature(signature: String) {
        val untrustedSignatures = _untrustedAnimeExtensionsFlow.value.map { it.signatureHash }.toSet()
        if (signature !in untrustedSignatures) return

        AnimeExtensionLoader.trustedSignatures += signature
        preferences.trustedSignatures() += signature

        val nowTrustedAnimeExtensions = _untrustedAnimeExtensionsFlow.value.filter { it.signatureHash == signature }
        _untrustedAnimeExtensionsFlow.value -= nowTrustedAnimeExtensions

        val ctx = context
        launchNow {
            nowTrustedAnimeExtensions
                .map { animeextension ->
                    async { AnimeExtensionLoader.loadExtensionFromPkgName(ctx, animeextension.pkgName) }
                }
                .map { it.await() }
                .forEach { result ->
                    if (result is AnimeLoadResult.Success) {
                        registerNewExtension(result.extension)
                    }
                }
        }
    }

    /**
     * Registers the given anime extension in this and the source managers.
     *
     * @param extension The anime extension to be registered.
     */
    private fun registerNewExtension(extension: AnimeExtension.Installed) {
        _installedAnimeExtensionsFlow.value += extension
    }

    /**
     * Registers the given updated anime extension in this and the source managers previously removing
     * the outdated ones.
     *
     * @param extension The anime extension to be registered.
     */
    private fun registerUpdatedExtension(extension: AnimeExtension.Installed) {
        val mutInstalledAnimeExtensions = _installedAnimeExtensionsFlow.value.toMutableList()
        val oldAnimeExtension = mutInstalledAnimeExtensions.find { it.pkgName == extension.pkgName }
        if (oldAnimeExtension != null) {
            mutInstalledAnimeExtensions -= oldAnimeExtension
        }
        mutInstalledAnimeExtensions += extension
        _installedAnimeExtensionsFlow.value = mutInstalledAnimeExtensions
    }

    /**
     * Unregisters the animeextension in this and the source managers given its package name. Note this
     * method is called for every uninstalled application in the system.
     *
     * @param pkgName The package name of the uninstalled application.
     */
    private fun unregisterAnimeExtension(pkgName: String) {
        val installedAnimeExtension = _installedAnimeExtensionsFlow.value.find { it.pkgName == pkgName }
        if (installedAnimeExtension != null) {
            _installedAnimeExtensionsFlow.value -= installedAnimeExtension
        }
        val untrustedAnimeExtension = _untrustedAnimeExtensionsFlow.value.find { it.pkgName == pkgName }
        if (untrustedAnimeExtension != null) {
            _untrustedAnimeExtensionsFlow.value -= untrustedAnimeExtension
        }
    }

    /**
     * Listener which receives events of the anime extensions being installed, updated or removed.
     */
    private inner class AnimeInstallationListener : AnimeExtensionInstallReceiver.Listener {

        override fun onExtensionInstalled(extension: AnimeExtension.Installed) {
            registerNewExtension(extension.withUpdateCheck())
            updatePendingUpdatesCount()
        }

        override fun onExtensionUpdated(extension: AnimeExtension.Installed) {
            registerUpdatedExtension(extension.withUpdateCheck())
            updatePendingUpdatesCount()
        }

        override fun onExtensionUntrusted(extension: AnimeExtension.Untrusted) {
            _untrustedAnimeExtensionsFlow.value += extension
        }

        override fun onPackageUninstalled(pkgName: String) {
            unregisterAnimeExtension(pkgName)
            updatePendingUpdatesCount()
        }
    }

    /**
     * AnimeExtension method to set the update field of an installed anime extension.
     */
    private fun AnimeExtension.Installed.withUpdateCheck(): AnimeExtension.Installed {
        return if (updateExists()) {
            copy(hasUpdate = true)
        } else {
            this
        }
    }

    private fun AnimeExtension.Installed.updateExists(availableAnimeExtension: AnimeExtension.Available? = null): Boolean {
        val availableExt = availableAnimeExtension ?: _availableAnimeExtensionsFlow.value.find { it.pkgName == pkgName }
        if (isUnofficial || availableExt == null) return false

        return (availableExt.versionCode > versionCode || availableExt.libVersion > libVersion)
    }

    private fun updatePendingUpdatesCount() {
        preferences.animeExtensionUpdatesCount().set(_installedAnimeExtensionsFlow.value.count { it.hasUpdate })
    }
}
