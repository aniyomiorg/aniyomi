package eu.kanade.tachiyomi.util.storage

import android.content.Context
import android.net.Uri
import android.os.Build
import com.arthenica.ffmpegkit.FFmpegKitConfig
import java.io.File

fun String.toFFmpegString(context: Context): String {
    return File(this).getUriCompat(context).toFFmpegString(context)
}

fun Uri.toFFmpegString(context: Context): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && this.scheme == "content") {
        FFmpegKitConfig.getSafParameter(context, this, "rw")
    } else {
        this.path!!
    }.replace("\"", "\\\"")
}
