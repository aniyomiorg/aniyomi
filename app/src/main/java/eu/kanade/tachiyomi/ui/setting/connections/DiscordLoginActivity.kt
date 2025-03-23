// AM (DISCORD) -->

// Original library from https://github.com/dead8309/KizzyRPC (Thank you)
// Thank you to the 最高 man for the refactored and simplified code
// https://github.com/saikou-app/saikou
package eu.kanade.tachiyomi.ui.setting.connections

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import eu.kanade.domain.connections.service.ConnectionsPreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connections.ConnectionsManager
import eu.kanade.tachiyomi.data.connections.discord.DiscordAccount
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.util.system.toast
import tachiyomi.i18n.MR
import uy.kohesive.injekt.injectLazy
import java.io.File

class DiscordLoginActivity : BaseActivity() {

    private val connectionsManager: ConnectionsManager by injectLazy()
    private val connectionsPreferences: ConnectionsPreferences by injectLazy()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.discord_login_activity)
        val webView = findViewById<WebView>(R.id.webview)

        webView.apply {
            settings.javaScriptEnabled = true
            settings.databaseEnabled = true
            settings.domStorageEnabled = true
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                if (url != null && url.endsWith("/app")) {
                    webView.stopLoading()
                    webView.evaluateJavascript(
                        """
                        (function() {
                            const wreq = (webpackChunkdiscord_app.push([[''], {}, e => { m = []; for (let c in e.c) m.push(e.c[c])}]), m)
                            webpackChunkdiscord_app.pop()
                            const token = wreq.find(m => m?.exports?.default?.getToken !== void 0).exports.default.getToken();
                            return token;
                        })()
                        """.trimIndent(),
                    ) {
                        login(it.trim('"'))
                    }
                }
            }
        }
        webView.loadUrl("https://discord.com/login")
    }

    private fun login(token: String) {
        Thread {
            try {
                val response = okhttp3.OkHttpClient().newCall(
                    okhttp3.Request.Builder()
                        .url("https://discord.com/api/v10/users/@me")
                        .addHeader("Authorization", token)
                        .build(),
                ).execute()

                if (response.isSuccessful) {
                    val body = response.body.string()
                    val jsonObject = org.json.JSONObject(body!!)
                    val id = jsonObject.getString("id")
                    val username = jsonObject.getString("username")
                    val avatarId = jsonObject.optString("avatar")
                    val avatarUrl = if (avatarId.isNotEmpty()) {
                        "https://cdn.discordapp.com/avatars/$id/$avatarId.png"
                    } else {
                        null
                    }

                    val account = DiscordAccount(
                        id = id,
                        username = username,
                        avatarUrl = avatarUrl,
                        token = token,
                        isActive = true,
                    )
                    connectionsManager.discord.addAccount(account)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()

        connectionsPreferences.connectionsToken(connectionsManager.discord).set(token)
        connectionsPreferences.setConnectionsCredentials(
            connectionsManager.discord,
            "Discord",
            "Logged In",
        )
        toast(MR.strings.login_success)
        applicationInfo.dataDir.let { File("$it/app_webview/").deleteRecursively() }
        setResult(RESULT_OK)
        finish()
    }
}
// <-- AM (DISCORD)
