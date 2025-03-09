// AM (CONNECTIONS) -->
package eu.kanade.tachiyomi.data.connections

import eu.kanade.tachiyomi.data.connections.discord.Discord

class ConnectionsManager {

    companion object {
        const val DISCORD = 201L
    }

    val discord = Discord(DISCORD)

    private val services = listOf(discord)

    fun getService(id: Long) = services.find { it.id == id }
}
// <-- AM (CONNECTIONS)
