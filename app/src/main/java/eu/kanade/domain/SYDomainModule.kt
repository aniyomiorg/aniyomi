package eu.kanade.domain

import eu.kanade.domain.source.manga.interactor.ToggleExcludeFromMangaDataSaver
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addFactory
import uy.kohesive.injekt.api.get

class SYDomainModule : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        addFactory { ToggleExcludeFromMangaDataSaver(get()) }
    }
}
