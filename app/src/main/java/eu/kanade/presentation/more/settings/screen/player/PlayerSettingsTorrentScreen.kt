package eu.kanade.presentation.more.settings.screen.player

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.net.toUri
import aniyomi.core.common.torrent.ProxyMode
import aniyomi.core.common.torrent.TorrentPreferences
import eu.kanade.core.preference.asState
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.tachiyomi.data.torrent.service.TorrentServerService
import kotlinx.collections.immutable.toPersistentMap
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object PlayerSettingsTorrentScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = AYMR.strings.pref_player_torrents

    @Composable
    override fun getPreferences(): List<Preference> {
        val torrentPreferences = remember { Injekt.get<TorrentPreferences>() }
        val scope = rememberCoroutineScope()

        val port = torrentPreferences.torrServerPort()
        val trackers = torrentPreferences.torrServerTrackers()
        val proxyMode = torrentPreferences.torrServerProxyMode()
        val proxyUrl = torrentPreferences.torrServerProxyUrl()

        return listOf(
            Preference.PreferenceItem.EditTextInfoPreference(
                preference = port,
                dialogSubtitle = stringResource(AYMR.strings.pref_player_torrents_port_summary),
                title = stringResource(AYMR.strings.pref_player_torrents_port),
                validate = { pref ->
                    val port = pref.toIntOrNull()
                        ?: return@EditTextInfoPreference false

                    if (port !in 0..65535) {
                        return@EditTextInfoPreference false
                    }

                    true
                },
                errorMessage = { _ ->
                    stringResource(AYMR.strings.pref_player_torrents_port_error)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            ),
            Preference.PreferenceItem.MultiLineEditTextPreference(
                preference = trackers,
                title = stringResource(AYMR.strings.pref_player_torrents_trackers),
                subtitle = trackers.asState(scope).value
                    .lines().take(2)
                    .joinToString(
                        separator = "\n",
                        postfix = if (trackers.asState(scope).value.lines().size > 2) "\n..." else "",
                    ),
                onValueChanged = {
                    TorrentServerService.stop()
                    true
                },
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(AYMR.strings.pref_player_torrents_trackers_reset),
                enabled = remember(trackers) { trackers.get() != trackers.defaultValue() },
                onClick = {
                    trackers.delete()
                },
            ),
            Preference.PreferenceItem.ListPreference(
                preference = proxyMode,
                entries = ProxyMode.entries.associateWith {
                    val titleRes = when (it) {
                        ProxyMode.None -> AYMR.strings.pref_player_torrents_proxy_mode_none
                        ProxyMode.Tracker -> AYMR.strings.pref_player_torrents_proxy_mode_tracker
                        ProxyMode.Peers -> AYMR.strings.pref_player_torrents_proxy_mode_peers
                        ProxyMode.Full -> AYMR.strings.pref_player_torrents_proxy_mode_full
                    }
                    stringResource(titleRes)
                }.toPersistentMap(),
                title = stringResource(AYMR.strings.pref_player_torrents_proxy_mode),
            ),
            Preference.PreferenceItem.EditTextInfoPreference(
                preference = proxyUrl,
                title = stringResource(AYMR.strings.pref_player_torrents_proxy_url),
                dialogSubtitle = stringResource(AYMR.strings.pref_player_torrents_proxy_url_dialog),
                validate = { pref ->
                    val uri = pref.toUri()

                    if (uri.scheme == null || uri.host == null) {
                        return@EditTextInfoPreference false
                    }

                    if (uri.scheme !in setOf("http", "https", "socks4", "socks4a", "socks5", "socks5h")) {
                        return@EditTextInfoPreference false
                    }

                    true
                },
                errorMessage = { pref ->
                    val uri = pref.toUri()

                    if (uri.scheme == null || uri.host == null) {
                        return@EditTextInfoPreference stringResource(
                            AYMR.strings.pref_player_torrents_proxy_url_invalid_uri,
                        )
                    }

                    if (uri.scheme !in setOf("http", "https", "socks4", "socks4a", "socks5", "socks5h")) {
                        return@EditTextInfoPreference stringResource(
                            AYMR.strings.pref_player_torrents_proxy_url_invalid_protocol,
                            uri.scheme!!,
                        )
                    }

                    ""
                },
                enabled = proxyMode.asState(scope).value != ProxyMode.None,
            ),
        )
    }
}
