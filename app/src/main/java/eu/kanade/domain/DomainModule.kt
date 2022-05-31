package eu.kanade.domain

import eu.kanade.data.anime.AnimeRepositoryImpl
import eu.kanade.data.animehistory.AnimeHistoryRepositoryImpl
import eu.kanade.data.animesource.AnimeSourceRepositoryImpl
import eu.kanade.data.chapter.ChapterRepositoryImpl
import eu.kanade.data.episode.EpisodeRepositoryImpl
import eu.kanade.data.history.HistoryRepositoryImpl
import eu.kanade.data.manga.MangaRepositoryImpl
import eu.kanade.data.source.SourceRepositoryImpl
import eu.kanade.domain.anime.repository.AnimeRepository
import eu.kanade.domain.animeextension.interactor.GetAnimeExtensionLanguages
import eu.kanade.domain.animeextension.interactor.GetAnimeExtensionSources
import eu.kanade.domain.animeextension.interactor.GetAnimeExtensionUpdates
import eu.kanade.domain.animeextension.interactor.GetAnimeExtensions
import eu.kanade.domain.animehistory.interactor.DeleteAnimeHistoryTable
import eu.kanade.domain.animehistory.interactor.GetAnimeHistory
import eu.kanade.domain.animehistory.interactor.GetNextEpisodeForAnime
import eu.kanade.domain.animehistory.interactor.RemoveAnimeHistoryByAnimeId
import eu.kanade.domain.animehistory.interactor.RemoveAnimeHistoryById
import eu.kanade.domain.animehistory.interactor.UpsertAnimeHistory
import eu.kanade.domain.animehistory.repository.AnimeHistoryRepository
import eu.kanade.domain.animesource.interactor.GetAnimeSourcesWithFavoriteCount
import eu.kanade.domain.animesource.interactor.GetEnabledAnimeSources
import eu.kanade.domain.animesource.interactor.GetLanguagesWithAnimeSources
import eu.kanade.domain.animesource.interactor.ToggleAnimeSource
import eu.kanade.domain.animesource.interactor.ToggleAnimeSourcePin
import eu.kanade.domain.animesource.repository.AnimeSourceRepository
import eu.kanade.domain.chapter.interactor.UpdateChapter
import eu.kanade.domain.chapter.repository.ChapterRepository
import eu.kanade.domain.episode.interactor.UpdateEpisode
import eu.kanade.domain.episode.repository.EpisodeRepository
import eu.kanade.domain.extension.interactor.GetExtensionLanguages
import eu.kanade.domain.extension.interactor.GetExtensionSources
import eu.kanade.domain.extension.interactor.GetExtensionUpdates
import eu.kanade.domain.extension.interactor.GetExtensions
import eu.kanade.domain.history.interactor.DeleteHistoryTable
import eu.kanade.domain.history.interactor.GetHistory
import eu.kanade.domain.history.interactor.GetNextChapterForManga
import eu.kanade.domain.history.interactor.RemoveHistoryById
import eu.kanade.domain.history.interactor.RemoveHistoryByMangaId
import eu.kanade.domain.history.interactor.UpsertHistory
import eu.kanade.domain.history.repository.HistoryRepository
import eu.kanade.domain.manga.interactor.GetFavoritesBySourceId
import eu.kanade.domain.manga.interactor.ResetViewerFlags
import eu.kanade.domain.manga.repository.MangaRepository
import eu.kanade.domain.source.interactor.GetEnabledSources
import eu.kanade.domain.source.interactor.GetLanguagesWithSources
import eu.kanade.domain.source.interactor.GetSourcesWithFavoriteCount
import eu.kanade.domain.source.interactor.SetMigrateSorting
import eu.kanade.domain.source.interactor.ToggleLanguage
import eu.kanade.domain.source.interactor.ToggleSource
import eu.kanade.domain.source.interactor.ToggleSourcePin
import eu.kanade.domain.source.repository.SourceRepository
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addFactory
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get
import eu.kanade.domain.anime.interactor.GetFavoritesBySourceId as GetFavoritesBySourceIdAnime
import eu.kanade.domain.anime.interactor.ResetViewerFlags as ResetViewerFlagsAnime

class DomainModule : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        addSingletonFactory<AnimeRepository> { AnimeRepositoryImpl(get()) }
        addFactory { GetFavoritesBySourceIdAnime(get()) }
        addFactory { GetNextEpisodeForAnime(get()) }
        addFactory { ResetViewerFlagsAnime(get()) }

        addSingletonFactory<EpisodeRepository> { EpisodeRepositoryImpl(get()) }
        addFactory { UpdateEpisode(get()) }

        addSingletonFactory<MangaRepository> { MangaRepositoryImpl(get()) }
        addFactory { GetFavoritesBySourceId(get()) }
        addFactory { GetNextChapterForManga(get()) }
        addFactory { ResetViewerFlags(get()) }

        addSingletonFactory<ChapterRepository> { ChapterRepositoryImpl(get()) }
        addFactory { UpdateChapter(get()) }

        addSingletonFactory<AnimeHistoryRepository> { AnimeHistoryRepositoryImpl(get()) }
        addFactory { DeleteAnimeHistoryTable(get()) }
        addFactory { GetAnimeHistory(get()) }
        addFactory { UpsertAnimeHistory(get()) }
        addFactory { RemoveAnimeHistoryById(get()) }
        addFactory { RemoveAnimeHistoryByAnimeId(get()) }

        addSingletonFactory<HistoryRepository> { HistoryRepositoryImpl(get()) }
        addFactory { DeleteHistoryTable(get()) }
        addFactory { GetHistory(get()) }
        addFactory { UpsertHistory(get()) }
        addFactory { RemoveHistoryById(get()) }
        addFactory { RemoveHistoryByMangaId(get()) }

        addSingletonFactory<AnimeSourceRepository> { AnimeSourceRepositoryImpl(get(), get()) }
        addFactory { GetLanguagesWithAnimeSources(get(), get()) }
        addFactory { GetEnabledAnimeSources(get(), get()) }
        addFactory { ToggleAnimeSource(get()) }
        addFactory { ToggleAnimeSourcePin(get()) }
        addFactory { GetAnimeSourcesWithFavoriteCount(get(), get()) }

        addFactory { GetAnimeExtensions(get(), get()) }
        addFactory { GetAnimeExtensionSources(get()) }
        addFactory { GetAnimeExtensionUpdates(get(), get()) }
        addFactory { GetAnimeExtensionLanguages(get(), get()) }

        addFactory { GetExtensions(get(), get()) }
        addFactory { GetExtensionSources(get()) }
        addFactory { GetExtensionUpdates(get(), get()) }
        addFactory { GetExtensionLanguages(get(), get()) }

        addSingletonFactory<SourceRepository> { SourceRepositoryImpl(get(), get()) }
        addFactory { GetLanguagesWithSources(get(), get()) }
        addFactory { GetEnabledSources(get(), get()) }
        addFactory { ToggleSource(get()) }
        addFactory { ToggleSourcePin(get()) }
        addFactory { GetSourcesWithFavoriteCount(get(), get()) }
        addFactory { SetMigrateSorting(get()) }
        addFactory { ToggleLanguage(get()) }
    }
}
