package api

import (
	"errors"
	"fmt"
	"net/http"

	"server/ffprobe"
	sets "server/settings"

	"github.com/gin-gonic/gin"
)

// ffp godoc
//
//	@Summary		Gather informations using ffprobe
//	@Description	Gather informations using ffprobe.
//
//	@Tags			API
//
//	@Param			hash	query	string	true	"Torrent hash"
//	@Param			id		query	string	true	"File index in torrent"
//
//	@Produce		json
//	@Success		200	"Data returned from ffprobe"
//	@Router			/ffp [get]
func ffp(c *gin.Context) {
	hash := c.Param("hash")
	indexStr := c.Param("id")

	if hash == "" || indexStr == "" {
		c.AbortWithError(http.StatusNotFound, errors.New("link should not be empty"))
		return
	}

	link := "http://127.0.0.1:" + sets.Port + "/play/" + hash + "/" + indexStr
	if sets.Ssl {
		link = "https://127.0.0.1:" + sets.SslPort + "/play/" + hash + "/" + indexStr
	}

	data, err := ffprobe.ProbeUrl(link)
	if err != nil {
		c.AbortWithError(http.StatusBadRequest, fmt.Errorf("error getting data: %v", err))
		return
	}

	c.JSON(200, data)
}
