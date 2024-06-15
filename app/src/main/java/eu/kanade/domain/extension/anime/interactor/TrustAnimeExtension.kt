package eu.kanade.domain.extension.anime.interactor

import android.content.pm.PackageInfo
import androidx.core.content.pm.PackageInfoCompat
import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.core.preference.getAndSet

class TrustAnimeExtension(
    private val preferences: SourcePreferences,
) {

    fun isTrusted(pkgInfo: PackageInfo, signatureHash: String): Boolean {
        val key = "${pkgInfo.packageName}:${PackageInfoCompat.getLongVersionCode(pkgInfo)}:$signatureHash"
        return key in preferences.trustedExtensions().get() ||
            signatureHash == officialSignature || signatureHash == AnyomiSignature
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

// jmir1's key
private const val officialSignature = "145e350c873d4ec438790ee24272db148a65057941c25391515ac8194f7d29c9"
private const val AnyomiSignature = "50ab1d1e3a20d204d0ad6d334c7691c632e41b98dfa132bf385695fdfa63839c"
