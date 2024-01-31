package torrstor

import (
	"sort"

	"github.com/anacrolix/torrent"
)

type Range struct {
	Start, End int
	File       *torrent.File
}

func inRanges(ranges []Range, ind int) bool {
	for _, r := range ranges {
		if ind >= r.Start && ind <= r.End {
			return true
		}
	}
	return false
}

func mergeRange(ranges []Range) []Range {
	if len(ranges) <= 1 {
		return ranges
	}
	// copy ranges
	merged := append([]Range(nil), ranges...)

	sort.Slice(merged, func(i, j int) bool {
		if merged[i].Start < merged[j].Start {
			return true
		}
		if merged[i].Start == merged[j].Start && merged[i].End < merged[j].End {
			return true
		}
		return false
	})

	j := 0
	for i := 1; i < len(merged); i++ {
		if merged[j].End >= merged[i].Start {
			if merged[j].End < merged[i].End {
				merged[j].End = merged[i].End
			}
		} else {
			j++
			merged[j] = merged[i]
		}
	}
	return merged[:j+1]
}
