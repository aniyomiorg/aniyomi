package eu.kanade.tachiyomi.extension.anime.util

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.extension.anime.installer.InstallerAnime
import eu.kanade.tachiyomi.extension.anime.installer.PackageInstallerInstallerAnime
import eu.kanade.tachiyomi.extension.anime.installer.ShizukuInstallerAnime
import eu.kanade.tachiyomi.extension.anime.util.AnimeExtensionInstaller.Companion.EXTRA_DOWNLOAD_ID
import eu.kanade.tachiyomi.util.system.getSerializableExtraCompat
import eu.kanade.tachiyomi.util.system.notificationBuilder
import logcat.LogPriority
import tachiyomi.core.util.system.logcat

class AnimeExtensionInstallService : Service() {

    private var installer: InstallerAnime? = null

    override fun onCreate() {
        val notification = notificationBuilder(Notifications.CHANNEL_EXTENSIONS_UPDATE) {
            setSmallIcon(R.drawable.ic_ani)
            setAutoCancel(false)
            setOngoing(true)
            setShowWhen(false)
            setContentTitle(getString(R.string.ext_install_service_notif))
            setProgress(100, 100, true)
        }.build()
        startForeground(Notifications.ID_EXTENSION_INSTALLER, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uri = intent?.data
        val id = intent?.getLongExtra(EXTRA_DOWNLOAD_ID, -1)?.takeIf { it != -1L }
        val installerUsed = intent?.getSerializableExtraCompat<BasePreferences.ExtensionInstaller>(
            EXTRA_INSTALLER,
        )
        if (uri == null || id == null || installerUsed == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (installer == null) {
            installer = when (installerUsed) {
                BasePreferences.ExtensionInstaller.PACKAGEINSTALLER -> PackageInstallerInstallerAnime(this)
                BasePreferences.ExtensionInstaller.SHIZUKU -> ShizukuInstallerAnime(this)
                else -> {
                    logcat(LogPriority.ERROR) { "Not implemented for installer $installerUsed" }
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
        }
        installer!!.addToQueue(id, uri)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        installer?.onDestroy()
        installer = null
    }

    override fun onBind(i: Intent?): IBinder? = null

    companion object {
        private const val EXTRA_INSTALLER = "EXTRA_INSTALLER"

        fun getIntent(
            context: Context,
            downloadId: Long,
            uri: Uri,
            installer: BasePreferences.ExtensionInstaller,
        ): Intent {
            return Intent(context, AnimeExtensionInstallService::class.java)
                .setDataAndType(uri, AnimeExtensionInstaller.APK_MIME)
                .putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                .putExtra(EXTRA_INSTALLER, installer)
        }
    }
}
