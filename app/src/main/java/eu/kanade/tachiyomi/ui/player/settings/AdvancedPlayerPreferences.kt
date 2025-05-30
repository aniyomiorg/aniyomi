package eu.kanade.tachiyomi.ui.player.settings

import tachiyomi.core.common.preference.PreferenceStore

class AdvancedPlayerPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun mpvUserFiles() = preferenceStore.getBoolean("mpv_scripts", false)
    fun mpvConf() = preferenceStore.getString("pref_mpv_conf", "")
    fun mpvInput() = preferenceStore.getString("pref_mpv_input", "")

    // Non-preference

    fun playerStatisticsPage() = preferenceStore.getInt("pref_player_statistics_page", 0)
}
