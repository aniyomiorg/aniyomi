package eu.kanade.domain.extension.manga.interactor

import android.content.pm.PackageInfo
import androidx.core.content.pm.PackageInfoCompat
import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.core.common.preference.getAndSet

class TrustMangaExtension(
    private val preferences: SourcePreferences,
) {

    fun isTrusted(pkgInfo: PackageInfo, signatureHash: String): Boolean {
        val key = "${pkgInfo.packageName}:${PackageInfoCompat.getLongVersionCode(pkgInfo)}:$signatureHash"
        return key in preferences.trustedExtensions().get() ||
            signatureHash == officialSignature
    }

    fun trust(pkgName: String, versionCode: Long, signatureHash: String) {
        preferences.trustedExtensions().getAndSet { exts ->
            // Remove previously trusted versions
            val removed = exts.filterNot { it.startsWith("$pkgName:") }.toMutableSet()

            removed.also {
                it += "$pkgName:$versionCode:$signatureHash"
            }
        }
    }

    fun revokeAll() {
        preferences.trustedExtensions().delete()
    }
}

// inorichi's key
private const val officialSignature = "7ce04da7773d41b489f4693a366c36bcd0a11fc39b547168553c285bd7348e23"
