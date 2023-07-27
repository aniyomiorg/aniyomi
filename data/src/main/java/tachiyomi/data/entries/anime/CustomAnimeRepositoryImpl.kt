package tachiyomi.data.entries.anime

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tachiyomi.domain.entries.anime.model.CustomAnimeInfo
import tachiyomi.domain.entries.anime.repository.CustomAnimeRepository
import java.io.File

class CustomAnimeRepositoryImpl(context: Context) : CustomAnimeRepository {
    private val editJson = File(context.getExternalFilesDir(null), "edits.json")

    private val customAnimeMap = fetchCustomData()

    override fun get(animeId: Long) = customAnimeMap[animeId]

    private fun fetchCustomData(): MutableMap<Long, CustomAnimeInfo> {
        if (!editJson.exists() || !editJson.isFile) return mutableMapOf()

        val json = try {
            Json.decodeFromString<AnimeList>(
                editJson.bufferedReader().use { it.readText() },
            )
        } catch (e: Exception) {
            null
        } ?: return mutableMapOf()

        val animesJson = json.animes ?: return mutableMapOf()
        return animesJson
            .mapNotNull { animeJson ->
                val id = animeJson.id ?: return@mapNotNull null
                id to animeJson.toAnime()
            }
            .toMap()
            .toMutableMap()
    }

    override fun set(animeInfo: CustomAnimeInfo) {
        if (
            animeInfo.title == null &&
            animeInfo.author == null &&
            animeInfo.artist == null &&
            animeInfo.description == null &&
            animeInfo.genre == null &&
            animeInfo.status == null
        ) {
            customAnimeMap.remove(animeInfo.id)
        } else {
            customAnimeMap[animeInfo.id] = animeInfo
        }
        saveCustomInfo()
    }

    private fun saveCustomInfo() {
        val jsonElements = customAnimeMap.values.map { it.toJson() }
        if (jsonElements.isNotEmpty()) {
            editJson.delete()
            editJson.writeText(Json.encodeToString(AnimeList(jsonElements)))
        }
    }

    @Serializable
    data class AnimeList(
        val animes: List<AnimeJson>? = null,
    )

    @Serializable
    data class AnimeJson(
        var id: Long? = null,
        val title: String? = null,
        val author: String? = null,
        val artist: String? = null,
        val description: String? = null,
        val genre: List<String>? = null,
        val status: Long? = null,
    ) {

        fun toAnime() = CustomAnimeInfo(
            id = this@AnimeJson.id!!,
            title = this@AnimeJson.title?.takeUnless { it.isBlank() },
            author = this@AnimeJson.author,
            artist = this@AnimeJson.artist,
            description = this@AnimeJson.description,
            genre = this@AnimeJson.genre,
            status = this@AnimeJson.status?.takeUnless { it == 0L },
        )
    }

    fun CustomAnimeInfo.toJson(): AnimeJson {
        return AnimeJson(
            id,
            title,
            author,
            artist,
            description,
            genre,
            status,
        )
    }
}
