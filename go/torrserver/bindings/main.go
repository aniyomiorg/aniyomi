package torrServer

import (
	server "server"
)

func StartTorrentServer(pathdb string) {
	server.Start(pathdb, "", false, false)
}

func WaitTorrentServer() {
	server.WaitServer()
}

func StopTorrentServer() {
	server.Stop()
}

func AddTrackers(trackers string) {
	server.AddTrackers(trackers)
}
