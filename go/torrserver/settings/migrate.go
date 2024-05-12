package settings

import (
	"encoding/binary"
	"fmt"
	"os"
	"path/filepath"

	bolt "go.etcd.io/bbolt"
	"server/log"
	"server/web/api/utils"
)

var dbTorrentsName = []byte("Torrents")

type torrentOldDB struct {
	Name      string
	Magnet    string
	InfoBytes []byte
	Hash      string
	Size      int64
	Timestamp int64
}

func Migrate() {
	if _, err := os.Lstat(filepath.Join(Path, "torrserver.db")); os.IsNotExist(err) {
		return
	}

	db, err := bolt.Open(filepath.Join(Path, "torrserver.db"), 0o666, nil)
	if err != nil {
		return
	}

	torrs := make([]*torrentOldDB, 0)
	err = db.View(func(tx *bolt.Tx) error {
		tdb := tx.Bucket(dbTorrentsName)
		if tdb == nil {
			return nil
		}
		c := tdb.Cursor()
		for h, _ := c.First(); h != nil; h, _ = c.Next() {
			hdb := tdb.Bucket(h)
			if hdb != nil {
				torr := new(torrentOldDB)
				torr.Hash = string(h)
				tmp := hdb.Get([]byte("Name"))
				if tmp == nil {
					return fmt.Errorf("error load torrent")
				}
				torr.Name = string(tmp)

				tmp = hdb.Get([]byte("Link"))
				if tmp == nil {
					return fmt.Errorf("error load torrent")
				}
				torr.Magnet = string(tmp)

				tmp = hdb.Get([]byte("Size"))
				if tmp == nil {
					return fmt.Errorf("error load torrent")
				}
				torr.Size = b2i(tmp)

				tmp = hdb.Get([]byte("Timestamp"))
				if tmp == nil {
					return fmt.Errorf("error load torrent")
				}
				torr.Timestamp = b2i(tmp)

				torrs = append(torrs, torr)
			}
		}
		return nil
	})
	db.Close()
	if err == nil && len(torrs) > 0 {
		for _, torr := range torrs {
			spec, err := utils.ParseLink(torr.Magnet)
			if err != nil {
				continue
			}

			title := torr.Name
			if len(spec.DisplayName) > len(title) {
				title = spec.DisplayName
			}
			log.TLogln("Migrate torrent", torr.Name, torr.Hash, torr.Size)
			AddTorrent(&TorrentDB{
				TorrentSpec: spec,
				Title:       title,
				Timestamp:   torr.Timestamp,
				Size:        torr.Size,
			})
		}
	}
	os.Remove(filepath.Join(Path, "torrserver.db"))
}

func b2i(v []byte) int64 {
	return int64(binary.BigEndian.Uint64(v))
}
