// AM (DISCORD) -->
// Original library from https://github.com/dead8309/KizzyRPC (Thank you)
// Thank you to the 最高 man for the refactored and simplified code
// https://github.com/saikou-app/saikou
package eu.kanade.tachiyomi.data.connections.discord

/**
 * DiscordRPC is a class that implements Discord Rich Presence functionality using WebSockets.
 * @param token discord account token
 */
class DiscordRPC(val token: String, val status: String) {
    private val discordWebSocket: DiscordWebSocket = DiscordWebSocketImpl(token)
    private var rpc: Presence? = null

    /**
     * Closes the Rich Presence connection.
     */
    fun closeRPC() {
        discordWebSocket.close()
    }

    /**
     * Sets the activity for the Rich Presence.
     * @param activity the activity to set.
     * @param since the activity start time.
     */
    suspend fun updateRPC(
        activity: Activity,
        since: Long? = null,
    ) {
        rpc = Presence(
            activities = listOf(activity),
            afk = true,
            since = since,
            status = status,
        )

        rpc?.let { discordWebSocket.sendActivity(it) }
    }
}
// <-- AM (DISCORD)
