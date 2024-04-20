package torrstor

import (
	"github.com/anacrolix/torrent"
	"github.com/anacrolix/torrent/storage"
	"server/settings"
)

type Piece struct {
	storage.PieceImpl `json:"-"`

	Id   int   `json:"-"`
	Size int64 `json:"size"`

	Complete bool  `json:"complete"`
	Accessed int64 `json:"accessed"`

	mPiece *MemPiece  `json:"-"`
	dPiece *DiskPiece `json:"-"`

	cache *Cache `json:"-"`
}

func NewPiece(id int, cache *Cache) *Piece {
	p := &Piece{
		Id:    id,
		cache: cache,
	}

	if !settings.BTsets.UseDisk {
		p.mPiece = NewMemPiece(p)
	} else {
		p.dPiece = NewDiskPiece(p)
	}
	return p
}

func (p *Piece) WriteAt(b []byte, off int64) (n int, err error) {
	if !settings.BTsets.UseDisk {
		return p.mPiece.WriteAt(b, off)
	} else {
		return p.dPiece.WriteAt(b, off)
	}
}

func (p *Piece) ReadAt(b []byte, off int64) (n int, err error) {
	if !settings.BTsets.UseDisk {
		return p.mPiece.ReadAt(b, off)
	} else {
		return p.dPiece.ReadAt(b, off)
	}
}

func (p *Piece) MarkComplete() error {
	p.Complete = true
	return nil
}

func (p *Piece) MarkNotComplete() error {
	p.Complete = false
	return nil
}

func (p *Piece) Completion() storage.Completion {
	return storage.Completion{
		Complete: p.Complete,
		Ok:       true,
	}
}

func (p *Piece) Release() {
	if !settings.BTsets.UseDisk {
		p.mPiece.Release()
	} else {
		p.dPiece.Release()
	}
	if !p.cache.isClosed {
		p.cache.torrent.Piece(p.Id).SetPriority(torrent.PiecePriorityNone)
		p.cache.torrent.Piece(p.Id).UpdateCompletion()
	}
}
