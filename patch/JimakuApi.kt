package eu.kanade.tachiyomi.ui.player.utils

import eu.kanade.tachiyomi.network.GET
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import logcat.LogPriority
import okhttp3.OkHttpClient
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.injectLazy

/**
 * Client for jimaku.cc, used to automatically fetch community-provided
 * Japanese subtitle files for a given AniList entry/episode.
 *
 * Every matching subtitle file is returned so the player can offer them
 * all as selectable subtitle tracks, similar to browsing jimaku.cc manually.
 */
class JimakuApi {
    private val client = OkHttpClient()
    private val json: Json by injectLazy()

    /**
     * Fetches every usable subtitle track for [anilistId]/[episode].
     * Returns an empty list on any failure (not found, rate limited, network error, etc.)
     * so callers can treat this as a best-effort, silent enhancement.
     */
    fun getSubtitles(anilistId: Long, episode: Int): List<JimakuSubtitle> {
        return try {
            val entry = searchEntry(anilistId) ?: return emptyList()
            val files = getFiles(entry.id, episode)
            files
                .filter { it.name.substringAfterLast('.', "").equals("srt", ignoreCase = true) }
                .filterNot { OP_ED_REGEX.matches(it.name) }
                .mapNotNull { file ->
                    val label = GROUP_NAME_REGEX.find(file.name)?.groupValues?.get(1) ?: file.name
                    file.url?.let { url -> JimakuSubtitle(url = url, label = label) }
                }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Jimaku: failed to fetch subtitles for $anilistId ep $episode" }
            emptyList()
        }
    }

    private fun searchEntry(anilistId: Long): JimakuEntry? {
        val url = "https://jimaku.cc/api/entries/search?anilist_id=$anilistId"
        val response = client.newCall(GET(url, headers = jimakuHeaders())).execute()
        val body = response.body.string()

        // The API returns a JSON object (e.g. an error/rate-limit payload) instead of
        // a list when something goes wrong.
        if (!body.trimStart().startsWith("[")) return null

        val entries = json.decodeFromString<List<JimakuEntry>>(body)
        return entries.firstOrNull()
    }

    private fun getFiles(entryId: Int, episode: Int): List<JimakuFile> {
        val url = "https://jimaku.cc/api/entries/$entryId/files?episode=$episode"
        val response = client.newCall(GET(url, headers = jimakuHeaders())).execute()
        val body = response.body.string()

        if (!body.trimStart().startsWith("[")) return emptyList()

        return json.decodeFromString<List<JimakuFile>>(body)
    }

    private fun jimakuHeaders() = okhttp3.Headers.Builder()
        .add("Authorization", JIMAKU_TOKEN)
        .build()

    companion object {
        // Personal-use token, kept fixed for now.
        private const val JIMAKU_TOKEN = "AAAAAAAAAMouAS6VaLBnJOs2yk0-foUB1Sbcc-LaSd9txD-YRcZNyBtRUA"

        // Matches "ED1.srt", "ED.ass", etc. so ending themes are excluded.
        private val OP_ED_REGEX = Regex("""ED(\d+)?\..+""", RegexOption.IGNORE_CASE)

        // Extracts the group/release name inside the leading [brackets], e.g.
        // "[shincaps] LIAR GAME - 01 (...).srt" -> "shincaps"
        private val GROUP_NAME_REGEX = Regex("""^\[([^]]+)]""")
    }
}

data class JimakuSubtitle(
    val url: String,
    val label: String,
)

@Serializable
data class JimakuEntry(
    @SerialName("id") val id: Int,
)

@Serializable
data class JimakuFile(
    @SerialName("name") val name: String,
    @SerialName("url") val url: String? = null,
)
