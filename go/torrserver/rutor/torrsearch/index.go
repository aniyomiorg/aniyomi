package torrsearch

import (
	"server/rutor/models"
)

// Index is an inverted Index. It maps tokens to document IDs.
type Index map[string][]int

var idx Index

func NewIndex(torrs []*models.TorrentDetails) {
	idx = make(Index)
	idx.add(torrs)
}

func Search(text string) []int {
	return idx.search(text)
}

func GetIDX() Index {
	return idx
}

func (idx Index) add(torrs []*models.TorrentDetails) {
	for ID, torr := range torrs {
		for _, token := range analyze(torr.Title) {
			ids := idx[token]
			if ids != nil && ids[len(ids)-1] == ID {
				// Don't add same ID twice.
				continue
			}
			idx[token] = append(ids, ID)
		}
	}
}

// intersection returns the set intersection between a and b.
// a and b have to be sorted in ascending order and contain no duplicates.
func intersection(a []int, b []int) []int {
	maxLen := len(a)
	if len(b) > maxLen {
		maxLen = len(b)
	}
	r := make([]int, 0, maxLen)
	var i, j int
	for i < len(a) && j < len(b) {
		if a[i] < b[j] {
			i++
		} else if a[i] > b[j] {
			j++
		} else {
			r = append(r, a[i])
			i++
			j++
		}
	}
	return r
}

// Search queries the Index for the given text.
func (idx Index) search(text string) []int {
	var r []int
	for _, token := range analyze(text) {
		if ids, ok := idx[token]; ok {
			if r == nil {
				r = ids
			} else {
				r = intersection(r, ids)
			}
		} else {
			// Token doesn't exist.
			return nil
		}
	}
	return r
}
