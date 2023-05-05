package eu.kanade.tachiyomi.ui.webview

import android.content.Context
import androidx.core.net.toUri
import cafe.adriel.voyager.core.model.StateScreenModel
import eu.kanade.presentation.more.stats.StatsScreenState
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.anime.AnimeSourceManager
import eu.kanade.tachiyomi.source.manga.MangaSourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.system.logcat
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast
import okhttp3.HttpUrl.Companion.toHttpUrl
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class WebViewScreenModel(
    val sourceId: Long?,
    private val MangaSourceManager: MangaSourceManager = Injekt.get(),
    private val AnimeSourceManager: AnimeSourceManager = Injekt.get(),
    private val network: NetworkHelper = Injekt.get(),
) : StateScreenModel<StatsScreenState>(StatsScreenState.Loading) {

    var headers = emptyMap<String, String>()

    init {
        sourceId?.let { MangaSourceManager.get(it) as? HttpSource }?.let { mangasource ->
            headers = mangasource.headers.toMultimap().mapValues { it.value.getOrNull(0) ?: "" }
        }
        sourceId?.let { AnimeSourceManager.get(it) as? AnimeHttpSource }?.let { animesource ->
            headers = animesource.headers.toMultimap().mapValues { it.value.getOrNull(0) ?: "" }
        }
    }

    fun shareWebpage(context: Context, url: String) {
        try {
            context.startActivity(url.toUri().toShareIntent(context, type = "text/plain"))
        } catch (e: Exception) {
            context.toast(e.message)
        }
    }

    fun openInBrowser(context: Context, url: String) {
        context.openInBrowser(url, forceDefaultBrowser = true)
    }

    fun clearCookies(url: String) {
        val cleared = network.cookieManager.remove(url.toHttpUrl())
        logcat { "Cleared $cleared cookies for: $url" }
    }
}
