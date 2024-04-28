package eu.kanade.tachiyomi.torrentutils.model

import kotlinx.serialization.Serializable

@Serializable
data class TorrServVersion(
    val version: String,
    val links: Map<String, String>,
)
