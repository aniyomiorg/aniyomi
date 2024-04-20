package api

import (
	"bytes"
	"encoding/hex"
	"fmt"
	"net/http"
	"net/url"
	"path/filepath"
	"sort"
	"strings"
	"time"

	"github.com/anacrolix/missinggo/v2/httptoo"

	sets "server/settings"
	"server/torr"
	"server/torr/state"
	"server/utils"

	"github.com/gin-gonic/gin"
	"github.com/pkg/errors"
)

// allPlayList godoc
//
//	@Summary		Get a M3U playlist with all torrents
//	@Description	Retrieve all torrents and generates a bundled M3U playlist.
//
//	@Tags			API
//
//	@Produce		audio/x-mpegurl
//	@Success		200	{file}	file
//	@Router			/playlistall/all.m3u [get]
func allPlayList(c *gin.Context) {
	torrs := torr.ListTorrent()

	host := utils.GetScheme(c) + "://" + c.Request.Host
	list := "#EXTM3U\n"
	hash := ""
	// fn=file.m3u fix forkplayer bug with end .m3u in link
	for _, tr := range torrs {
		list += "#EXTINF:0 type=\"playlist\"," + tr.Title + "\n"
		list += host + "/stream/" + url.PathEscape(tr.Title) + ".m3u?link=" + tr.TorrentSpec.InfoHash.HexString() + "&m3u&fn=file.m3u\n"
		hash += tr.Hash().HexString()
	}

	sendM3U(c, "all.m3u", hash, list)
}

// playList godoc
//
//	@Summary		Get HTTP link of torrent in M3U list
//	@Description	Get HTTP link of torrent in M3U list.
//
//	@Tags			API
//
//	@Param			hash		query	string	true	"Torrent hash"
//	@Param			fromlast	query	bool	false	"From last play file"
//
//	@Produce		audio/x-mpegurl
//	@Success		200	{file}	file
//	@Router			/playlist [get]
func playList(c *gin.Context) {
	hash, _ := c.GetQuery("hash")
	_, fromlast := c.GetQuery("fromlast")
	if hash == "" {
		c.AbortWithError(http.StatusBadRequest, errors.New("hash is empty"))
		return
	}

	tor := torr.GetTorrent(hash)
	if tor == nil {
		c.AbortWithStatus(http.StatusNotFound)
		return
	}

	if tor.Stat == state.TorrentInDB {
		tor = torr.LoadTorrent(tor)
		if tor == nil {
			c.AbortWithError(http.StatusInternalServerError, errors.New("error get torrent info"))
			return
		}
	}

	host := utils.GetScheme(c) + "://" + c.Request.Host
	list := getM3uList(tor.Status(), host, fromlast)
	list = "#EXTM3U\n" + list
	name := strings.ReplaceAll(c.Param("fname"), `/`, "") // strip starting / from param
	if name == "" {
		name = tor.Name() + ".m3u"
	} else if !strings.HasSuffix(strings.ToLower(name), ".m3u") && !strings.HasSuffix(strings.ToLower(name), ".m3u8") {
		name += ".m3u"
	}

	sendM3U(c, name, tor.Hash().HexString(), list)
}

func sendM3U(c *gin.Context, name, hash string, m3u string) {
	c.Header("Content-Type", "audio/x-mpegurl")
	c.Header("Connection", "close")
	if hash != "" {
		etag := hex.EncodeToString([]byte(fmt.Sprintf("%s/%s", hash, name)))
		c.Header("ETag", httptoo.EncodeQuotedString(etag))
	}
	if name == "" {
		name = "playlist.m3u"
	}
	c.Header("Content-Disposition", `attachment; filename="`+name+`"`)
	http.ServeContent(c.Writer, c.Request, name, time.Now(), bytes.NewReader([]byte(m3u)))
}

func getM3uList(tor *state.TorrentStatus, host string, fromLast bool) string {
	m3u := ""
	from := 0
	if fromLast {
		pos := searchLastPlayed(tor)
		if pos != -1 {
			from = pos
		}
	}
	for i, f := range tor.FileStats {
		if i >= from {
			if utils.GetMimeType(f.Path) != "*/*" {
				fn := filepath.Base(f.Path)
				if fn == "" {
					fn = f.Path
				}
				m3u += "#EXTINF:0," + fn + "\n"
				fileNamesakes := findFileNamesakes(tor.FileStats, f) // find external media with same name (audio/subtiles tracks)
				if fileNamesakes != nil {
					m3u += "#EXTVLCOPT:input-slave="         // include VLC option for external media
					for _, namesake := range fileNamesakes { // include play-links to external media, with # splitter
						sname := filepath.Base(namesake.Path)
						m3u += host + "/stream/" + url.PathEscape(sname) + "?link=" + tor.Hash + "&index=" + fmt.Sprint(namesake.Id) + "&play#"
					}
					m3u += "\n"
				}
				name := filepath.Base(f.Path)
				m3u += host + "/stream/" + url.PathEscape(name) + "?link=" + tor.Hash + "&index=" + fmt.Sprint(f.Id) + "&play\n"
			}
		}
	}
	return m3u
}

func findFileNamesakes(files []*state.TorrentFileStat, file *state.TorrentFileStat) []*state.TorrentFileStat {
	// find files with the same name in torrent
	name := filepath.Base(strings.TrimSuffix(file.Path, filepath.Ext(file.Path)))
	var namesakes []*state.TorrentFileStat
	for _, f := range files {
		if strings.Contains(f.Path, name) { // external tracks always include name of videofile
			if f != file { // exclude itself
				namesakes = append(namesakes, f)
			}
		}
	}
	return namesakes
}

func searchLastPlayed(tor *state.TorrentStatus) int {
	viewed := sets.ListViewed(tor.Hash)
	if len(viewed) == 0 {
		return -1
	}
	sort.Slice(viewed, func(i, j int) bool {
		return viewed[i].FileIndex > viewed[j].FileIndex
	})

	lastViewedIndex := viewed[0].FileIndex

	for i, stat := range tor.FileStats {
		if stat.Id == lastViewedIndex {
			if i >= len(tor.FileStats) {
				return -1
			}
			return i
		}
	}

	return -1
}
