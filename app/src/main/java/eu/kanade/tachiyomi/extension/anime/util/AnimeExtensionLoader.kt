package eu.kanade.tachiyomi.extension.anime.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import dalvik.system.PathClassLoader
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceFactory
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import eu.kanade.tachiyomi.extension.anime.model.AnimeLoadResult
import eu.kanade.tachiyomi.util.lang.Hash
import eu.kanade.tachiyomi.util.system.getApplicationIcon
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import logcat.LogPriority
import tachiyomi.core.util.system.logcat
import uy.kohesive.injekt.injectLazy

/**
 * Class that handles the loading of the extensions installed in the system.
 */
@SuppressLint("PackageManagerGetSignatures")
internal object AnimeExtensionLoader {

    private val preferences: SourcePreferences by injectLazy()
    private val loadNsfwSource by lazy {
        preferences.showNsfwSource().get()
    }

    private const val EXTENSION_FEATURE = "tachiyomi.animeextension"
    private const val METADATA_SOURCE_CLASS = "tachiyomi.animeextension.class"
    private const val METADATA_SOURCE_FACTORY = "tachiyomi.animeextension.factory"
    private const val METADATA_NSFW = "tachiyomi.animeextension.nsfw"
    private const val METADATA_HAS_README = "tachiyomi.animeextension.hasReadme"
    private const val METADATA_HAS_CHANGELOG = "tachiyomi.animeextension.hasChangelog"
    const val LIB_VERSION_MIN = 12
    const val LIB_VERSION_MAX = 15

    private const val PACKAGE_FLAGS = PackageManager.GET_CONFIGURATIONS or PackageManager.GET_SIGNATURES

    // jmir1's key
    private const val officialSignature = "50ab1d1e3a20d204d0ad6d334c7691c632e41b98dfa132bf385695fdfa63839c"

    /**
     * List of the trusted signatures.
     */
    var trustedSignatures = mutableSetOf<String>() + preferences.trustedSignatures().get() + officialSignature

