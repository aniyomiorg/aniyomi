package server

import (
	"net"
	"os"
	"path/filepath"
	"strconv"
	"strings"

	"server/log"
	"server/settings"
	"server/torr/utils"
	"server/web"
)

func Start(pathdb, port, sslport, sslCert, sslKey string, sslEnabled, roSets, searchWA bool) {
	settings.Path = pathdb
	settings.InitSets(roSets, searchWA)
	if roSets {
		log.TLogln("Enabled Read-only DB mode!")
	}
	// https checks
	if sslEnabled {
		// set settings ssl enabled
		settings.Ssl = sslEnabled
		if sslport == "" {
			dbSSlPort := strconv.Itoa(settings.BTsets.SslPort)
			if dbSSlPort != "0" {
				sslport = dbSSlPort
			} else {
				sslport = "8091"
			}
		} else { // store ssl port from params to DB
			dbSSlPort, err := strconv.Atoi(sslport)
			if err == nil {
				settings.BTsets.SslPort = dbSSlPort
			}
		}
		// check if ssl cert and key files exist
		if sslCert != "" && sslKey != "" {
			// set settings ssl cert and key files
			settings.BTsets.SslCert = sslCert
			settings.BTsets.SslKey = sslKey
		}
		log.TLogln("Check web ssl port", sslport)
		l, err := net.Listen("tcp", ":"+sslport)
		if l != nil {
			l.Close()
		}
		if err != nil {
			log.TLogln("Port", sslport, "already in use! Please set different port for HTTPS. Abort")
			os.Exit(1)
		}
	}
	// http checks
	if port == "" {
		port = "8090"
	}
	log.TLogln("Check web port", port)
	l, err := net.Listen("tcp", ":"+port)
	if l != nil {
		l.Close()
	}
	if err != nil {
		log.TLogln("Port", port, "already in use! Please set different sslport for HTTP. Abort")
		os.Exit(1)
	}
	// remove old disk caches
	go cleanCache()
	// set settings http and https ports. Start web server.
	settings.Port = port
	settings.SslPort = sslport
	web.Start()
}

func cleanCache() {
	if !settings.BTsets.UseDisk || settings.BTsets.TorrentsSavePath == "/" || settings.BTsets.TorrentsSavePath == "" {
		return
	}

	dirs, err := os.ReadDir(settings.BTsets.TorrentsSavePath)
	if err != nil {
		return
	}

	torrs := settings.ListTorrent()

	log.TLogln("Remove unused cache in dir:", settings.BTsets.TorrentsSavePath)
	for _, d := range dirs {
		if len(d.Name()) != 40 {
			// Not a hash
			continue
		}

		if !settings.BTsets.RemoveCacheOnDrop {
			for _, t := range torrs {
				if d.IsDir() && d.Name() != t.InfoHash.HexString() {
					log.TLogln("Remove unused cache:", d.Name())
					removeAllFiles(filepath.Join(settings.BTsets.TorrentsSavePath, d.Name()))
					break
				}
			}
		} else {
			if d.IsDir() {
				log.TLogln("Remove unused cache:", d.Name())
				removeAllFiles(filepath.Join(settings.BTsets.TorrentsSavePath, d.Name()))
			}
		}
	}
}

func removeAllFiles(path string) {
	files, err := os.ReadDir(path)
	if err != nil {
		return
	}
	for _, f := range files {
		name := filepath.Join(path, f.Name())
		os.Remove(name)
	}
	os.Remove(path)
}

func WaitServer() string {
	err := web.Wait()
	if err != nil {
		return err.Error()
	}
	return ""
}

func Stop() {
	web.Stop()
	settings.CloseDB()
}

func AddTrackers(trackers string) {
	tracks := strings.Split(trackers, ",\n")
	utils.SetDefTrackers(tracks)
}
