package eu.kanade.tachiyomi.data.track.simkl

import kotlinx.serialization.Serializable

@Serializable
data class OAuth(
    val access_token: String,
    val token_type: String,
    val scope: String,
)
