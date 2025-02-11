// AM (DISCORD) -->

// Taken from Animiru. Thank you Quickdev for permission!

package eu.kanade.tachiyomi.data.connections.discord

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.compose.ui.util.fastAny
import eu.kanade.domain.connections.service.ConnectionsPreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connections.ConnectionsManager
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import eu.kanade.tachiyomi.util.system.notificationBuilder
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.serialization.json.Json
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.category.anime.interactor.GetAnimeCategories
import tachiyomi.domain.category.model.Category.Companion.UNCATEGORIZED_ID
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import kotlin.math.ceil
import kotlin.math.floor

class DiscordRPCService : Service() {

    private val connectionsManager: ConnectionsManager by injectLazy()

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        val token = connectionsPreferences.connectionsToken(connectionsManager.discord).get()
        val status = when (connectionsPreferences.discordRPCStatus().get()) {
            -1 -> "dnd"
            0 -> "idle"
            else -> "online"
        }
        rpc = if (token.isNotBlank()) DiscordRPC(token, status) else null
        if (rpc != null) {
            launchIO {
                try { // Add a try-catch block here
                    if (lastUsedScreen == DiscordScreen.VIDEO) {
                        setAnimeScreen(this@DiscordRPCService, lastUsedScreen)
                    } else if (lastUsedScreen == DiscordScreen.MANGA) {
                        setMangaScreen(this@DiscordRPCService, lastUsedScreen)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting screen: ${e.message}", e)
                }
            }
            notification(this)
        } else {
            connectionsPreferences.enableDiscordRPC().set(false)
        }
    }
    override fun onDestroy() {
        NotificationReceiver.dismissNotification(this, Notifications.ID_DISCORD_RPC)
        rpc?.closeRPC() // Check for null before closing
        rpc = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun notification(context: Context) {
        val appName = context.getString(R.string.app_name) // Get app name once
        val builder = context.notificationBuilder(Notifications.CHANNEL_DISCORD_RPC) {
            setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            setSmallIcon(R.drawable.ic_discord_24dp)
            setContentText(context.resources.getString(R.string.pref_discord_rpc))
            setAutoCancel(false)
            setOngoing(true)
            setUsesChronometer(true)
        }

        startForeground(Notifications.ID_DISCORD_RPC, builder.build())
    }

    companion object {

        private val connectionsPreferences: ConnectionsPreferences by injectLazy()

        private var rpc: DiscordRPC? = null // Consider making private

        private val handler = Handler(Looper.getMainLooper())
        private val playerPreferences: PlayerPreferences by injectLazy()

        fun start(context: Context) {
            handler.removeCallbacksAndMessages(null)
            if (rpc == null && connectionsPreferences.enableDiscordRPC().get()) {
                since = System.currentTimeMillis()
                context.startService(Intent(context, DiscordRPCService::class.java))
            }
        }

        fun stop(context: Context, delay: Long = 30000L) {
            handler.postDelayed(
                { context.stopService(Intent(context, DiscordRPCService::class.java)) },
                delay,
            )
        }

        private var since = 0L // Consider making private

        internal var lastUsedScreen = DiscordScreen.APP // Consider making private
            set(value) {
                field = if ((
                        value == DiscordScreen.VIDEO ||
                            value == DiscordScreen.MANGA
                        ) ||
                    value == DiscordScreen.WEBVIEW
                ) {
                    field
                } else {
                    value
                }
            }
        private const val RICH_PRESENCE_APPLICATION_ID = "1173423931865170070"
        private const val MP_PREFIX = "mp:"
        private const val EXTERNAL_PREFIX = "external/"
        private val json = Json {
            encodeDefaults = true
            allowStructuredMapKeys = true
            ignoreUnknownKeys = true
        }
        private const val TAG = "DiscordRPCService"

        internal suspend fun setAnimeScreen(
            context: Context,
            discordScreen: DiscordScreen,
            playerData: PlayerData = PlayerData(),
        ) {
            if (discordScreen != DiscordScreen.VIDEO) return
            lastUsedScreen = discordScreen // Update last used screen

            if (rpc == null) return
            updateDiscordRPC(context, playerData, discordScreen)
        }
        private suspend fun updateDiscordRPC(
            context: Context,
            playerData: PlayerData,
            discordScreen: DiscordScreen,
            sinceTime: Long = since,
        ) {
            val appName = context.getString(R.string.app_name)

            val customMessage = connectionsPreferences.discordCustomMessage().get()
            val showProgress = connectionsPreferences.discordShowProgress().get()
            val showTimestamp = connectionsPreferences.discordShowTimestamp().get()
            val showButtons = connectionsPreferences.discordShowButtons().get()
            val showDownloadButton = connectionsPreferences.discordShowDownloadButton().get()
            val showDiscordButton = connectionsPreferences.discordShowDiscordButton().get()

            val name = playerData.animeTitle ?: appName
            val details = when {
                customMessage.isNotBlank() -> customMessage
                playerData.animeTitle != null -> playerData.animeTitle
                else -> context.getString(discordScreen.details)
            }

            val state = when {
                !showProgress -> null
                playerData.episodeNumber != null -> playerData.episodeNumber
                else -> context.getString(discordScreen.text)
            }

            val imageUrl = playerData.thumbnailUrl ?: discordScreen.imageUrl

            val timestamps = if (showTimestamp) {
                Activity.Timestamps(
                    start = playerData.startTimestamp ?: since,
                    end = playerData.endTimestamp,
                )
            } else {
                null
            }

            val buttons = if (showButtons) {
                buildList {
                    if (showDownloadButton) add(DOWNLOAD_BUTTON_LABEL)
                    if (showDiscordButton) add(DISCORD_BUTTON_LABEL)
                }.takeIf { it.isNotEmpty() }
            } else {
                null
            }

            val metadata = buttons?.let {
                Activity.Metadata(
                    buttonUrls = buildList {
                        if (showDownloadButton) add(DOWNLOAD_BUTTON_URL)
                        if (showDiscordButton) add(DISCORD_BUTTON_URL)
                    },
                )
            }

            rpc!!.updateRPC(
                activity = Activity(
                    name = name,
                    details = details,
                    state = state,
                    type = 3,
                    timestamps = timestamps,
                    assets = Activity.Assets(
                        largeImage = "$MP_PREFIX$imageUrl",
                        smallImage = "$MP_PREFIX${DiscordScreen.APP.imageUrl}",
                        smallText = context.getString(DiscordScreen.APP.text),
                    ),
                    buttons = buttons,
                    metadata = metadata,
                ),
                since = sinceTime,
            )
        }

        internal suspend fun setMangaScreen(
            context: Context,
            discordScreen: DiscordScreen,
            readerData: ReaderData = ReaderData(),
        ) {
            lastUsedScreen = discordScreen // Update last used screen
            if (rpc == null) return
            updateDiscordRPC(context, readerData, discordScreen)
        }

        private suspend fun updateDiscordRPC(
            context: Context,
            readerData: ReaderData,
            discordScreen: DiscordScreen,
            sinceTime: Long = since,
        ) {
            val appName = context.getString(R.string.app_name)
            val name = readerData.mangaTitle ?: appName
            val details = readerData.mangaTitle ?: context.getString(discordScreen.details)
            val state = readerData.chapterNumber ?: context.getString(discordScreen.text)
            val imageUrl = readerData.thumbnailUrl ?: discordScreen.imageUrl

            val showButtons = connectionsPreferences.discordShowButtons().get()
            val showDownloadButton = connectionsPreferences.discordShowDownloadButton().get()
            val showDiscordButton = connectionsPreferences.discordShowDiscordButton().get()

            val buttons = if (showButtons) {
                buildList {
                    if (showDownloadButton) add(DOWNLOAD_BUTTON_LABEL)
                    if (showDiscordButton) add(DISCORD_BUTTON_LABEL)
                }.takeIf { it.isNotEmpty() }
            } else {
                null
            }

            val metadata = buttons?.let {
                Activity.Metadata(
                    buttonUrls = buildList {
                        if (showDownloadButton) add(DOWNLOAD_BUTTON_URL)
                        if (showDiscordButton) add(DISCORD_BUTTON_URL)
                    },
                )
            }

            rpc!!.updateRPC(
                activity = Activity(
                    name = name,
                    details = details,
                    state = state,
                    type = 3,
                    timestamps = Activity.Timestamps(start = sinceTime),
                    assets = Activity.Assets(
                        largeImage = "$MP_PREFIX$imageUrl",
                        smallImage = "$MP_PREFIX${DiscordScreen.APP.imageUrl}",
                        smallText = context.getString(DiscordScreen.APP.text),
                    ),
                    buttons = buttons,
                    metadata = metadata,
                ),
                since = since,
            )
        }

        @Suppress("SwallowedException", "TooGenericExceptionCaught", "CyclomaticComplexMethod")
        internal suspend fun setPlayerActivity(
            context: Context,
            playerData: PlayerData = PlayerData(),
        ) {
            if (rpc == null || playerData.thumbnailUrl == null || playerData.animeId == null) return

            try { // Wrap in try-catch
                val categories = getCategories(playerData.animeId)
                val discordIncognito = isIncognito(categories, playerData.incognitoMode)

                val animeTitle = playerData.animeTitle.takeUnless { discordIncognito }
                val episodeNumber = getFormattedEpisodeNumber(playerData, discordIncognito)
                val (startTime, end) = getTimestamps(playerData)

                withIOContext {
                    val rpcExternalAsset = getRPCExternalAsset() // Get RPCExternalAsset
                    val animeThumbnail =
                        getDiscordThumbnail(rpcExternalAsset, playerData.thumbnailUrl, discordIncognito)

                    setAnimeScreen(
                        context = context,
                        discordScreen = DiscordScreen.VIDEO,
                        playerData = PlayerData(
                            animeTitle = animeTitle,
                            episodeNumber = episodeNumber,
                            thumbnailUrl = animeThumbnail,
                            startTimestamp = startTime,
                            endTimestamp = end,
                        ),
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting player activity: ${e.message}", e)
            }
        }

        @Suppress("SwallowedException", "TooGenericExceptionCaught", "CyclomaticComplexMethod")
        internal suspend fun setReaderActivity(
            context: Context,
            readerData: ReaderData = ReaderData(),
        ) {
            if (rpc == null || readerData.thumbnailUrl == null || readerData.mangaId == null) return
            try {
                val categories = getCategories(readerData.mangaId)
                val discordIncognito = isIncognito(categories, readerData.incognitoMode)

                val mangaTitle = readerData.mangaTitle.takeUnless { discordIncognito }
                val chapterNumber = getFormattedChapterNumber(readerData, discordIncognito)

                withIOContext {
                    val rpcExternalAsset = getRPCExternalAsset() // Get rpcExternalAsset
                    val mangaThumbnail =
                        getDiscordThumbnail(rpcExternalAsset, readerData.thumbnailUrl, discordIncognito)

                    setMangaScreen(
                        context = context,
                        discordScreen = DiscordScreen.MANGA,
                        readerData = ReaderData(
                            mangaTitle = mangaTitle,
                            chapterNumber = chapterNumber,
                            thumbnailUrl = mangaThumbnail,
                        ),
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting reader activity: ${e.message}", e)
            }
        }

        // Helper functions

        private suspend fun getCategories(id: Long?): List<String> {
            return Injekt.get<GetAnimeCategories>()
                .await(id!!)
                .map { it.id.toString() }
                .run { ifEmpty { plus(UNCATEGORIZED_ID.toString()) } }
        }

        private fun isIncognito(categories: List<String>, incognitoMode: Boolean): Boolean {
            val discordIncognitoMode = connectionsPreferences.discordRPCIncognito().get()
            val incognitoCategories = connectionsPreferences.discordRPCIncognitoCategories().get()
            val incognitoCategory = categories.fastAny { it in incognitoCategories }
            return discordIncognitoMode || incognitoMode || incognitoCategory
        }

        private fun getFormattedEpisodeNumber(playerData: PlayerData, discordIncognito: Boolean): String? {
            return playerData.episodeNumber?.let {
                when {
                    discordIncognito -> null
                    connectionsPreferences.useChapterTitles().get() -> it
                    ceil(it.toDouble()) == floor(it.toDouble()) -> "Episode ${it.toInt()}"
                    else -> "Episode $it"
                }
            }
        }
        private fun getFormattedChapterNumber(readerData: ReaderData, discordIncognito: Boolean): String? {
            val chapterNumber = readerData.chapterNumber
            val chapterProgress = readerData.chapterProgress
            return chapterNumber?.let {
                when {
                    discordIncognito -> null
                    connectionsPreferences.useChapterTitles().get() ->
                        "$it (${chapterProgress.first}/${chapterProgress.second})"

                    ceil(it.toDouble()) == floor(it.toDouble()) -> "Chapter ${it.toInt()}" + " " +
                        "(${chapterProgress.first}/${chapterProgress.second})"

                    else -> "Chapter $it (${chapterProgress.first}/${chapterProgress.second}"
                }
            }
        }

        private fun getTimestamps(playerData: PlayerData): Pair<Long?, Long?> {
            val startTime = playerData.startTimestamp ?: System.currentTimeMillis()
            val end = playerData.endTimestamp
            return Pair(startTime, end)
        }

        private suspend fun getRPCExternalAsset(): RPCExternalAsset {
            val connectionsManager: ConnectionsManager by injectLazy()
            val networkService: NetworkHelper by injectLazy()
            val client = networkService.client
            return RPCExternalAsset(
                applicationId = RICH_PRESENCE_APPLICATION_ID,
                token = connectionsPreferences.connectionsToken(connectionsManager.discord).get(),
                client = client,
                json = json,
            )
        }
        private suspend fun getDiscordThumbnail(
            rpcExternalAsset: RPCExternalAsset,
            thumbnailUrl: String?,
            incognito: Boolean,
        ): String? {
            if (incognito || thumbnailUrl == null) return null

            return try {
                rpcExternalAsset.getDiscordUri(thumbnailUrl)
                    ?.takeIf { !it.contains("external/Not Found") }
                    ?.substringAfter("\"id\": \"")
                    ?.substringBefore("\"}")
                    ?.split(EXTERNAL_PREFIX)
                    ?.getOrNull(1)
                    ?.let { "$EXTERNAL_PREFIX$it" }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting Discord URI: ${e.message}", e)
                null
            }
        }
    }
}
// <-- AM (DISCORD)
