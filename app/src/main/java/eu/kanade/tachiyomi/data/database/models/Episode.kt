package eu.kanade.tachiyomi.data.database.models

import eu.kanade.tachiyomi.source.model.SEpisode
import java.io.Serializable

interface Episode : SEpisode, Serializable {

    var id: Long?

    var anime_id: Long?

    var read: Boolean

    var bookmark: Boolean

    var last_page_read: Int

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
