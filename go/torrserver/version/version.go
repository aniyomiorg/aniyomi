package version

import (
	"log"
	"runtime/debug"
	// "github.com/anacrolix/torrent"
)

const Version = "MatriX.128"

func GetTorrentVersion() string {
	bi, ok := debug.ReadBuildInfo()
	if !ok {
		log.Printf("Failed to read build info")
		return ""
	}
	for _, dep := range bi.Deps {
		if dep.Path == "github.com/anacrolix/torrent" {
			if dep.Replace != nil {
				return dep.Replace.Version
			} else {
				return dep.Version
			}
		}
	}
	return ""
}
