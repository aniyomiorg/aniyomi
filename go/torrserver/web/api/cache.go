package api

import (
	"net/http"

	"server/torr"

	"github.com/gin-gonic/gin"
	"github.com/pkg/errors"
)

// Action: get
type cacheReqJS struct {
	requestI
	Hash string `json:"hash,omitempty"`
}

// cache godoc
//
//	@Summary		Return cache stats
//	@Description	Return cache stats.
//
//	@Tags			API
//
//	@Param			request	body	cacheReqJS	true	"Cache stats request"
//
//	@Produce		json
//	@Success		200	{object} state.CacheState	"Cache stats"
//	@Router			/cache [post]
func cache(c *gin.Context) {
	var req cacheReqJS
	err := c.ShouldBindJSON(&req)
	if err != nil {
		c.AbortWithError(http.StatusBadRequest, err)
		return
	}
	c.Status(http.StatusBadRequest)
	switch req.Action {
	case "get":
		{
			getCache(req, c)
		}
	}
}

func getCache(req cacheReqJS, c *gin.Context) {
	if req.Hash == "" {
		c.AbortWithError(http.StatusBadRequest, errors.New("hash is empty"))
		return
	}
	tor := torr.GetTorrent(req.Hash)

	if tor != nil {
		st := tor.CacheState()
		if st == nil {
			c.JSON(200, struct{}{})
		} else {
			c.JSON(200, st)
		}
	} else {
		c.Status(http.StatusNotFound)
	}
}
