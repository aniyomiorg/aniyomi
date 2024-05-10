package api

import (
	"github.com/gin-gonic/gin"
)

type requestI struct {
	Action string `json:"action,omitempty"`
}

func SetupRoute(route *gin.RouterGroup) {
	route.GET("/shutdown", shutdown)

	route.POST("/settings", settings)

	route.POST("/torrents", torrents)
	route.POST("/torrent/upload", torrentUpload)

	route.POST("/cache", cache)

	route.HEAD("/stream", stream)
	route.HEAD("/stream/*fname", stream)

	route.GET("/stream", stream)
	route.GET("/stream/*fname", stream)

	route.HEAD("/play/:hash/:id", play)
	route.GET("/play/:hash/:id", play)

	route.POST("/viewed", viewed)

	route.GET("/playlistall/all.m3u", allPlayList)
	route.GET("/playlist", playList)
	route.GET("/playlist/*fname", playList) // Is this endpoint still needed ? `fname` is never used in handler

	route.GET("/download/:size", download)

	route.GET("/search/*query", rutorSearch)

	route.GET("/ffp/:hash/:id", ffp)
}
