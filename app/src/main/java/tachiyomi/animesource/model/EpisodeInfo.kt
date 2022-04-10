package tachiyomi.animesource.model

data class EpisodeInfo(
    var key: String,
    var name: String,
    var dateUpload: Long = 0,
    var number: Float = -1f,
    var scanlator: String = "",
)
