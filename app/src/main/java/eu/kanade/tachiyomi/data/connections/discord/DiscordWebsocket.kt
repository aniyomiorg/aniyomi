// AM (DISCORD) -->

// Original library from https://github.com/dead8309/KizzyRPC (Thank you)
// Thank you to the 最高 man for the refactored and simplified code
// https://github.com/saikou-app/saikou
package eu.kanade.tachiyomi.data.connections.discord

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds

sealed interface DiscordWebSocket : CoroutineScope {
    suspend fun sendActivity(presence: Presence)
    fun close()
}

open class DiscordWebSocketImpl(
    private val token: String,
) : DiscordWebSocket {

    private val json = Json {
        encodeDefaults = true
        allowStructuredMapKeys = true
        ignoreUnknownKeys = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val request = Request.Builder()
        .url("wss://gateway.discord.gg/?encoding=json&v=10")
        .build()

    private var webSocket: WebSocket? = client.newWebSocket(request, Listener())

    private var connected = false

    override val coroutineContext: CoroutineContext
        get() = SupervisorJob() + Dispatchers.Default

    private fun sendIdentify() {
        val response = Identity.Response(
            op = 2,
            d = Identity(
                token = token,
                properties = Identity.Properties(
                    os = "windows",
                    browser = "Chrome",
                    device = "disco",
                ),
                compress = false,
                intents = 0,
            ),
        )
        webSocket?.send(json.encodeToString(response))
    }

    @Suppress("MagicNumber")
    override fun close() {
        webSocket?.send(
            json.encodeToString(
                Presence.Response(
                    op = 3,
                    d = Presence(status = "offline"),
                ),
            ),
        )
        webSocket?.close(4000, "Interrupt")
        connected = false
    }

    override suspend fun sendActivity(presence: Presence) {
        // TODO : Figure out a better way to wait for socket to be connected to account
        while (!connected) {
            delay(10.milliseconds)
        }
        log("Sending ${OpCode.PRESENCE_UPDATE}")
        val response = Presence.Response(
            op = OpCode.PRESENCE_UPDATE.value.toLong(),
            d = presence,
        )
        webSocket?.send(json.encodeToString(response))
    }

    inner class Listener : WebSocketListener() {
        private var seq: Int? = null
        private var heartbeatInterval: Long? = null

        var scope = CoroutineScope(coroutineContext)

        private fun sendHeartBeat(sendIdentify: Boolean) {
            scope.cancel()
            scope = CoroutineScope(coroutineContext)
            scope.launch {
                delay(heartbeatInterval!!)
                webSocket?.send("{\"op\":1, \"d\":$seq}")
            }
            if (sendIdentify) sendIdentify()
        }

        @Suppress("MagicNumber")
        override fun onMessage(webSocket: WebSocket, text: String) {
            log("Message : $text")

            val map = json.decodeFromString<Res>(text)
            seq = map.s

            when (map.op) {
                OpCode.HELLO.value -> {
                    map.d
                    heartbeatInterval = map.d.jsonObject["heartbeat_interval"]!!.jsonPrimitive.long
                    sendHeartBeat(true)
                }
                OpCode.DISPATCH.value -> if (map.t == "READY") { connected = true }
                OpCode.HEARTBEAT.value -> {
                    if (scope.isActive) scope.cancel()
                    webSocket.send("{\"op\":1, \"d\":$seq}")
                }

                OpCode.HEARTBEAT_ACK.value -> sendHeartBeat(false)
                OpCode.RECONNECT.value -> webSocket.close(400, "Reconnect")
                OpCode.INVALID_SESSION.value -> sendHeartBeat(true)
            }
        }

        @Suppress("MagicNumber")
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            log("Server Closed : $code $reason")
            if (code == 4000) {
                scope.cancel()
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            log("Failure : ${t.message}")
            if (t.message != "Interrupt") {
                this@DiscordWebSocketImpl.webSocket = client.newWebSocket(request, Listener())
            }
        }
    }

    private fun log(message: String) {
        Log.i("discord_rpc_aniyomi", message)
    }
}
// <-- AM (DISCORD)
