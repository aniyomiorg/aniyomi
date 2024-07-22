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
import androidx.compose.ui.util.fastAny
import eu.kanade.domain.connections.service.ConnectionsPreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connections.ConnectionsManager
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.ui.player.viewer.PipState
import eu.kanade.tachiyomi.util.system.notificationBuilder
import kotlinx.coroutines.DelicateCoroutinesApi
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.category.anime.interactor.GetAnimeCategories
import tachiyomi.domain.category.model.Category.Companion.UNCATEGORIZED_ID
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import kotlin.math.ceil
import kotlin.math.floor
import kotlinx.serialization.json.Json

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
                if (lastUsedScreen == DiscordScreen.VIDEO) {
                    setAnimeScreen(this@DiscordRPCService, lastUsedScreen)
                } else if (lastUsedScreen == DiscordScreen.MANGA) {
                    setMangaScreen(this@DiscordRPCService, lastUsedScreen)
                }
            }
            notification(this)
        } else {
            connectionsPreferences.enableDiscordRPC().set(false)
        }
    }

    override fun onDestroy() {
        NotificationReceiver.dismissNotification(this, Notifications.ID_DISCORD_RPC)
        rpc?.closeRPC()
        rpc = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun notification(context: Context) {
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

        internal var rpc: DiscordRPC? = null

        private val handler = Handler(Looper.getMainLooper())

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

        private var since = 0L

        internal var lastUsedScreen = DiscordScreen.APP
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

        internal suspend fun setAnimeScreen(
            context: Context,
            discordScreen: DiscordScreen,
            playerData: PlayerData = PlayerData(),
        ) {
            if (PipState.mode == PipState.ON && discordScreen != DiscordScreen.VIDEO) return
            lastUsedScreen = discordScreen

            if (rpc == null) return

            val name = playerData.animeTitle ?: context.resources.getString(R.string.app_name)

            val details = playerData.animeTitle ?: context.resources.getString(
                discordScreen.details,
            )

            val state = playerData.episodeNumber ?: context.resources.getString(discordScreen.text)

            val imageUrl = playerData.thumbnailUrl ?: discordScreen.imageUrl

            rpc!!.updateRPC(
                activity = Activity(
                    name = name,
                    details = details,
                    state = state,
                    type = 3,
                    timestamps = Activity.Timestamps(start = since),
                    assets = Activity.Assets(
                        largeImage = "mp:$imageUrl",
                        smallImage = "mp:${DiscordScreen.APP.imageUrl}",
                        smallText = context.resources.getString(DiscordScreen.APP.text),
                    ),
                ),
                since = since,
            )
        }

        internal suspend fun setMangaScreen(
            context: Context,
            discordScreen: DiscordScreen,
            readerData: ReaderData = ReaderData(),
        ) {
            lastUsedScreen = discordScreen

            if (rpc == null) return

            val name = readerData.mangaTitle ?: context.resources.getString(R.string.app_name)

            val details = readerData.mangaTitle ?: context.resources.getString(
                discordScreen.details,
            )

            val state = readerData.chapterNumber ?: context.resources.getString(discordScreen.text)

            val imageUrl = readerData.thumbnailUrl ?: discordScreen.imageUrl

            rpc!!.updateRPC(
                activity = Activity(
                    name = name,
                    details = details,
                    state = state,
                    type = 3,
                    timestamps = Activity.Timestamps(start = since),
                    assets = Activity.Assets(
                        largeImage = "mp:$imageUrl",
                        smallImage = "mp:${DiscordScreen.APP.imageUrl}",
                        smallText = context.resources.getString(DiscordScreen.APP.text),
                    ),
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

            val animeCategoryIds = Injekt.get<GetAnimeCategories>()
                .await(playerData.animeId)
                .map { it.id.toString() }
                .run { ifEmpty { plus(UNCATEGORIZED_ID.toString()) } }

            val discordIncognitoMode = connectionsPreferences.discordRPCIncognito().get()
            val incognitoCategories = connectionsPreferences.discordRPCIncognitoCategories().get()

            val incognitoCategory = animeCategoryIds.fastAny {
                it in incognitoCategories
            }

            val discordIncognito = discordIncognitoMode || playerData.incognitoMode || incognitoCategory

            val animeTitle = playerData.animeTitle.takeUnless { discordIncognito }

            val episodeNumber = playerData.episodeNumber?.let {
                when {
                    discordIncognito -> null
                    connectionsPreferences.useChapterTitles().get() -> it
                    ceil(it.toDouble()) == floor(it.toDouble()) -> "Episode ${it.toInt()}"
                    else -> "Episode $it"
                }
            }

            withIOContext {
                val connectionsManager: ConnectionsManager by injectLazy()
                val networkService: NetworkHelper by injectLazy()
                val client = networkService.client
                val json = Json {
                    encodeDefaults = true
                    allowStructuredMapKeys = true
                    ignoreUnknownKeys = true
                }
                val rpcExternalAsset = RPCExternalAsset(applicationId = RICH_PRESENCE_APPLICATION_ID, token = connectionsPreferences.connectionsToken(connectionsManager.discord).get(), client = client, json = json)

                val discordUri = if (!discordIncognito) {
                    try {
                      rpcExternalAsset.getDiscordUri(playerData.thumbnailUrl)
                    } catch (e: Throwable) {
                        null
                    }
                } else {
                    null
                }
                val animeThumbnail = discordUri?.takeIf { !it.contains("external/Not Found") }
                    ?.substringAfter("\"id\": \"")?.substringBefore("\"}")
                    ?.split("external/")?.getOrNull(1)?.let { "external/$it" }

                setAnimeScreen(
                    context = context,
                    discordScreen = DiscordScreen.VIDEO,
                    playerData = PlayerData(
                        animeTitle = animeTitle,
                        episodeNumber = episodeNumber,
                        thumbnailUrl = animeThumbnail,
                    ),
                )
            }
        }


        @Suppress("SwallowedException", "TooGenericExceptionCaught", "CyclomaticComplexMethod")
        internal suspend fun setReaderActivity(
            context: Context,
            readerData: ReaderData = ReaderData(),
        ) {
            if (rpc == null || readerData.thumbnailUrl == null || readerData.mangaId == null) return

            val animeCategoryIds = Injekt.get<GetAnimeCategories>()
                .await(readerData.mangaId)
                .map { it.id.toString() }
                .run { ifEmpty { plus(UNCATEGORIZED_ID.toString()) } }

            val discordIncognitoMode = connectionsPreferences.discordRPCIncognito().get()
            val incognitoCategories = connectionsPreferences.discordRPCIncognitoCategories().get()

            val incognitoCategory = animeCategoryIds.fastAny {
                it in incognitoCategories
            }

            val discordIncognito = discordIncognitoMode || readerData.incognitoMode || incognitoCategory

            val mangaTitle = readerData.mangaTitle.takeUnless { discordIncognito }

            val chapterNumber = readerData.chapterNumber?.let {
                when {
                    discordIncognito -> null
                    connectionsPreferences.useChapterTitles().get() ->
                        "$it (${readerData.chapterProgress.first}/${readerData.chapterProgress.second})"
                    ceil(it.toDouble()) == floor(it.toDouble()) -> "Chapter ${it.toInt()}" + " " +
                        "(${readerData.chapterProgress.first}/${readerData.chapterProgress.second})"
                    else -> "Chapter $it (${readerData.chapterProgress.first}/${readerData.chapterProgress.second}"
                }
            }

            withIOContext {
                val connectionsManager: ConnectionsManager by injectLazy()
                val networkService: NetworkHelper by injectLazy()
                val client = networkService.client
                val json = Json { ignoreUnknownKeys = true }  // Configura el JSON parser si es necesario
                val rpcExternalAsset = RPCExternalAsset(applicationId = RICH_PRESENCE_APPLICATION_ID , token = connectionsPreferences.connectionsToken(connectionsManager.discord).get(), client = client, json = json)

                val discordUri = if (!discordIncognito) {
                    try {
                        rpcExternalAsset.getDiscordUri(readerData.thumbnailUrl)
                    } catch (e: Throwable) {
                        null
                    }
                } else {
                    null
                }
                val mangaThumbnail = discordUri?.takeIf { !it.contains("external/Not Found") }
                    ?.substringAfter("\"id\": \"")?.substringBefore("\"}")
                    ?.split("external/")?.getOrNull(1)?.let { "external/$it" }

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
        }

        private const val RICH_PRESENCE_APPLICATION_ID = "1173423931865170070"
    }
}
// <-- AM (DISCORD)
