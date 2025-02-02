package eu.kanade.tachiyomi.util

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import eu.kanade.tachiyomi.R
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import java.io.IOException

class LocalHttpServerService : Service() {

    companion object {
        const val CHANNEL_ID = "LocalHttpServerChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "eu.kanade.tachiyomi.ACTION_STOP_SERVER"
    }

    private var server: LocalHttpServer? = null

    override fun onCreate() {
        super.onCreate()

        server = LocalHttpServer(LocalHttpServerHolder.PORT, contentResolver)
        try {
            server?.start()
            logcat(LogPriority.DEBUG) { "Local HTTP server started at  ${LocalHttpServerHolder.PORT}" }
        } catch (e: IOException) {
            logcat(LogPriority.ERROR) { "Error to start local HTTP server" }
            stopSelf()
            return
        }

        createNotificationChannel()

        val stopIntent = Intent(this, LocalHttpServerService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStopIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentText(stringResource(TLMR.strings.server_local_notification_casting))
            .setSmallIcon(R.drawable.cast_ic_notification_0)
            .addAction(
                R.drawable.quantum_ic_stop_white_24,
                "Stop",
                pendingStopIntent,
            )
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        server?.stop()
        logcat(LogPriority.DEBUG) { "Stopping local HTTP server" }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Creates the notification channel (required for Android Oreo or higher).
     */
    @SuppressLint("ObsoleteSdkInt")
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = stringResource(MR.strings.app_name)
            val descriptionText = stringResource(TLMR.strings.server_local_notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
