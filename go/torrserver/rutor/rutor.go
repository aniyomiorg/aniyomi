package rutor

import (
	"compress/flate"
	"encoding/json"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/agnivade/levenshtein"

	"server/log"
	"server/rutor/models"
	"server/rutor/torrsearch"
	"server/rutor/utils"
	"server/settings"
	utils2 "server/torr/utils"
)

var (
	torrs  []*models.TorrentDetails
	isStop bool
)

func Start() {
	go func() {
		if settings.BTsets.EnableRutorSearch {
			if !updateDB() {
				loadDB()
			}
			isStop = false
			for !isStop {
				for i := 0; i < 3*60*60; i++ {
					time.Sleep(time.Second)
					if isStop {
						return
					}
				}
				updateDB()
			}
		}
	}()
}

func Stop() {
	isStop = true
	torrs = nil
	torrsearch.NewIndex(nil)
	utils2.FreeOSMemGC()
	time.Sleep(time.Millisecond * 1500)
}

// http://releases.yourok.ru/torr/rutor.ls
func updateDB() bool {
	log.TLogln("Update rutor db")

	fnOrig := filepath.Join(settings.Path, "rutor.ls")

	if fi, err := os.Stat(fnOrig); err == nil {
		if time.Since(fi.ModTime()) < time.Minute*175 /*2:55*/ {
			log.TLogln("Less 3 hours rutor db old")
			return false
		}
	}

	fnTmp := filepath.Join(settings.Path, "rutor.tmp")
	out, err := os.Create(fnTmp)
	if err != nil {
		log.TLogln("Error create file rutor.tmp:", err)
		return false
	}

	resp, err := http.Get("http://releases.yourok.ru/torr/rutor.ls")
	if err != nil {
		log.TLogln("Error connect to rutor db:", err)
		out.Close()
		return false
	}
	defer resp.Body.Close()
	_, err = io.Copy(out, resp.Body)
	out.Close()
	if err != nil {
		log.TLogln("Error download rutor db:", err)
		return false
	}

	md5Tmp := utils.MD5File(fnTmp)
	md5Orig := utils.MD5File(fnOrig)
	if md5Tmp != md5Orig {
		err = os.Remove(fnOrig)
		if err != nil && !os.IsNotExist(err) {
			log.TLogln("Error remove old rutor db:", err)
			return false
		}
		err = os.Rename(fnTmp, fnOrig)
		if err != nil {
			log.TLogln("Error rename rutor db:", err)
			return false
		}
		loadDB()
		return true
	} else {
		os.Remove(fnTmp)
	}
	return false
}

func loadDB() {
	log.TLogln("Load rutor db")
	ff, err := os.Open(filepath.Join(settings.Path, "rutor.ls"))
	if err == nil {
		defer ff.Close()
		r := flate.NewReader(ff)
		defer r.Close()
		var ftorrs []*models.TorrentDetails
		dec := json.NewDecoder(r)

		_, err := dec.Token()
		if err != nil {
			log.TLogln("Error read token rutor db:", err)
			return
		}

		for dec.More() {
			var torr *models.TorrentDetails
			err = dec.Decode(&torr)
			if err == nil {
				ftorrs = append(ftorrs, torr)
			}
		}
		torrs = ftorrs
		log.TLogln("Index rutor db")
		torrsearch.NewIndex(torrs)
		log.TLogln("Torrents count:", len(torrs))
		log.TLogln("Indexed words:", len(torrsearch.GetIDX()))

	} else {
		log.TLogln("Error load rutor db:", err)
	}
	utils2.FreeOSMemGC()
}

func Search(query string) []*models.TorrentDetails {
	if !settings.BTsets.EnableRutorSearch {
		return nil
	}
	matchedIDs := torrsearch.Search(query)
	if len(matchedIDs) == 0 {
		return nil
	}
	var list []*models.TorrentDetails
	for _, id := range matchedIDs {
		list = append(list, torrs[id])
	}

	hash := utils.ClearStr(query)

	sort.Slice(list, func(i, j int) bool {
		lhash := utils.ClearStr(strings.ToLower(list[i].Name+list[i].GetNames())) + strconv.Itoa(list[i].Year)
		lev1 := levenshtein.ComputeDistance(hash, lhash)
		lhash = utils.ClearStr(strings.ToLower(list[j].Name+list[j].GetNames())) + strconv.Itoa(list[j].Year)
		lev2 := levenshtein.ComputeDistance(hash, lhash)
		if lev1 == lev2 {
			return list[j].CreateDate.Before(list[i].CreateDate)
		}
		return lev1 < lev2
	})
	return list
}
