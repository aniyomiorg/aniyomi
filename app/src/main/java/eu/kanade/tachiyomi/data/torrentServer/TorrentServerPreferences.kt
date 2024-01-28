package eu.kanade.tachiyomi.data.torrentServer

import tachiyomi.core.preference.PreferenceStore

class TorrentServerPreferences(
    private val preferenceStore: PreferenceStore,
    ) {
    fun port() = preferenceStore.getString("pref_torrent_port", "8090")
}
