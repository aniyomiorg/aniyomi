package api

import (
	"net/http"
	"net/url"
	"strconv"
	"strings"

	"server/torr"
	"server/torr/state"
	utils2 "server/utils"
	"server/web/api/utils"

	"github.com/gin-gonic/gin"
	"github.com/pkg/errors"
)

// get stat
// http://127.0.0.1:8090/stream/fname?link=...&stat
// get m3u
// http://127.0.0.1:8090/stream/fname?link=...&index=1&m3u
// http://127.0.0.1:8090/stream/fname?link=...&index=1&m3u&fromlast
// stream torrent
// http://127.0.0.1:8090/stream/fname?link=...&index=1&play
// http://127.0.0.1:8090/stream/fname?link=...&index=1&play&preload
// http://127.0.0.1:8090/stream/fname?link=...&index=1&play&save
// http://127.0.0.1:8090/stream/fname?link=...&index=1&play&save&title=...&poster=...
// only save
// http://127.0.0.1:8090/stream/fname?link=...&save&title=...&poster=...

// stream godoc
//
//	@Summary		Multi usage endpoint
//	@Description	Multi usage endpoint.
//
//	@Tags			API
//
//	@Param			link	query	string	true	"Magnet/hash/link to torrent"
//	@Param			index		query	string	false	"File index in torrent"
//	@Param			preload		query	string	false	"Should preload torrent"
//	@Param			stat		query	string	false	"Get statistics from torrent"
//	@Param			save		query	string	false	"Should save torrent"
//	@Param			m3u		query	string	false	"Get torrent as M3U playlist"
//	@Param			fromlast		query	string	false	"Get m3u from last play"
//	@Param			play		query	string	false	"Start stream torrent"
//	@Param			title		query	string	true	"Set title of torrent"
//	@Param			poster		query	string	true	"File index in torrent"
//	@Param			not_auth		query	string	true	"Set poster link of torrent"
//
//	@Produce		application/octet-stream
//	@Success		200	"Data returned according to query"
//	@Router			/stream [get]
func stream(c *gin.Context) {
	link := c.Query("link")
	indexStr := c.Query("index")
	_, preload := c.GetQuery("preload")
	_, stat := c.GetQuery("stat")
	_, save := c.GetQuery("save")
	_, m3u := c.GetQuery("m3u")
	_, fromlast := c.GetQuery("fromlast")
	_, play := c.GetQuery("play")
	title := c.Query("title")
	poster := c.Query("poster")
	data := ""
	notAuth := c.GetBool("not_auth")

	if notAuth && (play || m3u) {
		streamNoAuth(c)
		return
	}
	if notAuth {
		c.Header("WWW-Authenticate", "Basic realm=Authorization Required")
		c.AbortWithStatus(http.StatusUnauthorized)
		return
	}

	if link == "" {
		c.AbortWithError(http.StatusBadRequest, errors.New("link should not be empty"))
		return
	}

	title, _ = url.QueryUnescape(title)
	link, _ = url.QueryUnescape(link)
	poster, _ = url.QueryUnescape(poster)

	spec, err := utils.ParseLink(link)
	if err != nil {
		c.AbortWithError(http.StatusInternalServerError, err)
		return
	}

	tor := torr.GetTorrent(spec.InfoHash.HexString())
	if tor != nil {
		title = tor.Title
		poster = tor.Poster
		data = tor.Data
	}
	if tor == nil || tor.Stat == state.TorrentInDB {
		tor, err = torr.AddTorrent(spec, title, poster, data)
		if err != nil {
			c.AbortWithError(http.StatusInternalServerError, err)
			return
		}
	}

	if !tor.GotInfo() {
		c.AbortWithError(http.StatusInternalServerError, errors.New("timeout connection torrent"))
		return
	}

	if tor.Title == "" {
		tor.Title = tor.Name()
	}

	// save to db
	if save {
		torr.SaveTorrentToDB(tor)
		c.Status(200) // only set status, not return
	}

	// find file
	index := -1
	if len(tor.Files()) == 1 {
		index = 0
	} else {
		ind, err := strconv.Atoi(indexStr)
		if err == nil {
			index = ind
		}
	}
	if index == -1 && play { // if file index not set and play file exec
		index = 0
	}
	// preload torrent
	if preload {
		torr.Preload(tor, index)
	}
	// return stat if query
	if stat {
		c.JSON(200, tor.Status())
		return
	} else
	// return m3u if query
	if m3u {
		name := strings.ReplaceAll(c.Param("fname"), `/`, "") // strip starting / from param
		if name == "" {
			name = tor.Name() + ".m3u"
		} else if !strings.HasSuffix(strings.ToLower(name), ".m3u") && !strings.HasSuffix(strings.ToLower(name), ".m3u8") {
			name += ".m3u"
		}
		m3ulist := "#EXTM3U\n" + getM3uList(tor.Status(), utils2.GetScheme(c)+"://"+c.Request.Host, fromlast)
		sendM3U(c, name, tor.Hash().HexString(), m3ulist)
		return
	} else
	// return play if query
	if play {
		tor.Stream(index, c.Request, c.Writer)
		return
	}
}

