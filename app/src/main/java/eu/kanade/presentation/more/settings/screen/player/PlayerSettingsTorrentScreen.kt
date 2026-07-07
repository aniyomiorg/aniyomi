package eu.kanade.presentation.more.settings.screen.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.net.toUri
import aniyomi.core.common.torrent.ProxyMode
import aniyomi.core.common.torrent.TorrentPreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.tachiyomi.data.torrent.service.TorrentServerService
import kotlinx.collections.immutable.toPersistentMap
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object PlayerSettingsTorrentScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = AYMR.strings.pref_player_torrents

    @Composable
    override fun getPreferences(): List<Preference> {
        var isDialogShown by rememberSaveable { mutableStateOf(false) }

        val torrentPreferences = remember { Injekt.get<TorrentPreferences>() }

        val torrentEnablePref = torrentPreferences.torrServerEnable()
        val torrentEnable by torrentEnablePref.collectAsState()
        val shownNoticePref = torrentPreferences.torrServerShownNotice()
        val shownNotice by shownNoticePref.collectAsState()

        val portPref = torrentPreferences.torrServerPort()
        val trackersPref = torrentPreferences.torrServerTrackers()
        val trackers by trackersPref.collectAsState()
        val proxyModePref = torrentPreferences.torrServerProxyMode()
        val proxyMode by proxyModePref.collectAsState()
        val proxyUrlPref = torrentPreferences.torrServerProxyUrl()

        if (isDialogShown) {
            AlertDialog(
                onDismissRequest = { isDialogShown = false },
                title = { Text(stringResource(AYMR.strings.pref_player_torrents_notice)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium)) {
                        Text(stringResource(AYMR.strings.pref_player_torrents_notice_text))
                        Text(stringResource(AYMR.strings.pref_player_torrents_notice_footer))
                    }
                },
                confirmButton = {
                    TextButton(onClick = { isDialogShown = false }) {
                        Text(text = stringResource(MR.strings.action_ok))
                    }
                },
            )
        }

        return listOf(
            Preference.PreferenceItem.SwitchPreference(
                preference = torrentEnablePref,
                title = stringResource(AYMR.strings.pref_player_torrents_enable),
                onValueChanged = {
                    if (it && !shownNotice) {
                        isDialogShown = true
                        shownNoticePref.set(true)
                    }
                    if (!it) {
                        TorrentServerService.stop()
                    }
                    true
                },
            ),
            Preference.PreferenceItem.EditTextInfoPreference(
                preference = portPref,
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
                enabled = torrentEnable,
            ),
            Preference.PreferenceItem.MultiLineEditTextPreference(
                preference = trackersPref,
                title = stringResource(AYMR.strings.pref_player_torrents_trackers),
                subtitle = remember(trackers) {
                    trackers.lines().take(2)
                        .joinToString(
                            separator = "\n",
                            postfix = if (trackers.lines().size > 2) "\n..." else "",
                        )
                },
                onValueChanged = {
                    TorrentServerService.stop()
                    true
                },
                enabled = torrentEnable,
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(AYMR.strings.pref_player_torrents_trackers_reset),
                enabled = remember(torrentEnable, trackersPref) {
                    torrentEnable && trackersPref.get() != trackersPref.defaultValue()
                },
                onClick = {
                    trackersPref.delete()
                },
            ),
            Preference.PreferenceItem.ListPreference(
                preference = proxyModePref,
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
                enabled = torrentEnable,
            ),
            Preference.PreferenceItem.EditTextInfoPreference(
                preference = proxyUrlPref,
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
                enabled = torrentEnable && proxyMode != ProxyMode.None,
            ),
        )
    }
}
