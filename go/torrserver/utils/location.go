package utils

import (
	"github.com/gin-contrib/location"
	"github.com/gin-gonic/gin"
)

func GetScheme(c *gin.Context) string {
	url := location.Get(c)
	if url == nil {
		return "http"
	}
	return url.Scheme
}
