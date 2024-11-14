package eu.kanade.tachiyomi.ui.player

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import `is`.xyz.mpv.Utils
import logcat.LogPriority
import logcat.logcat

internal fun Uri.openContentFd(context: Context): String? {
    return context.contentResolver.openFileDescriptor(this, "r")?.detachFd()?.let {
        Utils.findRealPath(it)?.also { _ ->
            ParcelFileDescriptor.adoptFd(it).close()
        } ?: "fd://$it"
    }
}

internal fun Uri.resolveUri(context: Context): String? {
    val filepath = when (scheme) {
        "file" -> path
        "content" -> openContentFd(context)
        "data" -> "data://$schemeSpecificPart"
        in Utils.PROTOCOLS -> toString()
        else -> null
    }

    if (filepath == null) logcat(LogPriority.ERROR) { "unknown scheme: $scheme" }
    return filepath
}

internal val videoExtensions = listOf(
    "264", "265", "3g2", "3ga", "3gp", "3gp2", "3gpp", "3gpp2", "3iv", "amr", "asf",
    "asx", "av1", "avc", "avf", "avi", "bdm", "bdmv", "clpi", "cpi", "divx", "dv", "evo",
    "evob", "f4v", "flc", "fli", "flic", "flv", "gxf", "h264", "h265", "hdmov", "hdv",
    "hevc", "lrv", "m1u", "m1v", "m2t", "m2ts", "m2v", "m4u", "m4v", "mkv", "mod", "moov",
    "mov", "mp2", "mp2v", "mp4", "mp4v", "mpe", "mpeg", "mpeg2", "mpeg4", "mpg", "mpg4",
    "mpl", "mpls", "mpv", "mpv2", "mts", "mtv", "mxf", "mxu", "nsv", "nut", "ogg", "ogm",
    "ogv", "ogx", "qt", "qtvr", "rm", "rmj", "rmm", "rms", "rmvb", "rmx", "rv", "rvx",
    "sdp", "tod", "trp", "ts", "tsa", "tsv", "tts", "vc1", "vfw", "vob", "vro", "webm",
    "wm", "wmv", "wmx", "x264", "x265", "xvid", "y4m", "yuv",
)

internal val audioExtensions = listOf(
    "3ga", "3ga2", "a52", "aac", "ac3", "adt", "adts", "aif", "aifc", "aiff", "alac",
    "amr", "ape", "au", "awb", "dsf", "dts", "dts-hd", "dtshd", "eac3", "f4a", "flac",
    "lpcm", "m1a", "m2a", "m4a", "mk3d", "mka", "mlp", "mp+", "mp1", "mp2", "mp3", "mpa",
    "mpc", "mpga", "mpp", "oga", "ogg", "opus", "pcm", "ra", "ram", "rax", "shn", "snd",
    "spx", "tak", "thd", "thd+ac3", "true-hd", "truehd", "tta", "wav", "weba", "wma", "wv",
    "wvp",
)

internal val imageExtensions = listOf(
    "apng", "bmp", "exr", "gif", "j2c", "j2k", "jfif", "jp2", "jpc", "jpe", "jpeg", "jpg",
    "jpg2", "png", "tga", "tif", "tiff", "webp",
)
