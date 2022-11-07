package eu.kanade.tachiyomi.util

sealed class Video

data class VideoUrl(val url: String) : Video()
