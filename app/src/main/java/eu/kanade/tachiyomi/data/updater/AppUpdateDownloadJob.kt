package eu.kanade.tachiyomi.data.updater

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.workManager
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import okio.buffer
import okio.sink
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.io.IOException

class AppUpdateDownloadJob(
    private val context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    private val notifier = AppUpdateNotifier(context)
    private val network: NetworkHelper by injectLazy()

    @Suppress("SwallowedException")
    override suspend fun doWork(): Result {
        val url = inputData.getString(EXTRA_DOWNLOAD_URL) ?: return Result.failure()
        val title = inputData.getString(EXTRA_DOWNLOAD_TITLE) ?: context.stringResource(MR.strings.app_name)
        return try {
            downloadApk(title, url)
            Result.success()
        } catch (e: Exception) {
            notifier.onDownloadError(url)
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val title = inputData.getString(EXTRA_DOWNLOAD_TITLE) ?: context.stringResource(MR.strings.app_name)
        return ForegroundInfo(
            Notifications.ID_APP_UPDATER,
            notifier.onDownloadStarted(title).build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    /**
     * Called to start downloading apk of new update
     *
     * @param url url location of file
     */
    @Suppress("MagicNumber")
    private fun downloadApk(title: String, url: String) {
        val request = Request.Builder().url(url).build()
        network.client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                notifier.onDownloadError(url)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    notifier.onDownloadError(url)
                    return
                }

                val body = response.body ?: return
                val contentLength = body.contentLength()
                val source = body.source()
                val file = File(context.externalCacheDir, "update.apk")

                file.sink().buffer().use { sink ->
                    var totalBytesRead: Long = 0
                    var bytesRead: Long

                    notifier.onDownloadStarted(title)
                    while (source.read(sink.buffer, 2048).also { bytesRead = it } != -1L) {
                        totalBytesRead += bytesRead
                        val progress = (totalBytesRead * 100 / contentLength).toInt()
                        notifier.onProgressChange(progress)
                    }
                }
                notifier.promptInstall(file.getUriCompat(context))
            }
        })
    }

    companion object {
        private const val TAG = "AppUpdateDownload"

        const val EXTRA_DOWNLOAD_URL = "DOWNLOAD_URL"
        const val EXTRA_DOWNLOAD_TITLE = "DOWNLOAD_TITLE"

        fun start(context: Context, url: String, title: String? = null) {
            val constraints = Constraints(
                requiredNetworkType = NetworkType.CONNECTED,
            )

            val request = OneTimeWorkRequestBuilder<AppUpdateDownloadJob>()
                .setConstraints(constraints)
                .addTag(TAG)
                .setInputData(
                    workDataOf(
                        EXTRA_DOWNLOAD_URL to url,
                        EXTRA_DOWNLOAD_TITLE to title,
                    ),
                )
                .build()

            context.workManager.enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
        }

        fun stop(context: Context) {
            context.workManager.cancelUniqueWork(TAG)
        }
    }
}
