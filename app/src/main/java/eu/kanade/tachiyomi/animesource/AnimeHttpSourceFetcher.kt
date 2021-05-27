package eu.kanade.tachiyomi.animesource

import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import rx.Observable
import timber.log.Timber

fun AnimeHttpSource.getImageUrl(video: Video): Observable<Video> {
    Timber.w("getImageUrl")
    video.status = Video.LOAD_VIDEO
    return fetchVideoLink(video)
        .doOnError { video.status = Video.ERROR }
        .onErrorReturn { null }
        .doOnNext { video.videoUrl = it }
        .map { video }
}

fun AnimeHttpSource.fetchUrlFromVideo(video: Video): Observable<Video> {
    Timber.w("fetchUrlFromVideo")
    return Observable.just(video)
        .filter { !it.url.isEmpty() }
        .mergeWith(fetchRemainingImageUrlsFromPageList(video))
}

fun AnimeHttpSource.fetchRemainingImageUrlsFromPageList(video: Video): Observable<Video> {
    Timber.w("fetchRemainingImageUrlsFromPageList")
    return Observable.just(video)
        .filter { it.url.isEmpty() }
        .concatMap { getImageUrl(it) }
}
