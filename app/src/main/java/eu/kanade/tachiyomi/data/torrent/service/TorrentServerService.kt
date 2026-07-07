package eu.kanade.tachiyomi.data.torrent.service

import android.app.Application
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import aniyomi.core.common.torrent.ProxyMode
import aniyomi.core.common.torrent.TorrentPreferences
import aniyomi.core.common.torrent.TorrentServerApi
import aniyomi.core.common.torrent.TorrentServerUtils
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.util.system.cancelNotification
import eu.kanade.tachiyomi.util.system.notificationBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import xyz.secozzi.torrserver.TorrServer
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration.Companion.seconds

class TorrentServerService : Service() {
    private val serviceScope = CoroutineScope(EmptyCoroutineContext)
    private val applicationContext = Injekt.get<Application>()
    private val networkPreferences = Injekt.get<NetworkPreferences>()
    private val torrentPreferences = Injekt.get<TorrentPreferences>()
    private val torrentServerUtils = Injekt.get<TorrentServerUtils>()
    private val api = Injekt.get<TorrentServerApi>()

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (it.action != null) {
                when (it.action) {
                    ACTION_START -> {
                        startServer()
                        notification(applicationContext)
                        return START_STICKY
                    }
                    ACTION_STOP -> {
                        stopServer()
                        return START_NOT_STICKY
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startServer() {
        serviceScope.launch {
            if (api.echo() == "") {
                if (networkPreferences.verboseLogging().get()) {
                    TorrServer.registerLogCallback()
                }

                val proxyMode = torrentPreferences.torrServerProxyMode().get()
                val port = TorrServer.startServer(
                    port = torrentPreferences.torrServerPort().get(),
                    path = filesDir.absolutePath,
                    proxyMode = proxyMode.value,
                    proxyUrl = if (proxyMode == ProxyMode.None) "" else torrentPreferences.torrServerProxyUrl().get(),
                )
                if (port != -1) {
                    api.setPort(port)
                    wait(10)
                    torrentServerUtils.setTrackersList()
                }
            }
        }
    }

    private fun stopServer() {
        serviceScope.launch {
            TorrServer.stopServer()
            applicationContext.cancelNotification(Notifications.ID_TORRENT_SERVER)
            stopSelf()
        }
    }

    private fun notification(context: Context) {
        // fuck android 14
        val startAgainIntent = PendingIntent.getService(
            applicationContext,
            0,
            Intent(applicationContext, TorrentServerService::class.java).apply {
                action = ACTION_START
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val exitPendingIntent =
            PendingIntent.getService(
                applicationContext,
                0,
                Intent(applicationContext, TorrentServerService::class.java).apply {
                    action = ACTION_STOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val builder = context.notificationBuilder(Notifications.CHANNEL_TORRENT_SERVER) {
            setSmallIcon(R.drawable.ic_ani)
            setContentText(stringResource(AYMR.strings.torrentserver_is_running))
            setContentTitle(stringResource(MR.strings.app_name))
            setAutoCancel(false)
            setOngoing(true)
            setDeleteIntent(startAgainIntent)
            setUsesChronometer(true)
            addAction(
                R.drawable.ic_close_24dp,
                "Stop",
                exitPendingIntent,
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                Notifications.ID_TORRENT_SERVER,
                builder.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(Notifications.ID_TORRENT_SERVER, builder.build())
        }
    }

    companion object {
        const val ACTION_START = "start_torrent_server"
        const val ACTION_STOP = "stop_torrent_server"
        val applicationContext = Injekt.get<Application>()
        val api = Injekt.get<TorrentServerApi>()

        suspend fun start() {
            try {
                val intent =
                    Intent(applicationContext, TorrentServerService::class.java).apply {
                        action = ACTION_START
                    }
                applicationContext.startService(intent)
                wait(10)
            } catch (e: Exception) {
                logcat(LogPriority.DEBUG, e) { "Failed to start torrent service" }
                e.printStackTrace()
            }
        }

        fun stop() {
            try {
                val intent =
                    Intent(applicationContext, TorrentServerService::class.java).apply {
                        action = ACTION_STOP
                    }
                applicationContext.startService(intent)
            } catch (e: Exception) {
                logcat(LogPriority.DEBUG, e) { "Failed to stop torrent service" }
                e.printStackTrace()
            }
        }

        suspend fun wait(timeout: Int = -1): Boolean {
            var count = 0
            if (timeout < 0) {
                count = -20
            }
            while (api.echo() == "") {
                delay(1.seconds)
                count++
                if (count > timeout) {
                    return false
                }
            }
            return true
        }
    }
}
