package storage

import (
	"github.com/anacrolix/torrent/metainfo"
	"github.com/anacrolix/torrent/storage"
)

type Storage interface {
	storage.ClientImpl

	CloseHash(hash metainfo.Hash)
}
