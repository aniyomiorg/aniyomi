package blocker

import (
	"bytes"
	"fmt"
	"net"
)

type Ranger interface {
	Lookup(net.IP) (r Range, ok bool)
	NumRanges() int
}

type IPList struct {
	ranges []Range
}

type Range struct {
	First, Last net.IP
	Description string
}

func (r Range) String() string {
	return fmt.Sprintf("%s-%s: %s", r.First, r.Last, r.Description)
}

// Create a new IP list. The given ranges must already sorted by the lower
// bound IP in each range. Behaviour is undefined for lists of overlapping
// ranges.
func New(initSorted []Range) *IPList {
	return &IPList{
		ranges: initSorted,
	}
}

func (ipl *IPList) NumRanges() int {
	if ipl == nil {
		return 0
	}
	return len(ipl.ranges)
}

// Return the range the given IP is in. ok if false if no range is found.
func (ipl *IPList) Lookup(ip net.IP) (r Range, ok bool) {
	if ipl == nil {
		return
	}
	v4 := ip.To4()
	if v4 != nil {
		r, ok = ipl.lookup(v4)
		if ok {
			return
		}
	}
	v6 := ip.To16()
	if v6 != nil {
		return ipl.lookup(v6)
	}
	if v4 == nil && v6 == nil {
		r = Range{
			Description: "bad IP",
		}
		ok = true
	}
	return
}

// Return the range the given IP is in. Returns nil if no range is found.
func (ipl *IPList) lookup(ip net.IP) (Range, bool) {
	var rng Range
	ok := false
	for _, r := range ipl.ranges {
		ok = bytes.Compare(r.First, ip) <= 0 && bytes.Compare(ip, r.Last) <= 0
		if ok {
			rng = r
			break
		}
	}
	return rng, ok
}

func minifyIP(ip *net.IP) {
	v4 := ip.To4()
	if v4 != nil {
		*ip = append(make([]byte, 0, 4), v4...)
	}
}
