@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.animesource.model

import kotlinx.serialization.json.JsonObject
import java.io.Serializable

interface SEpisode : Serializable {

    var url: String

    var name: String

    var episode_number: Float

    var fillermark: Boolean

    var scanlator: String?

    var date_upload: Long

    var summary: String?

    var preview_url: String?

    /**
     * Extra metadata associated with the episode.
     *
     * The JSON object is not visible to users and intended for internal or source-specific
     * purposes. Apps may define their own namespaced keys (e.g., `"aniyomi.*"`) for sources to populate.
     *
     * This allows apps to attach and ask for custom information without affecting the visible
     * episode data.
     *
     * @since extensions-lib 17
     */
    var memo: JsonObject

    fun copyFrom(other: SEpisode) {
        name = other.name
        url = other.url
        date_upload = other.date_upload
        episode_number = other.episode_number
        fillermark = other.fillermark
        scanlator = other.scanlator
        summary = other.summary
        preview_url = other.preview_url
        memo = other.memo
    }

    companion object {
        fun create(): SEpisode {
            return SEpisodeImpl()
        }
    }
}
