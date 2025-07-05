package eu.kanade.tachiyomi.util

import tachiyomi.core.common.preference.PreferenceStore

class LocalHttpServerHolder(
    private val preferenceStore: PreferenceStore,
) {
    fun port() = preferenceStore.getString("pref_cast_server_port", "8181")
}
