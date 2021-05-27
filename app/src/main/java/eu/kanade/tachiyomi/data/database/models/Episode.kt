package eu.kanade.tachiyomi.data.database.models

import eu.kanade.tachiyomi.animesource.model.SEpisode
import java.io.Serializable

interface Episode : SEpisode, Serializable {

    var id: Long?

    var anime_id: Long?

    var seen: Boolean

    var bookmark: Boolean

    var last_second_seen: Long

    var total_seconds: Long

    var date_fetch: Long

    var source_order: Int

    val isRecognizedNumber: Boolean
        get() = episode_number >= 0f

    companion object {

        fun create(): Episode = EpisodeImpl().apply {
            episode_number = -1f
        }
    }
}
