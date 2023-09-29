package eu.kanade.presentation.more.settings.screen

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.source.service.SourcePreferences.DataSaver
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.debug.DebugInfoScreen
import eu.kanade.presentation.util.collectAsState
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.cache.EpisodeCache
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadCache
import eu.kanade.tachiyomi.data.download.manga.MangaDownloadCache
import eu.kanade.tachiyomi.data.library.anime.AnimeLibraryUpdateJob
import eu.kanade.tachiyomi.data.library.manga.MangaLibraryUpdateJob
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.network.PREF_DOH_360
import eu.kanade.tachiyomi.network.PREF_DOH_ADGUARD
import eu.kanade.tachiyomi.network.PREF_DOH_ALIDNS
import eu.kanade.tachiyomi.network.PREF_DOH_CLOUDFLARE
import eu.kanade.tachiyomi.network.PREF_DOH_CONTROLD
import eu.kanade.tachiyomi.network.PREF_DOH_DNSPOD
import eu.kanade.tachiyomi.network.PREF_DOH_GOOGLE
import eu.kanade.tachiyomi.network.PREF_DOH_MULLVAD
import eu.kanade.tachiyomi.network.PREF_DOH_NJALLA
import eu.kanade.tachiyomi.network.PREF_DOH_QUAD101
import eu.kanade.tachiyomi.network.PREF_DOH_QUAD9
import eu.kanade.tachiyomi.network.PREF_DOH_SHECAN
import eu.kanade.tachiyomi.util.CrashLogUtil
import eu.kanade.tachiyomi.util.system.isPreviewBuildType
import eu.kanade.tachiyomi.util.system.isReleaseBuildType
import eu.kanade.tachiyomi.util.system.isShizukuInstalled
import eu.kanade.tachiyomi.util.system.powerManager
import eu.kanade.tachiyomi.util.system.setDefaultSettings
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import logcat.LogPriority
import okhttp3.Headers
import tachiyomi.core.util.lang.launchNonCancellable
import tachiyomi.core.util.lang.withUIContext
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.entries.manga.repository.MangaRepository
import tachiyomi.domain.library.service.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

object SettingsAdvancedScreen : SearchableSettings {
    @ReadOnlyComposable
    @Composable
    @StringRes
    override fun getTitleRes() = R.string.pref_category_advanced

    @Composable
    override fun getPreferences(): List<Preference> {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val basePreferences = remember { Injekt.get<BasePreferences>() }
        val networkPreferences = remember { Injekt.get<NetworkPreferences>() }

        return listOf(
            Preference.PreferenceItem.SwitchPreference(
                pref = basePreferences.acraEnabled(),
                title = stringResource(R.string.pref_enable_acra),
                subtitle = stringResource(R.string.pref_acra_summary),
                enabled = isPreviewBuildType || isReleaseBuildType,
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(R.string.pref_dump_crash_logs),
                subtitle = stringResource(R.string.pref_dump_crash_logs_summary),
                onClick = {
                    scope.launch {
                        CrashLogUtil(context).dumpLogs()
                    }
                },
            ),
            Preference.PreferenceItem.SwitchPreference(
                pref = networkPreferences.verboseLogging(),
                title = stringResource(R.string.pref_verbose_logging),
                subtitle = stringResource(R.string.pref_verbose_logging_summary),
                onValueChanged = {
                    context.toast(R.string.requires_app_restart)
                    true
                },
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(R.string.pref_debug_info),
                onClick = { navigator.push(DebugInfoScreen) },
            ),
            getBackgroundActivityGroup(),
            getDataGroup(),
            getNetworkGroup(networkPreferences = networkPreferences),
            getLibraryGroup(),
            getExtensionsGroup(basePreferences = basePreferences),
            // SY -->
            getDataSaverGroup(),
            // SY <--
        )
    }

