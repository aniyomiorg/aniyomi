package eu.kanade.tachiyomi.util.storage

import android.content.Context
import android.os.Build
import com.arthenica.ffmpegkit.FFmpegKitConfig
import java.io.File

fun String.toFFmpegString(context: Context): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        FFmpegKitConfig.getSafParameter(context, File(this).getUriCompat(context), "rw")
    } else {
        File(this).getUriCompat(context).path!!
    }.replace("\"", "\\\"")
}
