package eu.kanade.tachiyomi.data.backup.restore.restorers

import android.content.Context
import android.content.Intent
import eu.kanade.tachiyomi.data.backup.models.BackupExtension
import eu.kanade.tachiyomi.util.storage.getUriCompat
import uy.kohesive.injekt.api.get
import java.io.File

class ExtensionsRestorer(
    private val context: Context,
) {

    fun restoreExtensions(extensions: List<BackupExtension>) {
        extensions.forEach {
            if (context.packageManager.getInstalledPackages(0).none { pkg -> pkg.packageName == it.pkgName }) {
                // save apk in files dir and open installer dialog
                val file = File(context.cacheDir, "${it.pkgName}.apk")
                file.writeBytes(it.apk)
                val intent = Intent(Intent.ACTION_VIEW)
                    .setDataAndType(
                        file.getUriCompat(context),
                        "application/vnd.android.package-archive",
                    )
                    .setFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                context.startActivity(intent)
            }
        }
    }
}
