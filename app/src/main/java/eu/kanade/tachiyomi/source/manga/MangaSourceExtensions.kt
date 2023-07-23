package eu.kanade.tachiyomi.source.manga

import android.graphics.drawable.Drawable
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.manga.MangaExtensionManager
import eu.kanade.tachiyomi.source.MangaSource
import tachiyomi.domain.source.manga.model.MangaSourceData
import tachiyomi.domain.source.manga.model.StubMangaSource
import tachiyomi.source.local.entries.manga.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

fun MangaSource.icon(): Drawable? = Injekt.get<MangaExtensionManager>().getAppIconForSource(this.id)

fun MangaSource.getPreferenceKey(): String = "source_$id"

fun MangaSource.toSourceData(): MangaSourceData = MangaSourceData(id = id, lang = lang, name = name)

fun MangaSource.getNameForMangaInfo(): String {
    val preferences = Injekt.get<SourcePreferences>()
    val enabledLanguages = preferences.enabledLanguages().get()
        .filterNot { it in listOf("all", "other") }
    val hasOneActiveLanguages = enabledLanguages.size == 1
    val isInEnabledLanguages = lang in enabledLanguages
    return when {
        // For edge cases where user disables a source they got manga of in their library.
        hasOneActiveLanguages && !isInEnabledLanguages -> toString()
        // Hide the language tag when only one language is used.
        hasOneActiveLanguages && isInEnabledLanguages -> name
        else -> toString()
    }
}

fun MangaSource.isLocalOrStub(): Boolean = isLocal() || this is StubMangaSource
