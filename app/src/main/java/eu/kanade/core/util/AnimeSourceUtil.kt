package eu.kanade.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun ifAnimeSourcesLoaded(): Boolean {
    return remember { Injekt.get<AnimeSourceManager>().isInitialized }.collectAsState().value
}
