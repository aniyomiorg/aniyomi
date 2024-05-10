package rutor

import (
	"compress/flate"
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"testing"

	"server/rutor/models"
)

func TestParseChannel(t *testing.T) {
	channel := make(chan *models.TorrentDetails, 0)
	var ftors []*models.TorrentDetails
	go func() {
		for torr := range channel {
			ftors = append(ftors, torr)
		}
	}()

	path, _ := os.Getwd()
	ff, err := os.Open(filepath.Join(path, "rutor.ls"))
	if err == nil {
		defer ff.Close()
		r := flate.NewReader(ff)
		defer r.Close()
		dec := json.NewDecoder(r)

		_, err := dec.Token()
		if err != nil {
			t.Error(err)
		}

		for dec.More() {
			var torr *models.TorrentDetails
			err = dec.Decode(&torr)
			if err != nil {
				t.Error(err)
			}
			channel <- torr
		}
		close(channel)
	} else {
		t.Error(err)
	}
}

func TestParseArr(t *testing.T) {
	var ftors []*models.TorrentDetails
	path, _ := os.Getwd()
	ff, err := os.Open(filepath.Join(path, "rutor.ls"))
	if err == nil {
		defer ff.Close()
		r := flate.NewReader(ff)
		defer r.Close()
		dec := json.NewDecoder(r)

		_, err := dec.Token()
		if err != nil {
			t.Error(err)
		}

		for dec.More() {
			var torr *models.TorrentDetails
			err = dec.Decode(&torr)
			if err != nil {
				t.Error(err)
			}
			ftors = append(ftors, torr)
			fmt.Println(len(ftors))
		}
	} else {
		t.Error(err)
	}
}
