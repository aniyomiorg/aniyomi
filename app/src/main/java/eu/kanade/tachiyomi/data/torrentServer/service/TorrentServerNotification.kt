package eu.kanade.tachiyomi.data.torrentServer.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.cancelNotification
import eu.kanade.tachiyomi.util.system.notify
import tachiyomi.core.i18n.stringResource
import tachiyomi.i18n.MR

class TorrentServerNotification : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("LaunchActivityFromNotification")
    override fun onCreate() {
        applicationContext.notify(
            Notifications.ID_TORRENT_SERVER,
            Notifications.CHANNEL_TORRENT_SERVER,
        ) {
            setContentTitle(stringResource(MR.strings.app_name))
            setContentText(stringResource(MR.strings.torrentserver_isrunning))
            setSmallIcon(R.drawable.ic_ani)
            setOngoing(true)
            val exitPendingIntent =
                PendingIntent.getService(
                    applicationContext,
                    0,
                    Intent(applicationContext, TorrentServerService::class.java).apply {
                        action = TorrentServerService.ACTION_STOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            setContentIntent(exitPendingIntent)
            addAction(
                R.drawable.ic_close_24dp,
                "Stop",
                exitPendingIntent,
            )
        }
    }

    override fun onDestroy() {
        cancelNotification(Notifications.ID_TORRENT_SERVER)
    }
}
