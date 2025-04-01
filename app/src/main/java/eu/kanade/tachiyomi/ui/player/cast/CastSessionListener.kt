package eu.kanade.tachiyomi.ui.player.cast
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import eu.kanade.tachiyomi.ui.player.CastManager

class CastSessionListener(
    private val castManager: CastManager,
) : SessionManagerListener<CastSession> {

    override fun onSessionStarted(session: CastSession, sessionId: String) {
        castManager.onSessionConnected(session)
        castManager.handleQualitySelection()
    }

    override fun onSessionEnded(session: CastSession, error: Int) {
        castManager.onSessionEnded()
    }

    override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
        castManager.onSessionConnected(session)
        session.remoteMediaClient?.let { client ->
            if (client.mediaQueue.itemCount == 0) {
                castManager.handleQualitySelection()
            }
        }
    }

    override fun onSessionResumeFailed(session: CastSession, error: Int) {
        castManager.onSessionEnded()
    }

    override fun onSessionStarting(session: CastSession) {}
    override fun onSessionStartFailed(session: CastSession, error: Int) {}
    override fun onSessionEnding(session: CastSession) {}
    override fun onSessionSuspended(session: CastSession, reason: Int) {}
    override fun onSessionResuming(p0: CastSession, p1: String) {}
}
