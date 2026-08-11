package eu.kanade.tachiyomi.data.backup.create.creators

import eu.kanade.tachiyomi.data.backup.models.BackupExtensionStore
import eu.kanade.tachiyomi.data.backup.models.backupAnimeExtensionStoreMapper
import mihon.domain.extension.anime.interactor.GetAnimeExtensionStores
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeExtensionStoresBackupCreator(
    private val getExtensionStores: GetAnimeExtensionStores = Injekt.get(),
) {

    suspend operator fun invoke(): List<BackupExtensionStore> {
        return getExtensionStores.get()
            .map(backupAnimeExtensionStoreMapper)
    }
}
