@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.animesource.model

import kotlinx.serialization.json.JsonObject
import mihon.core.common.extensions.EMPTY

class SAnimeImpl : SAnime {

    override lateinit var url: String

    override lateinit var title: String

    override var thumbnail_url: String? = null

    override var background_url: String? = null

    override var artist: String? = null

    override var author: String? = null

    override var status: Int = 0

    override var description: String? = null

    override var genre: String? = null

    override var initialized: Boolean = false

    override var update_strategy: AnimeUpdateStrategy = AnimeUpdateStrategy.ALWAYS_UPDATE

    override var fetch_type: FetchType = FetchType.Episodes

    override var season_number: Double = -1.0

    override var memo: JsonObject = JsonObject.EMPTY
}