    @Composable
    private fun getBackgroundActivityGroup(): Preference.PreferenceGroup {
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current

        return Preference.PreferenceGroup(
            title = stringResource(R.string.label_background_activity),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_disable_battery_optimization),
                    subtitle = stringResource(R.string.pref_disable_battery_optimization_summary),
                    onClick = {
                        val packageName: String = context.packageName
                        if (!context.powerManager.isIgnoringBatteryOptimizations(packageName)) {
                            try {
                                @SuppressLint("BatteryLife")
                                val intent = Intent().apply {
                                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                    data = "package:$packageName".toUri()
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                context.toast(R.string.battery_optimization_setting_activity_not_found)
                            }
                        } else {
                            context.toast(R.string.battery_optimization_disabled)
                        }
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = "Don't kill my app!",
                    subtitle = stringResource(R.string.about_dont_kill_my_app),
                    onClick = { uriHandler.openUri("https://dontkillmyapp.com/") },
                ),
            ),
        )
    }

    @Composable
    private fun getDataGroup(): Preference.PreferenceGroup {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val libraryPreferences = remember { Injekt.get<LibraryPreferences>() }

        val chapterCache = remember { Injekt.get<ChapterCache>() }
        val episodeCache = remember { Injekt.get<EpisodeCache>() }
        var readableSizeSema by remember { mutableStateOf(0) }
        val readableSize = remember(readableSizeSema) { chapterCache.readableSize }
        val readableAnimeSize = remember(readableSizeSema) { episodeCache.readableSize }

        return Preference.PreferenceGroup(
            title = stringResource(R.string.label_data),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_clear_chapter_cache),
                    subtitle = stringResource(R.string.used_cache_both, readableAnimeSize, readableSize),
                    onClick = {
                        scope.launchNonCancellable {
                            try {
                                val deletedFiles = chapterCache.clear() + episodeCache.clear()
                                withUIContext {
                                    context.toast(context.getString(R.string.cache_deleted, deletedFiles))
                                    readableSizeSema++
                                }
                            } catch (e: Throwable) {
                                logcat(LogPriority.ERROR, e)
                                withUIContext { context.toast(R.string.cache_delete_error) }
                            }
                        }
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = libraryPreferences.autoClearItemCache(),
                    title = stringResource(R.string.pref_auto_clear_chapter_cache),
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_invalidate_download_cache),
                    subtitle = stringResource(R.string.pref_invalidate_download_cache_summary),
                    onClick = {
                        Injekt.get<MangaDownloadCache>().invalidateCache()
                        Injekt.get<AnimeDownloadCache>().invalidateCache()
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_clear_manga_database),
                    subtitle = stringResource(R.string.pref_clear_manga_database_summary),
                    onClick = { navigator.push(ClearDatabaseScreen()) },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_clear_anime_database),
                    subtitle = stringResource(R.string.pref_clear_anime_database_summary),
                    onClick = { navigator.push(ClearAnimeDatabaseScreen()) },
                ),
            ),
        )
    }

    @Composable
    private fun getNetworkGroup(
        networkPreferences: NetworkPreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current
        val networkHelper = remember { Injekt.get<NetworkHelper>() }

        val userAgentPref = networkPreferences.defaultUserAgent()
        val userAgent by userAgentPref.collectAsState()

        return Preference.PreferenceGroup(
            title = stringResource(R.string.label_network),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_clear_cookies),
                    onClick = {
                        networkHelper.cookieJar.removeAll()
                        context.toast(R.string.cookies_cleared)
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_clear_webview_data),
                    onClick = {
                        try {
                            WebView(context).run {
                                setDefaultSettings()
                                clearCache(true)
                                clearFormData()
                                clearHistory()
                                clearSslPreferences()
                            }
                            WebStorage.getInstance().deleteAllData()
                            context.applicationInfo?.dataDir?.let { File("$it/app_webview/").deleteRecursively() }
                            context.toast(R.string.webview_data_deleted)
                        } catch (e: Throwable) {
                            logcat(LogPriority.ERROR, e)
                            context.toast(R.string.cache_delete_error)
                        }
                    },
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = networkPreferences.dohProvider(),
                    title = stringResource(R.string.pref_dns_over_https),
                    entries = mapOf(
                        -1 to stringResource(R.string.disabled),
                        PREF_DOH_CLOUDFLARE to "Cloudflare",
                        PREF_DOH_GOOGLE to "Google",
                        PREF_DOH_ADGUARD to "AdGuard",
                        PREF_DOH_QUAD9 to "Quad9",
                        PREF_DOH_ALIDNS to "AliDNS",
                        PREF_DOH_DNSPOD to "DNSPod",
                        PREF_DOH_360 to "360",
                        PREF_DOH_QUAD101 to "Quad 101",
                        PREF_DOH_MULLVAD to "Mullvad",
                        PREF_DOH_CONTROLD to "Control D",
                        PREF_DOH_NJALLA to "Njalla",
                        PREF_DOH_SHECAN to "Shecan",
                    ),
                    onValueChanged = {
                        context.toast(R.string.requires_app_restart)
                        true
                    },
                ),
                Preference.PreferenceItem.EditTextPreference(
                    pref = userAgentPref,
                    title = stringResource(R.string.pref_user_agent_string),
                    onValueChanged = {
                        try {
                            // OkHttp checks for valid values internally
                            Headers.Builder().add("User-Agent", it)
                        } catch (_: IllegalArgumentException) {
                            context.toast(R.string.error_user_agent_string_invalid)
                            return@EditTextPreference false
                        }
                        true
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_reset_user_agent_string),
                    enabled = remember(userAgent) { userAgent != userAgentPref.defaultValue() },
                    onClick = {
                        userAgentPref.delete()
                        context.toast(R.string.requires_app_restart)
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getLibraryGroup(): Preference.PreferenceGroup {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val trackManager = remember { Injekt.get<TrackManager>() }

        return Preference.PreferenceGroup(
            title = stringResource(R.string.label_library),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_refresh_library_covers),
                    onClick = {
                        MangaLibraryUpdateJob.startNow(context, target = MangaLibraryUpdateJob.Target.COVERS)
                        AnimeLibraryUpdateJob.startNow(context, target = AnimeLibraryUpdateJob.Target.COVERS)
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_refresh_library_tracking),
                    subtitle = stringResource(R.string.pref_refresh_library_tracking_summary),
                    enabled = trackManager.hasLoggedServices(),
                    onClick = {
                        MangaLibraryUpdateJob.startNow(context, target = MangaLibraryUpdateJob.Target.TRACKING)
                        AnimeLibraryUpdateJob.startNow(context, target = AnimeLibraryUpdateJob.Target.TRACKING)
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_reset_viewer_flags),
                    subtitle = stringResource(R.string.pref_reset_viewer_flags_summary),
                    onClick = {
                        scope.launchNonCancellable {
                            val success = Injekt.get<MangaRepository>().resetMangaViewerFlags()
                            withUIContext {
                                val message = if (success) {
                                    R.string.pref_reset_viewer_flags_success
                                } else {
                                    R.string.pref_reset_viewer_flags_error
                                }
                                context.toast(message)
                            }
                        }
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getExtensionsGroup(
        basePreferences: BasePreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current
        val extensionInstallerPref = basePreferences.extensionInstaller()
        var shizukuMissing by rememberSaveable { mutableStateOf(false) }

        if (shizukuMissing) {
            val dismiss = { shizukuMissing = false }
            AlertDialog(
                onDismissRequest = dismiss,
                title = { Text(text = stringResource(R.string.ext_installer_shizuku)) },
                text = { Text(text = stringResource(R.string.ext_installer_shizuku_unavailable_dialog)) },
                dismissButton = {
                    TextButton(onClick = dismiss) {
                        Text(text = stringResource(R.string.action_cancel))
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            dismiss()
                            uriHandler.openUri("https://shizuku.rikka.app/download")
                        },
                    ) {
                        Text(text = stringResource(android.R.string.ok))
                    }
                },
            )
        }
        return Preference.PreferenceGroup(
            title = stringResource(R.string.label_extensions),
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    pref = extensionInstallerPref,
                    title = stringResource(R.string.ext_installer_pref),
                    entries = extensionInstallerPref.entries
                        .associateWith { stringResource(it.titleResId) },
                    onValueChanged = {
                        if (it == BasePreferences.ExtensionInstaller.SHIZUKU &&
                            !context.isShizukuInstalled
                        ) {
                            shizukuMissing = true
                            false
                        } else {
                            true
                        }
                    },
                ),
            ),
        )
    }

    // SY -->
    @Composable
    private fun getDataSaverGroup(): Preference.PreferenceGroup {
        val sourcePreferences = remember { Injekt.get<SourcePreferences>() }
        val dataSaver by sourcePreferences.dataSaver().collectAsState()
        return Preference.PreferenceGroup(
            title = stringResource(R.string.data_saver),
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    pref = sourcePreferences.dataSaver(),
                    title = stringResource(R.string.data_saver),
                    subtitle = stringResource(R.string.data_saver_summary),
                    entries = mapOf(
                        DataSaver.NONE to stringResource(R.string.disabled),
                        DataSaver.BANDWIDTH_HERO to stringResource(R.string.bandwidth_hero),
                        DataSaver.WSRV_NL to stringResource(R.string.wsrv),
                        DataSaver.RESMUSH_IT to stringResource(R.string.resmush),
                    ),
                ),
                Preference.PreferenceItem.EditTextPreference(
                    pref = sourcePreferences.dataSaverServer(),
                    title = stringResource(R.string.bandwidth_data_saver_server),
                    subtitle = stringResource(R.string.data_saver_server_summary),
                    enabled = dataSaver == DataSaver.BANDWIDTH_HERO,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = sourcePreferences.dataSaverDownloader(),
                    title = stringResource(R.string.data_saver_downloader),
                    enabled = dataSaver != DataSaver.NONE,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = sourcePreferences.dataSaverIgnoreJpeg(),
                    title = stringResource(R.string.data_saver_ignore_jpeg),
                    enabled = dataSaver != DataSaver.NONE,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = sourcePreferences.dataSaverIgnoreGif(),
                    title = stringResource(R.string.data_saver_ignore_gif),
                    enabled = dataSaver != DataSaver.NONE,
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = sourcePreferences.dataSaverImageQuality(),
                    title = stringResource(R.string.data_saver_image_quality),
                    subtitle = stringResource(R.string.data_saver_image_quality_summary),
                    entries = listOf(
                        "10%",
                        "20%",
                        "40%",
                        "50%",
                        "70%",
                        "80%",
                        "90%",
                        "95%",
                    ).associateBy { it.trimEnd('%').toInt() },
                    enabled = dataSaver != DataSaver.NONE,
                ),
                kotlin.run {
                    val dataSaverImageFormatJpeg by sourcePreferences.dataSaverImageFormatJpeg().collectAsState()
                    Preference.PreferenceItem.SwitchPreference(
                        pref = sourcePreferences.dataSaverImageFormatJpeg(),
                        title = stringResource(R.string.data_saver_image_format),
                        subtitle = if (dataSaverImageFormatJpeg) {
                            stringResource(R.string.data_saver_image_format_summary_on)
                        } else {
                            stringResource(R.string.data_saver_image_format_summary_off)
                        },
                        enabled = dataSaver != DataSaver.NONE && dataSaver != DataSaver.RESMUSH_IT,
                    )
                },
                Preference.PreferenceItem.SwitchPreference(
                    pref = sourcePreferences.dataSaverColorBW(),
                    title = stringResource(R.string.data_saver_color_bw),
                    enabled = dataSaver == DataSaver.BANDWIDTH_HERO,
                ),
            ),
        )
    }
    // SY <--
}
