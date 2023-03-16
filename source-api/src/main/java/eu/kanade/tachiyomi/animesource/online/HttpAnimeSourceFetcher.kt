package eu.kanade.tachiyomi.animesource.online

import eu.kanade.tachiyomi.animesource.model.Video
import rx.Observable

fun HttpAnimeSource.getVideoUrl(video: Video): Observable<Video> {
    video.status = Video.State.LOAD_VIDEO
    return fetchVideoUrl(video)
        .doOnError { video.status = Video.State.ERROR }
        .onErrorReturn { null }
        .doOnNext { video.videoUrl = it }
        .map { video }
}

fun HttpAnimeSource.fetchUrlFromVideo(video: Video): Observable<Video> {
    return Observable.just(video)
        .filter { !it.videoUrl.isNullOrEmpty() }
        .mergeWith(fetchRemainingVideoUrlsFromVideoList(video))
}

fun HttpAnimeSource.fetchRemainingVideoUrlsFromVideoList(video: Video): Observable<Video> {
    return Observable.just(video)
        .filter { it.videoUrl.isNullOrEmpty() }
        .concatMap { getVideoUrl(it) }
}
