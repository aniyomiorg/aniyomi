package torrstor

import (
	"errors"

	"github.com/anacrolix/torrent/storage"
)

type PieceFake struct{}

func (PieceFake) ReadAt(p []byte, off int64) (n int, err error) {
	err = errors.New("fake")
	return
}

func (PieceFake) WriteAt(p []byte, off int64) (n int, err error) {
	err = errors.New("fake")
	return
}

func (PieceFake) MarkComplete() error {
	return errors.New("fake")
}

func (PieceFake) MarkNotComplete() error {
	return errors.New("fake")
}

func (PieceFake) Completion() storage.Completion {
	return storage.Completion{
		Complete: false,
		Ok:       true,
	}
}
