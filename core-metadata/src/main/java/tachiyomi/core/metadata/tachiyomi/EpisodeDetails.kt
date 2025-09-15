package tachiyomi.core.metadata.tachiyomi

import kotlinx.serialization.Serializable

@Serializable
class EpisodeDetails(
    val episode_number: Float,
    val name: String? = null,
    val date_upload: String? = null,
    val fillermark: Boolean = false,
    val scanlator: String? = null,
    val summary: String? = null,
    val preview_url: String? = null,
)
