package eu.kanade.tachiyomi.data.animelib

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import coil.imageLoader
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.episode.model.Episode
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.animedownload.AnimeDownloader
import eu.kanade.tachiyomi.data.notification.NotificationHandler
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.lang.chop
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.system.notification
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager
import uy.kohesive.injekt.injectLazy
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

class AnimelibUpdateNotifier(private val context: Context) {

    private val preferences: SecurityPreferences by injectLazy()

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
            setContentTitle(context.getString(R.string.app_name))
            setSmallIcon(R.drawable.ic_refresh_24dp)
            setLargeIcon(notificationBitmap)
            setOngoing(true)
            setOnlyAlertOnce(true)
            addAction(R.drawable.ic_close_24dp, context.getString(R.string.action_cancel), cancelIntent)
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
        if (preferences.hideNotificationContent().get()) {
            progressNotificationBuilder
                .setContentTitle(context.getString(R.string.notification_check_updates))
                .setContentText("($current/$total)")
        } else {
            val updatingText = anime.joinToString("\n") { it.title.chop(40) }
            progressNotificationBuilder
                .setContentTitle(context.getString(R.string.notification_updating, current, total))
                .setStyle(NotificationCompat.BigTextStyle().bigText(updatingText))
        }

        context.notificationManager.notify(
            Notifications.ID_LIBRARY_PROGRESS,
            progressNotificationBuilder
                .setProgress(total, current, false)
                .build(),
        )
    }

    fun showQueueSizeWarningNotification() {
        val notificationBuilder = context.notificationBuilder(Notifications.CHANNEL_LIBRARY_PROGRESS) {
            setContentTitle(context.getString(R.string.label_warning))
            setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.notification_size_warning)))
            setSmallIcon(R.drawable.ic_warning_white_24dp)
            setTimeoutAfter(AnimeDownloader.WARNING_NOTIF_TIMEOUT_MS)
            setContentIntent(NotificationHandler.openUrl(context, HELP_WARNING_URL))
        }

        context.notificationManager.notify(
            Notifications.ID_LIBRARY_SIZE_WARNING,
            notificationBuilder.build(),
        )
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

        context.notificationManager.notify(
            Notifications.ID_LIBRARY_ERROR,
            context.notificationBuilder(Notifications.CHANNEL_LIBRARY_ERROR) {
                setContentTitle(context.resources.getString(R.string.notification_update_error, failed))
                setContentText(context.getString(R.string.action_show_errors))
                setSmallIcon(R.drawable.ic_ani)

                setContentIntent(NotificationReceiver.openErrorLogPendingActivity(context, uri))
            }
                .build(),
        )
    }

    /**
     * Shows notification containing update entries that were skipped.
     *
     * @param skipped Number of entries that were skipped during the update.
     */
    fun showUpdateSkippedNotification(skipped: Int) {
        if (skipped == 0) {
            return
        }

        context.notificationManager.notify(
            Notifications.ID_LIBRARY_SKIPPED,
            context.notificationBuilder(Notifications.CHANNEL_LIBRARY_SKIPPED) {
                setContentTitle(context.resources.getString(R.string.notification_update_skipped, skipped))
                setContentText(context.getString(R.string.learn_more))
                setSmallIcon(R.drawable.ic_ani)
                setContentIntent(NotificationHandler.openUrl(context, HELP_SKIPPED_URL))
            }
                .build(),
        )
    }

    /**
     * Shows the notification containing the result of the update done by the service.
     *
     * @param updates a list of anime with new updates.
     */
    fun showUpdateNotifications(updates: List<Pair<Anime, Array<Episode>>>) {
        NotificationManagerCompat.from(context).apply {
            // Parent group notification
            notify(
                Notifications.ID_NEW_CHAPTERS,
                context.notification(Notifications.CHANNEL_NEW_CHAPTERS_EPISODES) {
                    setContentTitle(context.getString(R.string.notification_new_episodes))
                    if (updates.size == 1 && !preferences.hideNotificationContent().get()) {
                        setContentText(updates.first().first.title.chop(NOTIF_TITLE_MAX_LEN))
                    } else {
                        setContentText(context.resources.getQuantityString(R.plurals.notification_new_episodes_summary, updates.size, updates.size))

                        if (!preferences.hideNotificationContent().get()) {
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

                    setGroup(Notifications.GROUP_NEW_CHAPTERS)
                    setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                    setGroupSummary(true)
                    priority = NotificationCompat.PRIORITY_HIGH

                    setContentIntent(getNotificationIntent())
                    setAutoCancel(true)
                },
            )

            // Per-anime notification
            if (!preferences.hideNotificationContent().get()) {
                launchUI {
                    updates.forEach { (anime, episodes) ->
                        notify(anime.id.hashCode(), createNewEpisodesNotification(anime, episodes))
                    }
                }
            }
        }
    }

    private suspend fun createNewEpisodesNotification(anime: Anime, episodes: Array<Episode>): Notification {
        val icon = getAnimeIcon(anime)
        return context.notification(Notifications.CHANNEL_NEW_CHAPTERS_EPISODES) {
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
            setContentIntent(NotificationReceiver.openEpisodePendingActivity(context, anime, episodes.first()))
            setAutoCancel(true)

            // Mark episodes as read action
            addAction(
                R.drawable.ic_glasses_24dp,
                context.getString(R.string.action_mark_as_seen),
                NotificationReceiver.markAsReadPendingBroadcast(
                    context,
                    anime,
                    episodes,
                    Notifications.ID_NEW_EPISODES,
                ),
            )
            // View episodes action
            addAction(
                R.drawable.ic_book_24dp,
                context.getString(R.string.action_view_episodes),
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
                    context.getString(R.string.action_download),
                    NotificationReceiver.downloadEpisodesPendingBroadcast(
                        context,
                        anime,
                        episodes,
                        Notifications.ID_NEW_CHAPTERS,
                    ),
                )
            }
        }
    }

    /**
     * Cancels the progress notification.
     */
    fun cancelProgressNotification() {
        context.notificationManager.cancel(Notifications.ID_LIBRARY_PROGRESS)
    }

    private suspend fun getAnimeIcon(anime: Anime): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(anime)
            .transformations(CircleCropTransformation())
            .size(NOTIF_ICON_SIZE)
            .build()
        val drawable = context.imageLoader.execute(request).drawable
        return (drawable as? BitmapDrawable)?.bitmap
    }

    private fun getNewEpisodesDescription(episodes: Array<Episode>): String {
        val formatter = DecimalFormat(
            "#.###",
            DecimalFormatSymbols()
                .apply { decimalSeparator = '.' },
        )

        val displayableEpisodeNumbers = episodes
            .filter { it.isRecognizedNumber }
            .sortedBy { it.episodeNumber }
            .map { formatter.format(it.episodeNumber) }
            .toSet()

        return when (displayableEpisodeNumbers.size) {
            // No sensible episode numbers to show (i.e. no episodes have parsed episode number)
            0 -> {
                // "1 new episode" or "5 new episodes"
                context.resources.getQuantityString(R.plurals.notification_episodes_generic, episodes.size, episodes.size)
            }
            // Only 1 episode has a parsed episode number
            1 -> {
                val remaining = episodes.size - displayableEpisodeNumbers.size
                if (remaining == 0) {
                    // "Episode 2.5"
                    context.resources.getString(R.string.notification_episodes_single, displayableEpisodeNumbers.first())
                } else {
                    // "Episode 2.5 and 10 more"
                    context.resources.getString(R.string.notification_episodes_single_and_more, displayableEpisodeNumbers.first(), remaining)
                }
            }
            // Everything else (i.e. multiple parsed episode numbers)
            else -> {
                val shouldTruncate = displayableEpisodeNumbers.size > NOTIF_MAX_EPISODES
                if (shouldTruncate) {
                    // "Episodes 1, 2.5, 3, 4, 5 and 10 more"
                    val remaining = displayableEpisodeNumbers.size - NOTIF_MAX_EPISODES
                    val joinedEpisodeNumbers = displayableEpisodeNumbers.take(NOTIF_MAX_EPISODES).joinToString(", ")
                    context.resources.getQuantityString(R.plurals.notification_episodes_multiple_and_more, remaining, joinedEpisodeNumbers, remaining)
                } else {
                    // "Episodes 1, 2.5, 3"
                    context.resources.getString(R.string.notification_episodes_multiple, displayableEpisodeNumbers.joinToString(", "))
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
            action = MainActivity.SHORTCUT_RECENTLY_UPDATED
        }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val HELP_WARNING_URL = "https://aniyomi.org/help/faq/#why-does-the-app-warn-about-large-bulk-updates-and-downloads"
    }
}

private const val NOTIF_MAX_EPISODES = 5
private const val NOTIF_TITLE_MAX_LEN = 45
private const val NOTIF_ICON_SIZE = 192
private const val HELP_SKIPPED_URL = "https://aniyomi.org/help/faq/#why-does-global-update-skip-some-entries"
