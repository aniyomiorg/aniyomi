package eu.kanade.tachiyomi.data.library.anime

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import eu.kanade.presentation.util.formatEpisodeNumber
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.core.common.Constants
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloader
import eu.kanade.tachiyomi.data.notification.NotificationHandler
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.source.UnmeteredSource
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.lang.chop
import eu.kanade.tachiyomi.util.system.cancelNotification
import eu.kanade.tachiyomi.util.system.getBitmapOrNull
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notify
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchUI
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.library.anime.LibraryAnime
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.math.RoundingMode
import java.text.NumberFormat

class AnimeLibraryUpdateNotifier(
    private val context: Context,

    private val securityPreferences: SecurityPreferences = Injekt.get(),
    private val sourceManager: AnimeSourceManager = Injekt.get(),
) {

    private val percentFormatter = NumberFormat.getPercentInstance().apply {
        roundingMode = RoundingMode.DOWN
        maximumFractionDigits = 0
    }

    /**
     * Pending intent of action that cancels the library update
     */
    private val cancelIntent by lazy {
        NotificationReceiver.cancelAnimelibUpdatePendingBroadcast(context)
    }

    /**
     * Bitmap of the app for notifications.
     */
    private val notificationBitmap by lazy {
        BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
    }

    /**
     * Cached progress notification to avoid creating a lot.
     */
    val progressNotificationBuilder by lazy {
        context.notificationBuilder(Notifications.CHANNEL_LIBRARY_PROGRESS) {
            setContentTitle(context.stringResource(MR.strings.app_name))
            setSmallIcon(R.drawable.ic_refresh_24dp)
            setLargeIcon(notificationBitmap)
            setOngoing(true)
            setOnlyAlertOnce(true)
            addAction(
                R.drawable.ic_close_24dp,
                context.stringResource(MR.strings.action_cancel),
                cancelIntent,
            )
        }
    }

    /**
     * Shows the notification containing the currently updating anime and the progress.
     *
     * @param anime the anime that are being updated.
     * @param current the current progress.
     * @param total the total progress.
     */
    fun showProgressNotification(anime: List<Anime>, current: Int, total: Int) {
        progressNotificationBuilder
            .setContentTitle(
                context.stringResource(
                    MR.strings.notification_updating_progress,
                    percentFormatter.format(current.toFloat() / total),
                ),
            )

        if (!securityPreferences.hideNotificationContent().get()) {
            val updatingText = anime.joinToString("\n") { it.title.chop(40) }
            progressNotificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(updatingText))
        }

        context.notify(
            Notifications.ID_LIBRARY_PROGRESS,
            progressNotificationBuilder
                .setProgress(total, current, false)
                .build(),
        )
    }

    fun showQueueSizeWarningNotification() {
        context.notify(
            Notifications.ID_LIBRARY_SIZE_WARNING,
            Notifications.CHANNEL_LIBRARY_PROGRESS,
        ) {
            setContentTitle(context.stringResource(MR.strings.label_warning))
            setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    context.stringResource(MR.strings.notification_size_warning),
                ),
            )
            setSmallIcon(R.drawable.ic_warning_white_24dp)
            setTimeoutAfter(AnimeDownloader.WARNING_NOTIF_TIMEOUT_MS)
            setContentIntent(NotificationHandler.openUrl(context, HELP_WARNING_URL))
        }
    }

    /**
     * Warn when excessively checking any single source.
     */
    fun showQueueSizeWarningNotificationIfNeeded(animeToUpdate: List<LibraryAnime>) {
        val maxUpdatesFromSource = animeToUpdate
            .groupBy { it.anime.source }
            .filterKeys { sourceManager.get(it) !is UnmeteredSource }
            .maxOfOrNull { it.value.size } ?: 0

        if (maxUpdatesFromSource <= ANIME_PER_SOURCE_QUEUE_WARNING_THRESHOLD) {
            return
        }

        context.notify(
            Notifications.ID_LIBRARY_SIZE_WARNING,
            Notifications.CHANNEL_LIBRARY_PROGRESS,
        ) {
            setContentTitle(context.stringResource(MR.strings.label_warning))
            setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.stringResource(MR.strings.notification_size_warning)),
            )
            setSmallIcon(R.drawable.ic_warning_white_24dp)
            setTimeoutAfter(AnimeDownloader.WARNING_NOTIF_TIMEOUT_MS)
            setContentIntent(NotificationHandler.openUrl(context, HELP_WARNING_URL))
        }
    }

    /**
     * Shows notification containing update entries that failed with action to open full log.
     *
     * @param failed Number of entries that failed to update.
     * @param uri Uri for error log file containing all titles that failed.
     */
    fun showUpdateErrorNotification(failed: Int, uri: Uri) {
        if (failed == 0) {
            return
        }

        context.notify(
            Notifications.ID_LIBRARY_ERROR,
            Notifications.CHANNEL_LIBRARY_ERROR,
        ) {
            setContentTitle(context.stringResource(MR.strings.notification_update_error, failed))
            setContentText(context.stringResource(MR.strings.action_show_errors))
            setSmallIcon(R.drawable.ic_ani)

            setContentIntent(NotificationReceiver.openErrorLogPendingActivity(context, uri))
        }
    }

    /**
     * Shows the notification containing the result of the update done by the service.
     *
     * @param updates a list of anime with new updates.
     */
    fun showUpdateNotifications(updates: List<Pair<Anime, Array<Episode>>>) {
        // Parent group notification
        context.notify(
            Notifications.ID_NEW_EPISODES,
            Notifications.CHANNEL_NEW_CHAPTERS_EPISODES,
        ) {
            setContentTitle(context.stringResource(AYMR.strings.notification_new_episodes))
            if (updates.size == 1 && !securityPreferences.hideNotificationContent().get()) {
                setContentText(updates.first().first.title.chop(NOTIF_TITLE_MAX_LEN))
            } else {
                setContentText(
                    context.resources.getQuantityString(
                        R.plurals.notification_new_episodes_summary,
                        updates.size,
                        updates.size,
                    ),
                )

                if (!securityPreferences.hideNotificationContent().get()) {
                    setStyle(
                        NotificationCompat.BigTextStyle().bigText(
                            updates.joinToString("\n") {
                                it.first.title.chop(NOTIF_TITLE_MAX_LEN)
                            },
                        ),
                    )
                }
            }

            setSmallIcon(R.drawable.ic_ani)
            setLargeIcon(notificationBitmap)

            setGroup(Notifications.GROUP_NEW_EPISODES)
            setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            setGroupSummary(true)
            priority = NotificationCompat.PRIORITY_HIGH

            setContentIntent(getNotificationIntent())
            setAutoCancel(true)
        }

        // Per-anime notification
        if (!securityPreferences.hideNotificationContent().get()) {
            launchUI {
                context.notify(
                    updates.map { (anime, episodes) ->
                        NotificationManagerCompat.NotificationWithIdAndTag(
                            anime.id.hashCode(),
                            createNewEpisodesNotification(anime, episodes),
                        )
                    },
                )
            }
        }
    }

    private suspend fun createNewEpisodesNotification(anime: Anime, episodes: Array<Episode>): Notification {
        val icon = getAnimeIcon(anime)
        return context.notificationBuilder(Notifications.CHANNEL_NEW_CHAPTERS_EPISODES) {
            setContentTitle(anime.title)

            val description = getNewEpisodesDescription(episodes)
            setContentText(description)
            setStyle(NotificationCompat.BigTextStyle().bigText(description))

            setSmallIcon(R.drawable.ic_ani)

            if (icon != null) {
                setLargeIcon(icon)
            }

            setGroup(Notifications.GROUP_NEW_EPISODES)
            setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            priority = NotificationCompat.PRIORITY_HIGH

            // Open first episode on tap
            setContentIntent(
                NotificationReceiver.openEpisodePendingActivity(context, anime, episodes.first()),
            )
            setAutoCancel(true)

            // Mark episodes as read action
            addAction(
                R.drawable.ic_done_24dp,
                context.stringResource(AYMR.strings.action_mark_as_seen),
                NotificationReceiver.markAsViewedPendingBroadcast(
                    context,
                    anime,
                    episodes,
                    Notifications.ID_NEW_EPISODES,
                ),
            )
            // View episodes action
            addAction(
                R.drawable.ic_book_24dp,
                context.stringResource(AYMR.strings.action_view_episodes),
                NotificationReceiver.openEpisodePendingActivity(
                    context,
                    anime,
                    Notifications.ID_NEW_EPISODES,
                ),
            )
            // Download chapters action
            // Only add the action when chapters is within threshold
            if (episodes.size <= AnimeDownloader.EPISODES_PER_SOURCE_QUEUE_WARNING_THRESHOLD) {
                addAction(
                    android.R.drawable.stat_sys_download_done,
                    context.stringResource(MR.strings.action_download),
                    NotificationReceiver.downloadEpisodesPendingBroadcast(
                        context,
                        anime,
                        episodes,
                        Notifications.ID_NEW_CHAPTERS,
                    ),
                )
            }
        }.build()
    }

    /**
     * Cancels the progress notification.
     */
    fun cancelProgressNotification() {
        context.cancelNotification(Notifications.ID_LIBRARY_PROGRESS)
    }

    private suspend fun getAnimeIcon(anime: Anime): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(anime)
            .transformations(CircleCropTransformation())
            .size(NOTIF_ICON_SIZE)
            .build()
        val drawable = context.imageLoader.execute(request).image?.asDrawable(context.resources)
        return drawable?.getBitmapOrNull()
    }

    private fun getNewEpisodesDescription(episodes: Array<Episode>): String {
        val displayableEpisodeNumbers = episodes
            .filter { it.isRecognizedNumber }
            .sortedBy { it.episodeNumber }
            .map { formatEpisodeNumber(it.episodeNumber) }
            .toSet()

        return when (displayableEpisodeNumbers.size) {
            // No sensible episode numbers to show (i.e. no episodes have parsed episode number)
            0 -> {
                // "1 new episode" or "5 new episodes"
                context.resources.getQuantityString(
                    R.plurals.notification_episodes_generic,
                    episodes.size,
                    episodes.size,
                )
            }
            // Only 1 episode has a parsed episode number
            1 -> {
                val remaining = episodes.size - displayableEpisodeNumbers.size
                if (remaining == 0) {
                    // "Episode 2.5"
                    context.stringResource(
                        AYMR.strings.notification_episodes_single,
                        displayableEpisodeNumbers.first(),
                    )
                } else {
                    // "Episode 2.5 and 10 more"
                    context.stringResource(
                        AYMR.strings.notification_episodes_single_and_more,
                        displayableEpisodeNumbers.first(),
                        remaining,
                    )
                }
            }
            // Everything else (i.e. multiple parsed episode numbers)
            else -> {
                val shouldTruncate = displayableEpisodeNumbers.size > NOTIF_MAX_EPISODES
                if (shouldTruncate) {
                    // "Episodes 1, 2.5, 3, 4, 5 and 10 more"
                    val remaining = displayableEpisodeNumbers.size - NOTIF_MAX_EPISODES
                    val joinedEpisodeNumbers = displayableEpisodeNumbers.take(NOTIF_MAX_EPISODES).joinToString(
                        ", ",
                    )
                    context.resources.getQuantityString(
                        R.plurals.notification_episodes_multiple_and_more,
                        remaining,
                        joinedEpisodeNumbers,
                        remaining,
                    )
                } else {
                    // "Episodes 1, 2.5, 3"
                    context.stringResource(
                        AYMR.strings.notification_episodes_multiple,
                        displayableEpisodeNumbers.joinToString(", "),
                    )
                }
            }
        }
    }

    /**
     * Returns an intent to open the main activity.
     */
    private fun getNotificationIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = Constants.SHORTCUT_UPDATES
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val HELP_WARNING_URL =
            "https://aniyomi.org/docs/faq/library#why-am-i-warned-about-large-bulk-updates-and-downloads"
    }
}

private const val NOTIF_MAX_EPISODES = 5
private const val NOTIF_TITLE_MAX_LEN = 45
private const val NOTIF_ICON_SIZE = 192
private const val ANIME_PER_SOURCE_QUEUE_WARNING_THRESHOLD = 60
