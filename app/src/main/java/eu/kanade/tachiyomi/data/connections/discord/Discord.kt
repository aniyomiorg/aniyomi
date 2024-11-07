// AM (DISCORD) -->

// Taken from Animiru. Thank you Quickdev for permission!

package eu.kanade.tachiyomi.data.connections.discord

import android.graphics.Color
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connections.ConnectionsService

class Discord(id: Long) : ConnectionsService(id) {

    @StringRes
    override fun nameRes() = R.string.connections_discord

    override fun getLogo() = R.drawable.ic_discord_24dp

    @Suppress("MagicNumber")
    override fun getLogoColor() = Color.rgb(88, 101, 242)

    override fun logout() {
        super.logout()
        connectionsPreferences.connectionsToken(this).delete()
    }

    override suspend fun login(username: String, password: String) {
        // Not Needed
    }
}
// <-- AM (DISCORD)
