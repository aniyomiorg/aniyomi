package torr

import (
	"encoding/json"
	"time"

	"server/torr/utils"

	"server/settings"
	"server/torr/state"

	"github.com/anacrolix/torrent/metainfo"
)

type tsFiles struct {
	TorrServer struct {
		Files []*state.TorrentFileStat `json:"Files"`
	} `json:"TorrServer"`
}

func AddTorrentDB(torr *Torrent) {
	t := new(settings.TorrentDB)
	t.TorrentSpec = torr.TorrentSpec
	t.Title = torr.Title
	if torr.Data == "" {
		files := new(tsFiles)
		files.TorrServer.Files = torr.Status().FileStats
		buf, _ := json.Marshal(files)
		t.Data = string(buf)
		torr.Data = t.Data
	} else {
		t.Data = torr.Data
	}
	if utils.CheckImgUrl(torr.Poster) {
		t.Poster = torr.Poster
	}
	t.Size = torr.Size
	if t.Size == 0 && torr.Torrent != nil {
		t.Size = torr.Torrent.Length()
	}
	t.Timestamp = time.Now().Unix()
	settings.AddTorrent(t)
}

func GetTorrentDB(hash metainfo.Hash) *Torrent {
	list := settings.ListTorrent()
	for _, db := range list {
		if hash == db.InfoHash {
			torr := new(Torrent)
			torr.TorrentSpec = db.TorrentSpec
			torr.Title = db.Title
			torr.Poster = db.Poster
			torr.Timestamp = db.Timestamp
			torr.Size = db.Size
			torr.Data = db.Data
			torr.Stat = state.TorrentInDB
			return torr
		}
	}
	return nil
}

func RemTorrentDB(hash metainfo.Hash) {
	settings.RemTorrent(hash)
}

func ListTorrentsDB() map[metainfo.Hash]*Torrent {
	ret := make(map[metainfo.Hash]*Torrent)
	list := settings.ListTorrent()
	for _, db := range list {
		torr := new(Torrent)
		torr.TorrentSpec = db.TorrentSpec
		torr.Title = db.Title
		torr.Poster = db.Poster
		torr.Timestamp = db.Timestamp
		torr.Size = db.Size
		torr.Data = db.Data
		torr.Stat = state.TorrentInDB
		ret[torr.TorrentSpec.InfoHash] = torr
	}
	return ret
}
