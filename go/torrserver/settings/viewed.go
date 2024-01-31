package settings

import (
	"encoding/json"

	"server/log"
)

type Viewed struct {
	Hash      string `json:"hash"`
	FileIndex int    `json:"file_index"`
}

func SetViewed(vv *Viewed) {
	var indexes map[int]struct{}
	var err error

	buf := tdb.Get("Viewed", vv.Hash)
	if len(buf) == 0 {
		indexes = make(map[int]struct{})
		indexes[vv.FileIndex] = struct{}{}
		buf, err = json.Marshal(indexes)
		if err == nil {
			tdb.Set("Viewed", vv.Hash, buf)
		}
	} else {
		err = json.Unmarshal(buf, &indexes)
		if err == nil {
			indexes[vv.FileIndex] = struct{}{}
			buf, err = json.Marshal(indexes)
			if err == nil {
				tdb.Set("Viewed", vv.Hash, buf)
			}
		}
	}
	if err != nil {
		log.TLogln("Error set viewed:", err)
	}
}

func RemViewed(vv *Viewed) {
	buf := tdb.Get("Viewed", vv.Hash)
	var indeces map[int]struct{}
	err := json.Unmarshal(buf, &indeces)
	if err == nil {
		if vv.FileIndex != -1 {
			delete(indeces, vv.FileIndex)
			buf, err = json.Marshal(indeces)
			if err == nil {
				tdb.Set("Viewed", vv.Hash, buf)
			}
		} else {
			tdb.Rem("Viewed", vv.Hash)
		}
	}
	if err != nil {
		log.TLogln("Error rem viewed:", err)
	}
}

func ListViewed(hash string) []*Viewed {
	var err error
	if hash != "" {
		buf := tdb.Get("Viewed", hash)
		if len(buf) == 0 {
			return []*Viewed{}
		}
		var indeces map[int]struct{}
		err = json.Unmarshal(buf, &indeces)
		if err == nil {
			var ret []*Viewed
			for i := range indeces {
				ret = append(ret, &Viewed{hash, i})
			}
			return ret
		}
	} else {
		var ret []*Viewed
		keys := tdb.List("Viewed")
		for _, key := range keys {
			buf := tdb.Get("Viewed", key)
			if len(buf) == 0 {
				return []*Viewed{}
			}
			var indeces map[int]struct{}
			err = json.Unmarshal(buf, &indeces)
			if err == nil {
				for i := range indeces {
					ret = append(ret, &Viewed{key, i})
				}
			}
		}
		return ret
	}

	if err != nil {
		log.TLogln("Error list viewed:", err)
	}
	return []*Viewed{}
}
