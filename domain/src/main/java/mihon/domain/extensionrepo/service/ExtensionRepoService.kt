package mihon.domain.extensionrepo.service

import androidx.core.net.toUri
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.json.Json
import logcat.LogPriority
import mihon.domain.extensionrepo.model.ExtensionRepo
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat

class ExtensionRepoService(
    networkHelper: NetworkHelper,
    private val json: Json,
) {
    val client = networkHelper.client

    suspend fun fetchRepoDetails(
        repo: String,
    ): ExtensionRepo? {
        return withIOContext {
            val url = "$repo/repo.json".toUri()

            try {
                with(json) {
                    client.newCall(GET(url.toString()))
                        .awaitSuccess()
                        .parseAs<ExtensionRepoMetaDto>()
                        .toExtensionRepo(baseUrl = repo)
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Failed to fetch repo details" }
                null
            }
        }
    }
}
