package tachiyomi.animesource.model

sealed class Video

data class VideoUrl(val url: String) : Video()
