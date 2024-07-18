package eu.kanade.domain

import android.app.Application
import eu.kanade.domain.source.manga.interactor.ToggleExcludeFromMangaDataSaver
import tachiyomi.data.entries.anime.CustomAnimeRepositoryImpl
import tachiyomi.data.entries.manga.CustomMangaRepositoryImpl
import tachiyomi.domain.entries.anime.interactor.GetCustomAnimeInfo
import tachiyomi.domain.entries.anime.interactor.SetCustomAnimeInfo
import tachiyomi.domain.entries.anime.repository.CustomAnimeRepository
import tachiyomi.domain.entries.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.entries.manga.interactor.SetCustomMangaInfo
import tachiyomi.domain.entries.manga.repository.CustomMangaRepository
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addFactory
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

class SYDomainModule : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        addFactory { ToggleExcludeFromMangaDataSaver(get()) }

        addSingletonFactory<CustomMangaRepository> { CustomMangaRepositoryImpl(get<Application>()) }
        addFactory { GetCustomMangaInfo(get()) }
        addFactory { SetCustomMangaInfo(get()) }

        addSingletonFactory<CustomAnimeRepository> { CustomAnimeRepositoryImpl(get<Application>()) }
        addFactory { GetCustomAnimeInfo(get()) }
        addFactory { SetCustomAnimeInfo(get()) }
    }
}