    /**
     * Return a list of all the installed extensions initialized concurrently.
     *
     * @param context The application context.
     */
    fun loadExtensions(context: Context): List<AnimeLoadResult> {
        val pkgManager = context.packageManager

        @Suppress("DEPRECATION")
        val installedPkgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pkgManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PACKAGE_FLAGS.toLong()))
        } else {
            pkgManager.getInstalledPackages(PACKAGE_FLAGS)
        }

        val extPkgs = installedPkgs.filter { isPackageAnExtension(it) }

        if (extPkgs.isEmpty()) return emptyList()

        // Load each extension concurrently and wait for completion
        return runBlocking {
            val deferred = extPkgs.map {
                async { loadExtension(context, it.packageName, it) }
            }
            deferred.map { it.await() }
        }
    }

    /**
     * Attempts to load an extension from the given package name. It checks if the extension
     * contains the required feature flag before trying to load it.
     */
    fun loadExtensionFromPkgName(context: Context, pkgName: String): AnimeLoadResult {
        val pkgInfo = try {
            context.packageManager.getPackageInfo(pkgName, PACKAGE_FLAGS)
        } catch (error: PackageManager.NameNotFoundException) {
            // Unlikely, but the package may have been uninstalled at this point
            logcat(LogPriority.ERROR, error)
            return AnimeLoadResult.Error
        }
        if (!isPackageAnExtension(pkgInfo)) {
            logcat(LogPriority.WARN) { "Tried to load a package that wasn't a extension ($pkgName)" }
            return AnimeLoadResult.Error
        }
        return loadExtension(context, pkgName, pkgInfo)
    }

    /**
     * Loads an extension given its package name.
     *
     * @param context The application context.
     * @param pkgName The package name of the extension to load.
     * @param pkgInfo The package info of the extension.
     */
    private fun loadExtension(context: Context, pkgName: String, pkgInfo: PackageInfo): AnimeLoadResult {
        val pkgManager = context.packageManager

        val appInfo = try {
            pkgManager.getApplicationInfo(pkgName, PackageManager.GET_META_DATA)
        } catch (error: PackageManager.NameNotFoundException) {
            // Unlikely, but the package may have been uninstalled at this point
            logcat(LogPriority.ERROR, error)
            return AnimeLoadResult.Error
        }

        val extName = pkgManager.getApplicationLabel(appInfo).toString().substringAfter("Aniyomi: ")
        val versionName = pkgInfo.versionName
        val versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo)

        if (versionName.isNullOrEmpty()) {
            logcat(LogPriority.WARN) { "Missing versionName for extension $extName" }
            return AnimeLoadResult.Error
        }

        // Validate lib version
        val libVersion = versionName.substringBeforeLast('.').toDoubleOrNull()
        if (libVersion == null || libVersion < LIB_VERSION_MIN || libVersion > LIB_VERSION_MAX) {
            logcat(LogPriority.WARN) {
                "Lib version is $libVersion, while only versions " +
                    "$LIB_VERSION_MIN to $LIB_VERSION_MAX are allowed"
            }
            return AnimeLoadResult.Error
        }

        val signatureHash = getSignatureHash(pkgInfo)

        if (signatureHash == null) {
            logcat(LogPriority.WARN) { "Package $pkgName isn't signed" }
            return AnimeLoadResult.Error
        } else if (signatureHash !in trustedSignatures) {
            val extension = AnimeExtension.Untrusted(extName, pkgName, versionName, versionCode, libVersion, signatureHash)
            logcat(LogPriority.WARN, message = { "Extension $pkgName isn't trusted" })
            return AnimeLoadResult.Untrusted(extension)
        }

        val isNsfw = appInfo.metaData.getInt(METADATA_NSFW) == 1
        if (!loadNsfwSource && isNsfw) {
            logcat(LogPriority.WARN) { "NSFW extension $pkgName not allowed" }
            return AnimeLoadResult.Error
        }

        val hasReadme = appInfo.metaData.getInt(METADATA_HAS_README, 0) == 1
        val hasChangelog = appInfo.metaData.getInt(METADATA_HAS_CHANGELOG, 0) == 1

        val classLoader = PathClassLoader(appInfo.sourceDir, null, context.classLoader)

        val sources = appInfo.metaData.getString(METADATA_SOURCE_CLASS)!!
            .split(";")
            .map {
                val sourceClass = it.trim()
                if (sourceClass.startsWith(".")) {
                    pkgInfo.packageName + sourceClass
                } else {
                    sourceClass
                }
            }
            .flatMap {
                try {
                    when (val obj = Class.forName(it, false, classLoader).newInstance()) {
                        is AnimeSource -> listOf(obj)
                        is AnimeSourceFactory -> obj.createSources()
                        else -> throw Exception("Unknown source class type! ${obj.javaClass}")
                    }
                } catch (e: Throwable) {
                    logcat(LogPriority.ERROR, e) { "Extension load error: $extName ($it)" }
                    return AnimeLoadResult.Error
                }
            }

        val langs = sources.filterIsInstance<AnimeCatalogueSource>()
            .map { it.lang }
            .toSet()
        val lang = when (langs.size) {
            0 -> ""
            1 -> langs.first()
            else -> "all"
        }

        val extension = AnimeExtension.Installed(
            name = extName,
            pkgName = pkgName,
            versionName = versionName,
            versionCode = versionCode,
            libVersion = libVersion,
            lang = lang,
            isNsfw = isNsfw,
            hasReadme = hasReadme,
            hasChangelog = hasChangelog,
            sources = sources,
            pkgFactory = appInfo.metaData.getString(METADATA_SOURCE_FACTORY),
            isUnofficial = signatureHash != officialSignature,
            icon = context.getApplicationIcon(pkgName),
        )
        return AnimeLoadResult.Success(extension)
    }

    /**
     * Returns true if the given package is an extension.
     *
     * @param pkgInfo The package info of the application.
     */
    private fun isPackageAnExtension(pkgInfo: PackageInfo): Boolean {
        return pkgInfo.reqFeatures.orEmpty().any { it.name == EXTENSION_FEATURE }
    }

    /**
     * Returns the signature hash of the package or null if it's not signed.
     *
     * @param pkgInfo The package info of the application.
     */
    private fun getSignatureHash(pkgInfo: PackageInfo): String? {
        val signatures = pkgInfo.signatures
        return if (signatures != null && signatures.isNotEmpty()) {
            Hash.sha256(signatures.first().toByteArray())
        } else {
            null
        }
    }
}
