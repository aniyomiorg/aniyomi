package eu.kanade.tachiyomi.ui.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.ImagePicker
import com.google.android.gms.cast.framework.media.MediaIntentReceiver
import com.google.android.gms.cast.framework.media.NotificationAction
import com.google.android.gms.cast.framework.media.NotificationActionsProvider
import com.google.android.gms.cast.framework.media.NotificationOptions
import com.google.android.gms.cast.framework.media.widget.ExpandedControllerActivity
import com.google.android.gms.common.images.WebImage
import eu.kanade.tachiyomi.R

open class CastOptionsProvider : OptionsProvider {

    private fun getReceiverApplicationId(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_META_DATA)
            packageInfo.applicationInfo?.metaData?.getString(context.getString(R.string.app_cast_id))
                ?: CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
        } catch (e: PackageManager.NameNotFoundException) {
            CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
        }
    }

    @SuppressLint("VisibleForTests")
    override fun getCastOptions(context: Context): CastOptions {
        val notificationOptions = NotificationOptions.Builder()
            .setTargetActivityClassName(ExpandedControlsActivity::class.java.name)
            .setNotificationActionsProvider(
                object : NotificationActionsProvider(context) {
                    override fun getNotificationActions(): List<NotificationAction> {
                        return when {
                            hasQueue() -> listOf(
                                NotificationAction.Builder().setAction(MediaIntentReceiver.ACTION_REWIND).build(),
                                NotificationAction.Builder().setAction(
                                    MediaIntentReceiver.ACTION_TOGGLE_PLAYBACK,
                                ).build(),
                                NotificationAction.Builder().setAction(MediaIntentReceiver.ACTION_FORWARD).build(),
                                NotificationAction.Builder().setAction(MediaIntentReceiver.ACTION_STOP_CASTING).build(),

                            )
                            isPhoto -> listOf(
                                NotificationAction.Builder().setAction(
                                    MediaIntentReceiver.ACTION_TOGGLE_PLAYBACK,
                                ).build(),
                                NotificationAction.Builder().setAction(MediaIntentReceiver.ACTION_STOP_CASTING).build(),
                            )
                            else -> listOf(
                                NotificationAction.Builder().setAction(MediaIntentReceiver.ACTION_REWIND).build(),
                                NotificationAction.Builder().setAction(
                                    MediaIntentReceiver.ACTION_TOGGLE_PLAYBACK,
                                ).build(),
                                NotificationAction.Builder().setAction(MediaIntentReceiver.ACTION_FORWARD).build(),
                                NotificationAction.Builder().setAction(MediaIntentReceiver.ACTION_STOP_CASTING).build(),
                            )
                        }
                    }

                    override fun getCompactViewActionIndices(): IntArray {
                        return when {
                            hasQueue() -> intArrayOf(1, 2)
                            isPhoto -> intArrayOf(0, 1)
                            else -> intArrayOf(1, 3)
                        }
                    }

                    private fun hasQueue(): Boolean {
                        val mediaStatus = mediaStatus
                        return mediaStatus != null && mediaStatus.queueItemCount > 1
                    }

                    private val isPhoto: Boolean
                        get() {
                            val mediaStatus = mediaStatus ?: return false
                            val mediaInfo = mediaStatus.mediaInfo ?: return false
                            val metadata = mediaInfo.metadata ?: return false
                            return metadata.mediaType == MediaMetadata.MEDIA_TYPE_PHOTO
                        }

                    private val mediaStatus: MediaStatus?
                        get() {
                            val castContext = CastContext.getSharedInstance(applicationContext)
                            val castSession = castContext.sessionManager.currentCastSession ?: return null
                            val client = castSession.remoteMediaClient ?: return null
                            return client.mediaStatus
                        }
                },
            )
            .build()

        val mediaOptions = CastMediaOptions.Builder()
            .setImagePicker(ImagePickerImpl())
            .setNotificationOptions(notificationOptions)
            .setExpandedControllerActivityClassName(ExpandedControllerActivity::class.java.name)
            .build()

        return CastOptions.Builder()
            .setReceiverApplicationId(getReceiverApplicationId(context))
            .setCastMediaOptions(mediaOptions)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? {
        return null
    }

    private class ImagePickerImpl : ImagePicker() {
        @Deprecated("Deprecated in Java")
        override fun onPickImage(mediaMetadata: MediaMetadata?, type: Int): WebImage? {
            if (mediaMetadata == null || !mediaMetadata.hasImages()) {
                return null
            }
            val images = mediaMetadata.images
            return if (images.size == 1) {
                images[0]
            } else {
                if (type ==
                    IMAGE_TYPE_MEDIA_ROUTE_CONTROLLER_DIALOG_BACKGROUND
                ) {
                    images[0]
                } else {
                    images[1]
                }
            }
        }
    }
}
