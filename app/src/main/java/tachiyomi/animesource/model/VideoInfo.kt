package tachiyomi.animesource.model

sealed class VideoInfo

data class VideoUrl(val url: String) : VideoInfo()
