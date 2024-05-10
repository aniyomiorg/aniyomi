package blocker

import (
	"bufio"
	"bytes"
	"errors"
	"net"
	"net/http"
	"os"
	"path/filepath"
	"strings"

	"server/log"
	"server/settings"

	"github.com/gin-gonic/gin"
)

func Blocker() gin.HandlerFunc {
	emptyFN := func(c *gin.Context) {
		c.Next()
	}

	name := filepath.Join(settings.Path, "bip.txt")
	buf, _ := os.ReadFile(name)
	blackIpList := scanBuf(buf)

	name = filepath.Join(settings.Path, "wip.txt")
	buf, _ = os.ReadFile(name)
	whiteIpList := scanBuf(buf)

	if blackIpList.NumRanges() == 0 && whiteIpList.NumRanges() == 0 {
		return emptyFN
	}

	return func(c *gin.Context) {
		arr := strings.Split(c.Request.RemoteAddr, ":")
		if len(arr) > 0 {
			ip := net.ParseIP(arr[0])
			minifyIP(&ip)
			if whiteIpList.NumRanges() > 0 {
				if _, ok := whiteIpList.Lookup(ip); !ok {
					log.WebLogln("Block ip, not in white list", ip.String())
					c.String(http.StatusTeapot, "Banned")
					c.Abort()
					return
				}
			}
			if blackIpList.NumRanges() > 0 {
				if r, ok := blackIpList.Lookup(ip); ok {
					log.WebLogln("Block ip, in black list:", ip.String(), "in range", r.Description, ":", r.First, "-", r.Last)
					c.String(http.StatusTeapot, "Banned")
					c.Abort()
					return
				}
			}
		}
		c.Next()
	}
}

func scanBuf(buf []byte) Ranger {
	if len(buf) == 0 {
		return New(nil)
	}
	var ranges []Range
	scanner := bufio.NewScanner(strings.NewReader(string(buf)))
	for scanner.Scan() {
		r, ok, err := parseLine(scanner.Bytes())
		if err != nil {
			log.TLogln("Error scan ip list:", err)
			return New(nil)
		}
		if ok {
			ranges = append(ranges, r)
		}
	}
	err := scanner.Err()
	if err != nil {
		log.TLogln("Error scan ip list:", err)
	}
	if len(ranges) > 0 {
		return New(ranges)
	}
	return New(nil)
}

func parseLine(l []byte) (r Range, ok bool, err error) {
	l = bytes.TrimSpace(l)
	if len(l) == 0 || bytes.HasPrefix(l, []byte("#")) {
		return
	}
	colon := bytes.LastIndexAny(l, ":")
	hyphen := bytes.IndexByte(l[colon+1:], '-')
	hyphen += colon + 1
	if colon >= 0 {
		r.Description = string(l[:colon])
	}
	if hyphen-(colon+1) >= 0 {
		r.First = net.ParseIP(string(l[colon+1 : hyphen]))
		minifyIP(&r.First)
		r.Last = net.ParseIP(string(l[hyphen+1:]))
		minifyIP(&r.Last)
	} else {
		r.First = net.ParseIP(string(l[colon+1:]))
		minifyIP(&r.First)
		r.Last = r.First
	}
	if r.First == nil || r.Last == nil || len(r.First) != len(r.Last) {
		err = errors.New("bad IP range")
		return
	}
	ok = true
	return
}
