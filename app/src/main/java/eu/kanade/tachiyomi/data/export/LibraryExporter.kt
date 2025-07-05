package eu.kanade.tachiyomi.data.export

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.manga.model.Manga

enum class ExportEntryType {
    MANGA,
    ANIME,
}

data class ExportEntry(
    val type: ExportEntryType,
    val title: String,
    val author: String?,
    val artist: String?,
) {
    companion object {
        fun Anime.toExportEntry(): ExportEntry = ExportEntry(
            title = this.title,
            type = ExportEntryType.ANIME,
            author = this.author,
            artist = this.artist,
        )

        fun Manga.toExportEntry(): ExportEntry = ExportEntry(
            title = this.title,
            type = ExportEntryType.MANGA,
            author = this.author,
            artist = this.artist,
        )
    }
}

object LibraryExporter {

    data class ExportOptions(
        val includeTitle: Boolean,
        val includeType: Boolean,
        val includeAuthor: Boolean,
        val includeArtist: Boolean,
    )

    suspend fun exportToCsv(
        context: Context,
        uri: Uri,
        favorites: List<ExportEntry>,
        options: ExportOptions,
        onExportComplete: () -> Unit,
    ) {
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val csvData = generateCsvData(favorites, options)
                outputStream.write(csvData.toByteArray())
            }
            onExportComplete()
        }
    }

    private val escapeRequired = listOf("\r", "\n", "\"", ",")

    private fun generateCsvData(favorites: List<ExportEntry>, options: ExportOptions): String {
        val columnSize = listOf(
            options.includeTitle,
            options.includeType,
            options.includeAuthor,
            options.includeArtist,
        )
            .count { it }

        val rows = buildList(favorites.size) {
            favorites.forEach { entry ->
                buildList(columnSize) {
                    if (options.includeTitle) add(entry.title)
                    if (options.includeType) add(entry.type.name.lowercase())
                    if (options.includeAuthor) add(entry.author)
                    if (options.includeArtist) add(entry.artist)
                }
                    .let(::add)
            }
        }
        return rows.joinToString("\r\n") { columns ->
            columns.joinToString(",") columns@{ column ->
                if (column.isNullOrBlank()) return@columns ""
                if (escapeRequired.any { column.contains(it) }) {
                    column.replace("\"", "\"\"").let { "\"$it\"" }
                } else {
                    column
                }
            }
        }
    }
}
