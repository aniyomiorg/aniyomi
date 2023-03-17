package eu.kanade.domain

import eu.kanade.data.category.anime.AnimeCategoryRepositoryImpl
import eu.kanade.data.category.manga.MangaCategoryRepositoryImpl
import eu.kanade.data.entries.anime.AnimeRepositoryImpl
import eu.kanade.data.entries.manga.MangaRepositoryImpl
import eu.kanade.data.history.anime.AnimeHistoryRepositoryImpl
import eu.kanade.data.history.manga.MangaHistoryRepositoryImpl
import eu.kanade.data.items.chapter.ChapterRepositoryImpl
import eu.kanade.data.items.episode.EpisodeRepositoryImpl
import eu.kanade.data.source.anime.AnimeSourceDataRepositoryImpl
import eu.kanade.data.source.anime.AnimeSourceRepositoryImpl
import eu.kanade.data.source.manga.MangaSourceDataRepositoryImpl
import eu.kanade.data.source.manga.MangaSourceRepositoryImpl
import eu.kanade.data.track.anime.AnimeTrackRepositoryImpl
import eu.kanade.data.track.manga.MangaTrackRepositoryImpl
import eu.kanade.data.updates.anime.AnimeUpdatesRepositoryImpl
import eu.kanade.data.updates.manga.MangaUpdatesRepositoryImpl
import eu.kanade.domain.category.anime.interactor.CreateAnimeCategoryWithName
import eu.kanade.domain.category.anime.interactor.DeleteAnimeCategory
import eu.kanade.domain.category.anime.interactor.GetAnimeCategories
import eu.kanade.domain.category.anime.interactor.RenameAnimeCategory
import eu.kanade.domain.category.anime.interactor.ReorderAnimeCategory
import eu.kanade.domain.category.anime.interactor.ResetAnimeCategoryFlags
import eu.kanade.domain.category.anime.interactor.SetAnimeCategories
import eu.kanade.domain.category.anime.interactor.SetDisplayModeForAnimeCategory
import eu.kanade.domain.category.anime.interactor.SetSortModeForAnimeCategory
import eu.kanade.domain.category.anime.interactor.UpdateAnimeCategory
import eu.kanade.domain.category.anime.repository.AnimeCategoryRepository
import eu.kanade.domain.category.manga.interactor.CreateMangaCategoryWithName
import eu.kanade.domain.category.manga.interactor.DeleteMangaCategory
import eu.kanade.domain.category.manga.interactor.GetMangaCategories
import eu.kanade.domain.category.manga.interactor.RenameMangaCategory
import eu.kanade.domain.category.manga.interactor.ReorderMangaCategory
import eu.kanade.domain.category.manga.interactor.ResetMangaCategoryFlags
import eu.kanade.domain.category.manga.interactor.SetDisplayModeForMangaCategory
import eu.kanade.domain.category.manga.interactor.SetMangaCategories
import eu.kanade.domain.category.manga.interactor.SetSortModeForMangaCategory
import eu.kanade.domain.category.manga.interactor.UpdateMangaCategory
import eu.kanade.domain.category.manga.repository.MangaCategoryRepository
import eu.kanade.domain.download.anime.interactor.DeleteAnimeDownload
import eu.kanade.domain.download.manga.interactor.DeleteChapterDownload
import eu.kanade.domain.entries.anime.interactor.GetAnime
import eu.kanade.domain.entries.anime.interactor.GetAnimeFavorites
import eu.kanade.domain.entries.anime.interactor.GetAnimeWithEpisodes
import eu.kanade.domain.entries.anime.interactor.GetDuplicateLibraryAnime
import eu.kanade.domain.entries.anime.interactor.GetLibraryAnime
import eu.kanade.domain.entries.anime.interactor.NetworkToLocalAnime
import eu.kanade.domain.entries.anime.interactor.ResetAnimeViewerFlags
import eu.kanade.domain.entries.anime.interactor.SetAnimeEpisodeFlags
import eu.kanade.domain.entries.anime.interactor.SetAnimeViewerFlags
import eu.kanade.domain.entries.anime.interactor.UpdateAnime
import eu.kanade.domain.entries.anime.repository.AnimeRepository
import eu.kanade.domain.entries.manga.interactor.GetDuplicateLibraryManga
import eu.kanade.domain.entries.manga.interactor.GetLibraryManga
import eu.kanade.domain.entries.manga.interactor.GetManga
import eu.kanade.domain.entries.manga.interactor.GetMangaFavorites
import eu.kanade.domain.entries.manga.interactor.GetMangaWithChapters
import eu.kanade.domain.entries.manga.interactor.NetworkToLocalManga
import eu.kanade.domain.entries.manga.interactor.ResetMangaViewerFlags
import eu.kanade.domain.entries.manga.interactor.SetMangaChapterFlags
import eu.kanade.domain.entries.manga.interactor.SetMangaViewerFlags
import eu.kanade.domain.entries.manga.interactor.UpdateManga
import eu.kanade.domain.entries.manga.repository.MangaRepository
import eu.kanade.domain.extension.anime.interactor.GetAnimeExtensionLanguages
import eu.kanade.domain.extension.anime.interactor.GetAnimeExtensionSources
import eu.kanade.domain.extension.anime.interactor.GetAnimeExtensionsByType
import eu.kanade.domain.extension.manga.interactor.GetExtensionSources
import eu.kanade.domain.extension.manga.interactor.GetMangaExtensionLanguages
import eu.kanade.domain.extension.manga.interactor.GetMangaExtensionsByType
import eu.kanade.domain.history.anime.interactor.GetAnimeHistory
import eu.kanade.domain.history.anime.interactor.GetNextEpisodes
import eu.kanade.domain.history.anime.interactor.RemoveAnimeHistory
import eu.kanade.domain.history.anime.interactor.UpsertAnimeHistory
import eu.kanade.domain.history.anime.repository.AnimeHistoryRepository
import eu.kanade.domain.history.manga.interactor.GetMangaHistory
import eu.kanade.domain.history.manga.interactor.GetNextChapters
import eu.kanade.domain.history.manga.interactor.GetTotalReadDuration
import eu.kanade.domain.history.manga.interactor.RemoveMangaHistory
import eu.kanade.domain.history.manga.interactor.UpsertMangaHistory
import eu.kanade.domain.history.manga.repository.MangaHistoryRepository
import eu.kanade.domain.items.chapter.interactor.SetReadStatus
import eu.kanade.domain.items.chapter.interactor.ShouldUpdateDbChapter
import eu.kanade.domain.items.chapter.interactor.SyncChaptersWithSource
import eu.kanade.domain.items.chapter.interactor.SyncChaptersWithTrackServiceTwoWay
import eu.kanade.domain.items.chapter.interactor.UpdateChapter
import eu.kanade.domain.items.chapter.repository.ChapterRepository
import eu.kanade.domain.items.episode.interactor.GetEpisode
import eu.kanade.domain.items.episode.interactor.GetEpisodeByAnimeId
import eu.kanade.domain.items.episode.interactor.SetAnimeDefaultEpisodeFlags
import eu.kanade.domain.items.episode.interactor.SetSeenStatus
import eu.kanade.domain.items.episode.interactor.ShouldUpdateDbEpisode
import eu.kanade.domain.items.episode.interactor.SyncEpisodesWithSource
import eu.kanade.domain.items.episode.interactor.SyncEpisodesWithTrackServiceTwoWay
import eu.kanade.domain.items.episode.interactor.UpdateEpisode
import eu.kanade.domain.items.episode.repository.EpisodeRepository
import eu.kanade.domain.source.anime.interactor.GetAnimeSourcesWithFavoriteCount
import eu.kanade.domain.source.anime.interactor.GetAnimeSourcesWithNonLibraryAnime
import eu.kanade.domain.source.anime.interactor.GetEnabledAnimeSources
import eu.kanade.domain.source.anime.interactor.GetLanguagesWithAnimeSources
import eu.kanade.domain.source.anime.interactor.GetRemoteAnime
import eu.kanade.domain.source.anime.interactor.ToggleAnimeSource
import eu.kanade.domain.source.anime.interactor.ToggleAnimeSourcePin
import eu.kanade.domain.source.anime.repository.AnimeSourceDataRepository
import eu.kanade.domain.source.anime.repository.AnimeSourceRepository
import eu.kanade.domain.source.manga.interactor.GetEnabledMangaSources
import eu.kanade.domain.source.manga.interactor.GetLanguagesWithMangaSources
import eu.kanade.domain.source.manga.interactor.GetMangaSourcesWithFavoriteCount
import eu.kanade.domain.source.manga.interactor.GetMangaSourcesWithNonLibraryManga
import eu.kanade.domain.source.manga.interactor.GetRemoteManga
import eu.kanade.domain.source.manga.interactor.ToggleMangaSource
import eu.kanade.domain.source.manga.interactor.ToggleMangaSourcePin
import eu.kanade.domain.source.manga.repository.MangaSourceDataRepository
import eu.kanade.domain.source.manga.repository.MangaSourceRepository
import eu.kanade.domain.source.service.SetMigrateSorting
import eu.kanade.domain.source.service.ToggleLanguage
import eu.kanade.domain.track.anime.interactor.DeleteAnimeTrack
import eu.kanade.domain.track.anime.interactor.GetAnimeTracks
import eu.kanade.domain.track.anime.interactor.GetTracksPerAnime
import eu.kanade.domain.track.anime.interactor.InsertAnimeTrack
import eu.kanade.domain.track.anime.repository.AnimeTrackRepository
import eu.kanade.domain.track.manga.interactor.DeleteMangaTrack
import eu.kanade.domain.track.manga.interactor.GetMangaTracks
import eu.kanade.domain.track.manga.interactor.GetTracksPerManga
import eu.kanade.domain.track.manga.interactor.InsertMangaTrack
import eu.kanade.domain.track.manga.repository.MangaTrackRepository
import eu.kanade.domain.updates.anime.interactor.GetAnimeUpdates
import eu.kanade.domain.updates.anime.repository.AnimeUpdatesRepository
import eu.kanade.domain.updates.manga.interactor.GetMangaUpdates
import eu.kanade.domain.updates.manga.repository.MangaUpdatesRepository
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addFactory
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