func streamNoAuth(c *gin.Context) {
	link := c.Query("link")
	indexStr := c.Query("index")
	_, preload := c.GetQuery("preload")
	_, m3u := c.GetQuery("m3u")
	_, fromlast := c.GetQuery("fromlast")
	_, play := c.GetQuery("play")
	title := c.Query("title")
	poster := c.Query("poster")
	data := ""

	if link == "" {
		c.AbortWithError(http.StatusBadRequest, errors.New("link should not be empty"))
		return
	}

	link, _ = url.QueryUnescape(link)

	spec, err := utils.ParseLink(link)
	if err != nil {
		c.AbortWithError(http.StatusInternalServerError, err)
		return
	}

	tor := torr.GetTorrent(spec.InfoHash.HexString())
	if tor == nil {
		c.Header("WWW-Authenticate", "Basic realm=Authorization Required")
		c.AbortWithStatus(http.StatusUnauthorized)
		return
	}

	title = tor.Title
	poster = tor.Poster
	data = tor.Data

	if tor.Stat == state.TorrentInDB {
		tor, err = torr.AddTorrent(spec, title, poster, data)
		if err != nil {
			c.AbortWithError(http.StatusInternalServerError, err)
			return
		}
	}

	if !tor.GotInfo() {
		c.AbortWithError(http.StatusInternalServerError, errors.New("timeout connection torrent"))
		return
	}

	// find file
	index := -1
	if len(tor.Files()) == 1 {
		index = 0
	} else {
		ind, err := strconv.Atoi(indexStr)
		if err == nil {
			index = ind
		}
	}
	if index == -1 && play { // if file index not set and play file exec
		index = 0
	}
	// preload torrent
	if preload {
		torr.Preload(tor, index)
	}

	// return m3u if query
	if m3u {
		name := strings.ReplaceAll(c.Param("fname"), `/`, "") // strip starting / from param
		if name == "" {
			name = tor.Name() + ".m3u"
		} else if !strings.HasSuffix(strings.ToLower(name), ".m3u") && !strings.HasSuffix(strings.ToLower(name), ".m3u8") {
			name += ".m3u"
		}
		m3ulist := "#EXTM3U\n" + getM3uList(tor.Status(), utils2.GetScheme(c)+"://"+c.Request.Host, fromlast)
		sendM3U(c, name, tor.Hash().HexString(), m3ulist)
		return
	} else
	// return play if query
	if play {
		tor.Stream(index, c.Request, c.Writer)
		return
	}
	c.Header("WWW-Authenticate", "Basic realm=Authorization Required")
	c.AbortWithStatus(http.StatusUnauthorized)
}
