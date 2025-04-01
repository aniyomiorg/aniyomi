package tachiyomi.domain.source.anime.model

data class AnimeSource(
    val id: Long,
    val lang: String,
    val name: String,
    val supportsLatest: Boolean,
    val isStub: Boolean,
    val pin: Pins = Pins.unpinned,
    val isUsedLast: Boolean = false,
    // SY -->
    val category: String? = null,
    val isExcludedFromDataSaver: Boolean = false,
    val categories: Set<String> = emptySet(),
    // SY <--
) {

    val visualName: String
        get() = when {
            lang.isEmpty() -> name
            else -> "$name (${lang.uppercase()})"
        }

    val key: () -> String = {
        when {
            isUsedLast -> "$id-lastused"
            // SY -->
            category != null -> "$id-$category"
            // SY <--
            else -> "$id"
        }
    }
}
