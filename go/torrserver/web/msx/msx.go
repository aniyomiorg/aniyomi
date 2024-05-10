package msx

import (
	_ "embed"
	"encoding/json"
	"net/http"
	"sync"

	"server/version"

	"github.com/gin-gonic/gin"
)

var (
	//go:embed russian.json.gz
	rus []byte
	//go:embed torrents.js.gz
	trs []byte
	//go:embed torrent.js.gz
	trn []byte
	//go:embed ts.js.gz
	its []byte

	idb = new(sync.Mutex)
	ids = make(map[string]string)
)

func asset(c *gin.Context, t string, d []byte) {
	c.Header("Content-Encoding", "gzip")
	c.Data(200, t+"; charset=UTF-8", d)
}

func SetupRoute(r *gin.RouterGroup) {
	r.GET("/msx/:pth", msxPTH)
	r.GET("/msx/imdb", msxIMDB)
	r.GET("/msx/imdb/:id", msxIMDBID)
}

// msxPTH godoc
//
//	@Summary		Multi usage endpoint
//	@Description	Multi usage endpoint.
//
//	@Tags			MSX
//
//	@Param			link	query	string	true	"Magnet/hash/link to torrent"
//
//	@Produce		json
//	@Success		200	"Data returned according to query"
//	@Router			/msx [get]
func msxPTH(c *gin.Context) {
	js := []string{"http://msx.benzac.de/js/tvx-plugin.min.js"}
	switch p := c.Param("pth"); p {
	case "start.json":
		c.JSON(200, map[string]string{
			"name":      "TorrServer",
			"version":   version.Version,
			"parameter": "menu:request:interaction:init@{PREFIX}{SERVER}/msx/ts",
		})
	case "russian.json":
		asset(c, "application/json", rus)
	case "torrents.js":
		asset(c, "text/javascript", trs)
	case "torrent.js":
		asset(c, "text/javascript", trn)
	case "ts.js":
		asset(c, "text/javascript", its)
	case "torrents":
		js = append(js, p+".js")
		p = "torrent"
		fallthrough
	case "torrent":
		if c.Query("platform") == "tizen" {
			js = append(js, "http://msx.benzac.de/interaction/js/tizen-player.js")
		}
		fallthrough
	case "ts":
		b := []byte("<!DOCTYPE html>\n<html>\n<head>\n<title>TorrServer Plugin</title>\n<meta charset='UTF-8'>\n")
		for _, j := range append(js, p+".js") {
			b = append(b, "<script type='text/javascript' src='"+j+"'></script>\n"...)
		}
		c.Data(200, "text/html; charset=UTF-8", append(b, "</head>\n<body></body>\n</html>"...))
	default:
		c.AbortWithStatus(404)
	}
}

// msxIMDB godoc
//
//	@Summary		Get MSX IMDB informations
//	@Description	Get MSX IMDB informations.
//
//	@Tags			MSX
//
//	@Produce		json
//	@Success		200	"JSON MSX IMDB informations"
//	@Router			/msx/imdb [get]
func msxIMDB(c *gin.Context) {
	idb.Lock()
	defer idb.Unlock()
	l := len(ids)
	ids = make(map[string]string)
	c.JSON(200, l)
}

// msxIMDB godoc
//
//	@Summary		Get MSX IMDB informations
//	@Description	Get MSX IMDB informations.
//
//	@Tags			MSX
//
//	@Param			id	path	string	true	"IMDB ID"
//
// @Produce		json
// @Success		200	"JSON MSX IMDB informations"
// @Router			/msx/imdb/:id [get]
func msxIMDBID(c *gin.Context) {
	idb.Lock()
	defer idb.Unlock()
	p := c.Param("id")
	i, o := ids[p]
	if !o {
		if r, e := http.Get("https://v2.sg.media-imdb.com/suggestion/h/" + p + ".json"); e == nil {
			defer r.Body.Close()
			if r.StatusCode == 200 {
				var j struct {
					D []struct{ I struct{ ImageUrl string } }
				}
				if e = json.NewDecoder(r.Body).Decode(&j); e == nil && len(j.D) > 0 {
					i = j.D[0].I.ImageUrl
				}
			}
		}
		ids[p] = i
	}
	c.JSON(200, i)
}