class DomainModule : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        addSingletonFactory<AnimeCategoryRepository> { AnimeCategoryRepositoryImpl(get()) }
        addFactory { GetAnimeCategories(get()) }
        addFactory { ResetAnimeCategoryFlags(get(), get()) }
        addFactory { SetDisplayModeForAnimeCategory(get(), get()) }
        addFactory { SetSortModeForAnimeCategory(get(), get()) }
        addFactory { CreateAnimeCategoryWithName(get(), get()) }
        addFactory { RenameAnimeCategory(get()) }
        addFactory { ReorderAnimeCategory(get()) }
        addFactory { UpdateAnimeCategory(get()) }
        addFactory { DeleteAnimeCategory(get()) }

        addSingletonFactory<MangaCategoryRepository> { MangaCategoryRepositoryImpl(get()) }
        addFactory { GetMangaCategories(get()) }
        addFactory { ResetMangaCategoryFlags(get(), get()) }
        addFactory { SetDisplayModeForMangaCategory(get(), get()) }
        addFactory { SetSortModeForMangaCategory(get(), get()) }
        addFactory { CreateMangaCategoryWithName(get(), get()) }
        addFactory { RenameMangaCategory(get()) }
        addFactory { ReorderMangaCategory(get()) }
        addFactory { UpdateMangaCategory(get()) }
        addFactory { DeleteMangaCategory(get()) }

        addSingletonFactory<AnimeRepository> { AnimeRepositoryImpl(get()) }
        addFactory { GetDuplicateLibraryAnime(get()) }
        addFactory { GetAnimeFavorites(get()) }
        addFactory { GetLibraryAnime(get()) }
        addFactory { GetAnimeWithEpisodes(get(), get()) }
        addFactory { GetAnime(get()) }
        addFactory { GetNextEpisodes(get(), get(), get()) }
        addFactory { ResetAnimeViewerFlags(get()) }
        addFactory { SetAnimeEpisodeFlags(get()) }
        addFactory { SetAnimeDefaultEpisodeFlags(get(), get(), get()) }
        addFactory { SetAnimeViewerFlags(get()) }
        addFactory { NetworkToLocalAnime(get()) }
        addFactory { UpdateAnime(get()) }
        addFactory { SetAnimeCategories(get()) }

        addSingletonFactory<MangaRepository> { MangaRepositoryImpl(get()) }
        addFactory { GetDuplicateLibraryManga(get()) }
        addFactory { GetMangaFavorites(get()) }
        addFactory { GetLibraryManga(get()) }
        addFactory { GetMangaWithChapters(get(), get()) }
        addFactory { GetManga(get()) }
        addFactory { GetNextChapters(get(), get(), get()) }
        addFactory { ResetMangaViewerFlags(get()) }
        addFactory { SetMangaChapterFlags(get()) }
        addFactory {
            eu.kanade.domain.items.chapter.interactor.SetMangaDefaultChapterFlags(
                get(),
                get(),
                get(),
            )
        }
        addFactory { SetMangaViewerFlags(get()) }
        addFactory { NetworkToLocalManga(get()) }
        addFactory { UpdateManga(get()) }
        addFactory { SetMangaCategories(get()) }

        addSingletonFactory<AnimeTrackRepository> { AnimeTrackRepositoryImpl(get()) }
        addFactory { DeleteAnimeTrack(get()) }
        addFactory { GetTracksPerAnime(get()) }
        addFactory { GetAnimeTracks(get()) }
        addFactory { InsertAnimeTrack(get()) }

        addSingletonFactory<MangaTrackRepository> { MangaTrackRepositoryImpl(get()) }
        addFactory { DeleteMangaTrack(get()) }
        addFactory { GetTracksPerManga(get()) }
        addFactory { GetMangaTracks(get()) }
        addFactory { InsertMangaTrack(get()) }

        addSingletonFactory<EpisodeRepository> { EpisodeRepositoryImpl(get()) }
        addFactory { GetEpisode(get()) }
        addFactory { GetEpisodeByAnimeId(get()) }
        addFactory { UpdateEpisode(get()) }
        addFactory { SetSeenStatus(get(), get(), get(), get()) }
        addFactory { ShouldUpdateDbEpisode() }
        addFactory { SyncEpisodesWithSource(get(), get(), get(), get()) }
        addFactory { SyncEpisodesWithTrackServiceTwoWay(get(), get()) }

        addSingletonFactory<ChapterRepository> { ChapterRepositoryImpl(get()) }
        addFactory { eu.kanade.domain.items.chapter.interactor.GetChapter(get()) }
        addFactory { eu.kanade.domain.items.chapter.interactor.GetChapterByMangaId(get()) }
        addFactory { UpdateChapter(get()) }
        addFactory { SetReadStatus(get(), get(), get(), get()) }
        addFactory { ShouldUpdateDbChapter() }
        addFactory { SyncChaptersWithSource(get(), get(), get(), get()) }
        addFactory { SyncChaptersWithTrackServiceTwoWay(get(), get()) }

        addSingletonFactory<AnimeHistoryRepository> { AnimeHistoryRepositoryImpl(get()) }
        addFactory { GetAnimeHistory(get()) }
        addFactory { UpsertAnimeHistory(get()) }
        addFactory { RemoveAnimeHistory(get()) }

        addFactory { DeleteAnimeDownload(get(), get()) }

        addFactory { GetAnimeExtensionsByType(get(), get()) }
        addFactory { GetAnimeExtensionSources(get()) }
        addFactory { GetAnimeExtensionLanguages(get(), get()) }

        addSingletonFactory<MangaHistoryRepository> { MangaHistoryRepositoryImpl(get()) }
        addFactory { GetMangaHistory(get()) }
        addFactory { UpsertMangaHistory(get()) }
        addFactory { RemoveMangaHistory(get()) }
        addFactory { GetTotalReadDuration(get()) }

        addFactory { DeleteChapterDownload(get(), get()) }

        addFactory { GetMangaExtensionsByType(get(), get()) }
        addFactory { GetExtensionSources(get()) }
        addFactory { GetMangaExtensionLanguages(get(), get()) }

        addSingletonFactory<AnimeUpdatesRepository> { AnimeUpdatesRepositoryImpl(get()) }
        addFactory { GetAnimeUpdates(get()) }

        addSingletonFactory<MangaUpdatesRepository> { MangaUpdatesRepositoryImpl(get()) }
        addFactory { GetMangaUpdates(get()) }

        addSingletonFactory<AnimeSourceRepository> { AnimeSourceRepositoryImpl(get(), get()) }
        addSingletonFactory<AnimeSourceDataRepository> { AnimeSourceDataRepositoryImpl(get()) }
        addFactory { GetEnabledAnimeSources(get(), get()) }
        addFactory { GetLanguagesWithAnimeSources(get(), get()) }
        addFactory { GetRemoteAnime(get()) }
        addFactory { GetAnimeSourcesWithFavoriteCount(get(), get()) }
        addFactory { GetAnimeSourcesWithNonLibraryAnime(get()) }
        addFactory { ToggleAnimeSource(get()) }
        addFactory { ToggleAnimeSourcePin(get()) }

        addSingletonFactory<MangaSourceRepository> { MangaSourceRepositoryImpl(get(), get()) }
        addSingletonFactory<MangaSourceDataRepository> { MangaSourceDataRepositoryImpl(get()) }
        addFactory { GetEnabledMangaSources(get(), get()) }
        addFactory { GetLanguagesWithMangaSources(get(), get()) }
        addFactory { GetRemoteManga(get()) }
        addFactory { GetMangaSourcesWithFavoriteCount(get(), get()) }
        addFactory { GetMangaSourcesWithNonLibraryManga(get()) }
        addFactory { SetMigrateSorting(get()) }
        addFactory { ToggleLanguage(get()) }
        addFactory { ToggleMangaSource(get()) }
        addFactory { ToggleMangaSourcePin(get()) }
    }
}
