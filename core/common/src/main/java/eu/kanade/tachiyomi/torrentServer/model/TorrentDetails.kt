package eu.kanade.tachiyomi.torrentServer.model

import kotlinx.serialization.Serializable

@Serializable
@Suppress("ConstructorParameterNaming")
data class TorrentDetails(
    val Title: String,
    val Name: String,
    val Names: List<String>,
    val Categories: String,
    val Size: String,
    val CreateDate: String,
    val Tracker: String,
    val Link: String,
    val Year: Int,
    val Peer: Int,
    val Seed: Int,
    val Magnet: String,
    val Hash: String,
    val IMDBID: String,
    val VideoQuality: Int,
    val AudioQuality: Int,
)
