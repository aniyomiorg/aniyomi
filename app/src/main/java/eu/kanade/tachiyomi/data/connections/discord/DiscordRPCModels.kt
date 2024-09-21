// AM (DISCORD) -->

// Taken from Animiru. Thank you Quickdev for permission!
// Original library from https://github.com/dead8309/KizzyRPC (Thank you)
// Thank you to the 最高 man for the refactored and simplified code
// https://github.com/saikou-app/saikou
package eu.kanade.tachiyomi.data.connections.discord

import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// Constant for logging tag
@Suppress("TopLevelPropertyNaming")
const val RICH_PRESENCE_TAG = "discord_rpc"

// Constant for application id
@Suppress("TopLevelPropertyNaming")
private const val RICH_PRESENCE_APPLICATION_ID = "1173423931865170070"

// Constant for buttons list
@Suppress("TopLevelPropertyNaming")
private val RICH_PRESENCE_BUTTONS = listOf("Discord Server")

// Constant for metadata list
@Suppress("TopLevelPropertyNaming")
private val RICH_PRESENCE_METADATA = Activity.Metadata(
    listOf(
        "https://discord.gg/vN8nbPHzeC"
    ),
)

@Serializable
data class Activity(
    @SerialName("application_id")
    val applicationId: String? = RICH_PRESENCE_APPLICATION_ID,
    val name: String? = null,
    val details: String? = null,
    val state: String? = null,
    val type: Int? = null,
    val timestamps: Timestamps? = null,
    val assets: Assets? = null,
    val buttons: List<String>? = RICH_PRESENCE_BUTTONS,
    val metadata: Metadata? = RICH_PRESENCE_METADATA,

) {
    @Serializable
    data class Assets(
        @SerialName("large_image")
        val largeImage: String? = null,
        @SerialName("large_text")
        val largeText: String? = null,
        @SerialName("small_image")
        val smallImage: String? = null,
        @SerialName("small_text")
        val smallText: String? = null,
    )

    @Serializable
    data class Metadata(
        @SerialName("button_urls")
        val buttonUrls: List<String>,
    )

    @Serializable
    data class Timestamps(
        val start: Long? = null,
        val stop: Long? = null,
    )
}

@Serializable
data class Presence(
    val activities: List<Activity> = listOf(),
    val afk: Boolean = true,
    val since: Long? = null,
    val status: String? = null,
) {
    @Serializable
    data class Response(
        val op: Long,
        val d: Presence,
    )
}

@Serializable
data class Identity(
    val token: String,
    val properties: Properties,
    val compress: Boolean,
    val intents: Long,
) {

    @Serializable
    data class Response(
        val op: Long,
        val d: Identity,
    )

    @Serializable
    data class Properties(
        @SerialName("\$os")
        val os: String,

        @SerialName("\$browser")
        val browser: String,

        @SerialName("\$device")
        val device: String,
    )
}

@Serializable
data class Res(
    val t: String?,
    val s: Int?,
    val op: Int,
    val d: JsonElement,
)

@Suppress("MagicNumber")
enum class OpCode(val value: Int) {
    /** An event was dispatched. */
    DISPATCH(0),

    /** Fired periodically by the client to keep the connection alive. */
    HEARTBEAT(1),

    /** Starts a new session during the initial handshake. */
    IDENTIFY(2),

    /** Update the client's presence. */
    PRESENCE_UPDATE(3),

    /** Joins/leaves or moves between voice channels. */
    VOICE_STATE(4),

    /** Resume a previous session that was disconnected. */
    RESUME(6),

    /** You should attempt to reconnect and resume immediately. */
    RECONNECT(7),

    /** Request information about offline guild members in a large guild. */
    REQUEST_GUILD_MEMBERS(8),

    /** The session has been invalidated. You should reconnect and identify/resume accordingly */
    INVALID_SESSION(9),

    /** Sent immediately after connecting, contains the heartbeat_interval to use. */
    HELLO(10),

    /** Sent in response to receiving a heartbeat to acknowledge that it has been received. */
    HEARTBEAT_ACK(11),

    /** For future use or unknown opcodes. */
    UNKNOWN(-1),
}

data class PlayerData(
    val incognitoMode: Boolean = false,
    val animeId: Long? = null,
    val animeTitle: String? = null,
    val episodeNumber: String? = null,
    val thumbnailUrl: String? = null,
)

data class ReaderData(
    val incognitoMode: Boolean = false,
    val mangaId: Long? = null,
    val mangaTitle: String? = null,
    val chapterProgress: Pair<Int, Int> = Pair(0, 0),
    val chapterNumber: String? = null,
    val thumbnailUrl: String? = null,
)

// Enum class for standard Rich Presence in-app screens
enum class DiscordScreen(
    @StringRes val text: Int,
    @StringRes val details: Int,
    val imageUrl: String,
) {
    APP(R.string.app_name, R.string.browsing, AnimetailImage),
    LIBRARY(R.string.label_library, R.string.browsing, LibraryImageUrl),
    UPDATES(R.string.label_recent_updates, R.string.scrolling, UpdatesImageUrl),
    HISTORY(R.string.label_recent_manga, R.string.scrolling, HistoryImageUrl),
    BROWSE(R.string.label_sources, R.string.browsing, BrowseImageUrl),
    MORE(R.string.label_settings, R.string.messing, MoreImageUrl),
    WEBVIEW(R.string.action_web_view, R.string.browsing, WebviewImageUrl),
    VIDEO(R.string.video, R.string.watching, VideoImageUrl),
    MANGA(R.string.manga, R.string.reading, MangaImageUrl),
}

// Constants for standard Rich Presence image urls
// change the image Urls used here to match animetail brown/ green theme, Luft
private const val AnimetailImageUrl = "emojis/1286834441981005824.webp?quality=lossless"
private const val AnimetailPreviewImageUrl = "emojis/1286834519533420544.webp?quality=lossless"
private val AnimetailImage = if (BuildConfig.PREVIEW == true) AnimetailPreviewImageUrl else AnimetailImageUrl
private const val LibraryImageUrl = "emojis/1235353629867638924.webp?quality=lossless"
private const val UpdatesImageUrl = "emojis/1235354596570955917.webp?quality=lossless"
private const val HistoryImageUrl = "emojis/1235354299089817671.webp?quality=lossless"
private const val BrowseImageUrl = "emojis/1235354864419344455.webp?quality=lossless"
private const val MoreImageUrl = "emojis/1235355169752088706.webp?quality=lossless"
private const val WebviewImageUrl = "emojis/1235355362169851996.webp?quality=lossless"
private const val VideoImageUrl = "emojis/1235355607201218660.webp?quality=lossless"
private const val MangaImageUrl = "emojis/1235355804274659390.webp?quality=lossless"

// <-- AM (DISCORD)
