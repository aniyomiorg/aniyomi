package eu.kanade.tachiyomi.data.torrentServer.service


import android.app.Application
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.torrentServer.TorrentServerApi
import eu.kanade.tachiyomi.data.torrentServer.TorrentServerFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.coroutines.EmptyCoroutineContext


class TorrentServerService: Service() {
    private val serverFile = TorrentServerFile()
    private val serviceScope = CoroutineScope(EmptyCoroutineContext)
    private val applicationContext = Injekt.get<Application>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if(it.action != null){
                when(it.action){
                    ACTION_START -> {
                        startServer()
                        return START_STICKY
                    }
                    ACTION_STOP -> {
                        stopServer()
                        return START_NOT_STICKY
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startServer(){
        serviceScope.launch {
            if(serverFile.exists() && TorrentServerApi.echo() == ""){
                if (BuildConfig.DEBUG) Log.d("TorrentService", "startServer()")
                serverFile.run()
                applicationContext.startService(Intent(applicationContext, TorrentServerNotification::class.java))
            }
        }
    }

    private fun stopServer(){
        serviceScope.launch {
            if(serverFile.exists()){
                if (BuildConfig.DEBUG) Log.d("TorrentService", "stopServer()")
                serverFile.stop()
                applicationContext.stopService(Intent(applicationContext, TorrentServerNotification::class.java))
                stopSelf()
            }
        }
    }


    companion object {
        const val ACTION_START = "start_torrent_server"
        const val ACTION_STOP = "stop_torrent_server"
        val applicationContext = Injekt.get<Application>()

        fun start(){
            try {
                val intent = Intent(applicationContext, TorrentServerService::class.java).apply {
                    action = ACTION_START
                }
                applicationContext.startService(intent)
            }catch (e: Exception){
                if (BuildConfig.DEBUG) Log.d("TorrentService", "start() error: ${e.message}")
                e.printStackTrace()
            }
        }

        fun stop(){
            try {
                val intent = Intent(applicationContext, TorrentServerService::class.java).apply {
                    action = ACTION_STOP
                }
                applicationContext.startService(intent)
            }catch (e: Exception){
                if (BuildConfig.DEBUG) Log.d("TorrentService", "stop() error: ${e.message}")
                e.printStackTrace()
            }
        }

        fun wait(timeout: Int = -1): Boolean {
            var count = 0
            if (timeout < 0)
                count = -20
            while (TorrentServerApi.echo() == "") {
                Thread.sleep(1000)
                count++
                if (count > timeout)
                    return false
            }
            return true
        }

        fun isInstalled(): Boolean = TorrentServerFile().exists()
    }

}

