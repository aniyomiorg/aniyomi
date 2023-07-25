package eu.kanade.tachiyomi.data.download.anime

import android.app.PendingIntent
import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.download.anime.model.AnimeDownload
import eu.kanade.tachiyomi.data.notification.NotificationHandler
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.lang.chop
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager
import eu.kanade.tachiyomi.util.system.notify
import uy.kohesive.injekt.injectLazy
import java.util.regex.Pattern

/**
 * DownloadNotifier is used to show notifications when downloading one or multiple chapters.
 *
 * @param context context of application
 */
internal class AnimeDownloadNotifier(private val context: Context) {

    private val preferences: SecurityPreferences by injectLazy()

    private val progressNotificationBuilder by lazy {
        context.notificationBuilder(Notifications.CHANNEL_DOWNLOADER_PROGRESS) {
            setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            setAutoCancel(false)
            setOnlyAlertOnce(true)
        }
    }

    private val errorNotificationBuilder by lazy {
        context.notificationBuilder(Notifications.CHANNEL_DOWNLOADER_ERROR) {
            setAutoCancel(false)
        }
    }

    /**
     * Status of download. Used for correct notification icon.
     */
    private var isDownloading = false

    /**
     * Shows a notification from this builder.
     *
     * @param id the id of the notification.
     */
    private fun NotificationCompat.Builder.show(id: Int) {
        context.notify(id, build())
    }

    /**
     * Dismiss the downloader's notification. Downloader error notifications use a different id, so
     * those can only be dismissed by the user.
     */
    fun dismissProgress() {
        context.notificationManager.cancel(Notifications.ID_DOWNLOAD_EPISODE_PROGRESS)
    }

    /**
     * Called when download progress changes.
     *
     * @param download download object containing download information.
     */
    fun onProgressChange(download: AnimeDownload) {
        with(progressNotificationBuilder) {
            if (!isDownloading) {
                setSmallIcon(android.R.drawable.stat_sys_download)
                clearActions()
                // Open download manager when clicked
                setContentIntent(NotificationHandler.openAnimeDownloadManagerPendingActivity(context))
                isDownloading = true
                // Pause action
                addAction(
                    R.drawable.ic_pause_24dp,
                    context.getString(R.string.action_pause),
                    NotificationReceiver.pauseAnimeDownloadsPendingBroadcast(context),
                )
            }

            val downloadingProgressText = if (download.totalProgress == 0) {
                context.getString(R.string.update_check_notification_download_in_progress)
            } else {
                context.getString(R.string.episode_downloading_progress, download.progress)
            }

            if (preferences.hideNotificationContent().get()) {
                setContentTitle(downloadingProgressText)
                setContentText(null)
            } else {
                val title = download.anime.title.chop(15)
                val quotedTitle = Pattern.quote(title)
                val episode = download.episode.name.replaceFirst("$quotedTitle[\\s]*[-]*[\\s]*".toRegex(RegexOption.IGNORE_CASE), "")
                setContentTitle("$title - $episode".chop(30))
                setContentText(downloadingProgressText)
            }
            if (download.totalProgress == 0) {
                setProgress(100, download.progress, true)
            } else {
                setProgress(100, download.progress, false)
            }
            setOngoing(true)

            show(Notifications.ID_DOWNLOAD_EPISODE_PROGRESS)
        }
    }

    /**
     * Show notification when download is paused.
     */
    fun onPaused() {
        with(progressNotificationBuilder) {
            setContentTitle(context.getString(R.string.download_paused))
            setContentText(context.getString(R.string.download_notifier_download_paused_episodes))
            setSmallIcon(R.drawable.ic_pause_24dp)
            setProgress(0, 0, false)
            setOngoing(false)
            clearActions()
            // Open download manager when clicked
            setContentIntent(NotificationHandler.openAnimeDownloadManagerPendingActivity(context))
            // Resume action
            addAction(
                R.drawable.ic_play_arrow_24dp,
                context.getString(R.string.action_resume),
                NotificationReceiver.resumeAnimeDownloadsPendingBroadcast(context),
            )
            // Clear action
            addAction(
                R.drawable.ic_close_24dp,
                context.getString(R.string.action_cancel_all),
                NotificationReceiver.clearAnimeDownloadsPendingBroadcast(context),
            )

            show(Notifications.ID_DOWNLOAD_EPISODE_PROGRESS)
        }

        // Reset initial values
        isDownloading = false
    }

    /**
     *  Resets the state once downloads are completed.
     */
    fun onComplete() {
        dismissProgress()

        // Reset states to default
        isDownloading = false
    }

    /**
     * Called when the downloader receives a warning.
     *
     * @param reason the text to show.
     */
    fun onWarning(reason: String, timeout: Long? = null, contentIntent: PendingIntent? = null) {
        with(errorNotificationBuilder) {
            setContentTitle(context.getString(R.string.download_notifier_downloader_title))
            setStyle(NotificationCompat.BigTextStyle().bigText(reason))
            setSmallIcon(R.drawable.ic_warning_white_24dp)
            setAutoCancel(true)
            clearActions()
            setContentIntent(NotificationHandler.openAnimeDownloadManagerPendingActivity(context))
            setProgress(0, 0, false)
            timeout?.let { setTimeoutAfter(it) }
            contentIntent?.let { setContentIntent(it) }

            show(Notifications.ID_DOWNLOAD_EPISODE_ERROR)
        }

        // Reset download information
        isDownloading = false
    }

    /**
     * Called when the downloader receives an error. It's shown as a separate notification to avoid
     * being overwritten.
     *
     * @param error string containing error information.
     * @param episode string containing episode title.
     */
    fun onError(error: String? = null, episode: String? = null, animeTitle: String? = null) {
        // Create notification
        with(errorNotificationBuilder) {
            setContentTitle(
                animeTitle?.plus(": $episode") ?: context.getString(R.string.download_notifier_downloader_title),
            )
            setContentText(error ?: context.getString(R.string.download_notifier_unknown_error))
            setSmallIcon(R.drawable.ic_warning_white_24dp)
            clearActions()
            setContentIntent(NotificationHandler.openAnimeDownloadManagerPendingActivity(context))
            setProgress(0, 0, false)

            show(Notifications.ID_DOWNLOAD_EPISODE_ERROR)
        }

        // Reset download information
        isDownloading = false
    }
}
