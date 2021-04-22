package eu.kanade.tachiyomi.extension.model

sealed class AnimeLoadResult {

    class Success(val extension: AnimeExtension.Installed) : AnimeLoadResult()
    class Untrusted(val extension: AnimeExtension.Untrusted) : AnimeLoadResult()
    class Error(val message: String? = null) : AnimeLoadResult() {
        constructor(exception: Throwable) : this(exception.message)
    }
}
