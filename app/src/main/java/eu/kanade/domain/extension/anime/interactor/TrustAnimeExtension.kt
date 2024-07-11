package eu.kanade.domain.extension.anime.interactor

import android.content.pm.PackageInfo
import androidx.core.content.pm.PackageInfoCompat
import eu.kanade.domain.source.service.SourcePreferences
import mihon.domain.extensionrepo.anime.repository.AnimeExtensionRepoRepository
import tachiyomi.core.common.preference.getAndSet

class TrustAnimeExtension(
    private val animeExtensionRepoRepository: AnimeExtensionRepoRepository,
    private val preferences: SourcePreferences,
) {

    suspend fun isTrusted(pkgInfo: PackageInfo, fingerprints: List<String>): Boolean {
        val trustedFingerprints = animeExtensionRepoRepository.getAll().map { it.signingKeyFingerprint }.toHashSet()
        val key = "${pkgInfo.packageName}:${PackageInfoCompat.getLongVersionCode(pkgInfo)}:${fingerprints.last()}"
        return trustedFingerprints.any { fingerprints.contains(it) } || key in preferences.trustedExtensions().get()
    }

    fun trust(pkgName: String, versionCode: Long, signatureHash: String) {
        preferences.trustedExtensions().getAndSet { exts ->
            // Remove previously trusted versions
            val removed = exts.filterNot { it.startsWith("$pkgName:") }.toMutableSet()

            removed.also { it += "$pkgName:$versionCode:$signatureHash" }
        }
    }

    fun revokeAll() {
        preferences.trustedExtensions().delete()
    }
}

// jmir1's key
private const val officialSignature = "50ab1d1e3a20d204d0ad6d334c7691c632e41b98dfa132bf385695fdfa63839c"
